package app.f2ltrainer.data

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.F2l

/** Checks algorithms typed in by the user against the cube simulator. */
object AlgValidator {
    sealed interface Result {
        /** [normalized] is the algorithm written so that it starts from the case picture. */
        data class Ok(val normalized: String) : Result
        data class Error(val message: String) : Result
    }

    const val MAX_LENGTH = 40

    fun validate(case: F2lCase, input: String, freeSlot: Boolean = false): Result {
        val tokens = try {
            Alg.parse(input)
        } catch (e: IllegalArgumentException) {
            return Result.Error(e.message ?: "Invalid notation")
        }
        if (tokens.isEmpty()) return Result.Error("Please enter an algorithm")
        if (tokens.size > MAX_LENGTH) return Result.Error("Too long (max $MAX_LENGTH moves)")
        val free = if (freeSlot) setOf(F2l.Slot.BR) else emptySet()
        // Accept the algorithm with any pre-AUF: try all four U-layer angles.
        for (j in 0 until 4) {
            val adjusted = Alg.prefixU(j, tokens)
            if (F2l.isSolved(case.displayState.apply(adjusted), free)) {
                return Result.Ok(Alg.format(adjusted))
            }
        }
        return Result.Error("This does not solve the case" + if (freeSlot) "" else " (cross and the other slots must stay solved)")
    }
}
