package com.gymlogger.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSetTest {

    @Test
    fun `getTypeForRir should return FAILURE for RIR 0`() {
        assertEquals(WorkoutSet.SetType.FAILURE, WorkoutSet.getTypeForRir(0, WorkoutSet.SetType.NORMAL))
    }

    @Test
    fun `getTypeForRir should return NORMAL for RIR 1 to 3`() {
        assertEquals(WorkoutSet.SetType.NORMAL, WorkoutSet.getTypeForRir(1, WorkoutSet.SetType.WARMUP))
        assertEquals(WorkoutSet.SetType.NORMAL, WorkoutSet.getTypeForRir(2, WorkoutSet.SetType.FAILURE))
        assertEquals(WorkoutSet.SetType.NORMAL, WorkoutSet.getTypeForRir(3, WorkoutSet.SetType.WARMUP))
    }

    @Test
    fun `getTypeForRir should return WARMUP for RIR 4 and above`() {
        assertEquals(WorkoutSet.SetType.WARMUP, WorkoutSet.getTypeForRir(4, WorkoutSet.SetType.NORMAL))
        assertEquals(WorkoutSet.SetType.WARMUP, WorkoutSet.getTypeForRir(5, WorkoutSet.SetType.NORMAL))
        assertEquals(WorkoutSet.SetType.WARMUP, WorkoutSet.getTypeForRir(10, WorkoutSet.SetType.NORMAL))
    }

    @Test
    fun `getTypeForRir should stay DROP_SET regardless of RIR`() {
        assertEquals(WorkoutSet.SetType.DROP_SET, WorkoutSet.getTypeForRir(0, WorkoutSet.SetType.DROP_SET))
        assertEquals(WorkoutSet.SetType.DROP_SET, WorkoutSet.getTypeForRir(2, WorkoutSet.SetType.DROP_SET))
        assertEquals(WorkoutSet.SetType.DROP_SET, WorkoutSet.getTypeForRir(5, WorkoutSet.SetType.DROP_SET))
    }

    @Test
    fun `getTypeForRir should return currentType if RIR is null`() {
        assertEquals(WorkoutSet.SetType.NORMAL, WorkoutSet.getTypeForRir(null, WorkoutSet.SetType.NORMAL))
        assertEquals(WorkoutSet.SetType.WARMUP, WorkoutSet.getTypeForRir(null, WorkoutSet.SetType.WARMUP))
    }
}
