package com.gymlogger.model

data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: Equipment,
    val instructions: String? = null,
    val category: ExerciseCategory = ExerciseCategory.STRENGTH
) {
    val formattedInstructions: List<String>
        get() {
            val instr = instructions ?: return emptyList()
            val stepPattern = Regex("\\d+[.)]+\\s*")
            val hasNewlines = instr.contains('\n')

            val rawSteps = if (hasNewlines) {
                instr.split('\n')
            } else {
                val matches = stepPattern.findAll(instr).toList()
                if (matches.isNotEmpty()) {
                    instr.split(stepPattern).filter { it.isNotBlank() }
                } else {
                    instr.split(Regex("\\.\\s+")).filter { it.isNotBlank() }
                }
            }

            return rawSteps.map { step ->
                step.replace(Regex("^\\s*(\\d+[.)]+\\s*|Step\\s*\\d+[:.]?\\s*|To begin this exercise;\\s*)", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .removeSuffix(".")
            }.filter { it.isNotBlank() }
        }
    enum class Equipment {
        BARBELL,
        BANDS,
        BENCH,
        DUMBBELL,
        CABLE,
        MACHINE,
        BODYWEIGHT,
        KETTLEBELL,
        MEDICINE_BALL,
        STABILITY_BALL,
        MACEBELL,
        SANDBAG,
        LANDMINE,
        SPECIALTY_BAR,
        FUNCTIONAL_TOOL,
        EXERCISEBALL,
        WEIGHTPLATE,
        EZCURLBAR,
        SMITH,
        LEVERAGE,
        TRAPBAR,
        OTHER,
        BOX
    }

    enum class ExerciseCategory {
        STRENGTH,
        CARDIO,
        FLEXIBILITY,
        POWER
    }
}
