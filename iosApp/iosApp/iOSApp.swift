import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        MacroCalculator.shared.activeEngine = SwiftLlamaEngine()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
