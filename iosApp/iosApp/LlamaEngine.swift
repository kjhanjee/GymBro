import Foundation
import ComposeApp
import llama

class SwiftLlamaEngine: NSObject, IosAiEngine {
    
    private var model: OpaquePointer?
    private var context: OpaquePointer?
    private var isInitialized = false

    func initEngine(completionHandler: @escaping (Error?) -> Void) {
        if isInitialized {
            completionHandler(nil)
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            llama_backend_init()
            
            var modelParams = llama_model_default_params()
            modelParams.n_gpu_layers = 99 // use Metal
            
            // IMPORTANT: Add the GGUF file to your Xcode project resources and update the name here
            guard let modelPath = Bundle.main.path(forResource: "gemma-2b-it-q4_k_m", ofType: "gguf") else {
                print("LlamaEngine: Model file 'gemma-2b-it-q4_k_m.gguf' not found in bundle.")
                self.isInitialized = true
                completionHandler(nil)
                return
            }
            
            self.model = llama_load_model_from_file(modelPath, modelParams)
            
            var contextParams = llama_context_default_params()
            contextParams.n_ctx = 2048
            self.context = llama_new_context_with_model(self.model, contextParams)
            
            self.isInitialized = true
            print("LlamaEngine: Model loaded successfully.")
            completionHandler(nil)
        }
    }
    
    func releaseEngine(completionHandler: @escaping (Error?) -> Void) {
        if let ctx = context { llama_free(ctx) }
        if let mdl = model { llama_free_model(mdl) }
        llama_backend_free()
        isInitialized = false
        completionHandler(nil)
    }

    func generateResponse(prompt: String, completionHandler: @escaping (String?, Error?) -> Void) {
        // For simplicity, we just use the stream and concatenate
        var fullResponse = ""
        generateResponseStream(prompt: prompt, onToken: { token in
            fullResponse += token
        }, onComplete: {
            completionHandler(fullResponse, nil)
        }, onError: { errorMsg in
            completionHandler(nil, NSError(domain: "LlamaEngine", code: 1, userInfo: [NSLocalizedDescriptionKey: errorMsg]))
        })
    }
    
    func generateResponseStream(prompt: String, onToken: @escaping (String) -> Void, onComplete: @escaping () -> Void, onError: @escaping (String) -> Void) {
        guard let ctx = context, let mdl = model else {
            onError("Model not loaded")
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            // Very basic tokenization
            let promptCString = prompt.cString(using: .utf8)
            var tokens = [llama_token](repeating: 0, count: prompt.utf8.count + 8)
            let n_tokens = llama_tokenize(mdl, promptCString, Int32(prompt.utf8.count), false, true, &tokens, Int32(tokens.count))
            
            if n_tokens < 0 {
                DispatchQueue.main.async { onError("Failed to tokenize") }
                return
            }
            
            // Clear KV cache for a new generation
            llama_kv_cache_clear(ctx)
            
            var batch = llama_batch_init(512, 0, 1)
            defer { llama_batch_free(batch) }
            
            for i in 0..<n_tokens {
                llama_batch_add(&batch, tokens[Int(i)], i, [0], false)
            }
            batch.logits[Int(n_tokens - 1)] = 1 // Evaluate logits for the last token
            
            if llama_decode(ctx, batch) != 0 {
                DispatchQueue.main.async { onError("Decode failed") }
                return
            }
            
            var currentPos = n_tokens
            let maxTokens: Int32 = 512
            
            for _ in 0..<maxTokens {
                let n_vocab = llama_n_vocab(mdl)
                let logits = llama_get_logits_ith(ctx, batch.n_tokens - 1)
                
                var candidates = [llama_token_data]()
                candidates.reserveCapacity(Int(n_vocab))
                for i in 0..<n_vocab {
                    candidates.append(llama_token_data(id: i, logit: logits![Int(i)], p: 0.0))
                }
                
                var candidates_p = llama_token_data_array(data: &candidates, size: candidates.count, sorted: false)
                let newToken = llama_sample_token_greedy(ctx, &candidates_p)
                
                if newToken == llama_token_eos(mdl) {
                    break
                }
                
                var buf = [CChar](repeating: 0, count: 32)
                let n_chars = llama_token_to_piece(mdl, newToken, &buf, Int32(buf.count), 0, false)
                if n_chars > 0 {
                    let tokenStr = String(cString: buf)
                    DispatchQueue.main.async { onToken(tokenStr) }
                }
                
                // Prepare next token
                llama_batch_clear(&batch)
                llama_batch_add(&batch, newToken, currentPos, [0], true)
                currentPos += 1
                
                if llama_decode(ctx, batch) != 0 {
                    break
                }
            }
            
            DispatchQueue.main.async { onComplete() }
        }
    }
    
    func startChat() {
        // Not implemented fully, KV cache clears each time anyway in this minimal impl
    }
    
    func summarizeMessages(conversationText: String, completionHandler: @escaping (String?, Error?) -> Void) {
        let prompt = "Summarize the following:\n\(conversationText)\n\nSummary:"
        generateResponse(prompt: prompt, completionHandler: completionHandler)
    }
    
    func calculateMacros(mealDescription: String, labelsInfo: String, completionHandler: @escaping (String?, Error?) -> Void) {
        let prompt = "Analyze meal: \(mealDescription)\nReturn JSON."
        generateResponse(prompt: prompt, completionHandler: completionHandler)
    }
    
    func sendChatMessageStream(prompt: String, onTextToken: @escaping (String) -> Void, onThoughtToken: @escaping (String) -> Void, onComplete: @escaping () -> Void, onError: @escaping (String) -> Void) {
        // For simplicity, streams everything as text
        generateResponseStream(prompt: prompt, onToken: onTextToken, onComplete: onComplete, onError: onError)
    }
}
