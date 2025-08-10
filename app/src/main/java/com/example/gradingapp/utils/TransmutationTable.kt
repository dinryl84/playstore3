package com.example.gradingapp.utils

object TransmutationTable {

    /**
     * Converts an initial grade (percentage) to a transmuted grade according to DepEd standards
     * @param initialGrade The computed percentage grade (0-100)
     * @return The transmuted grade (75-100 scale)
     */
    fun getTransmutedGrade(initialGrade: Double): Int {
        return when {
            initialGrade >= 100.0 -> 100
            initialGrade >= 98.40 -> 99
            initialGrade >= 96.80 -> 98
            initialGrade >= 95.20 -> 97
            initialGrade >= 93.60 -> 96
            initialGrade >= 92.00 -> 95
            initialGrade >= 90.40 -> 94
            initialGrade >= 88.80 -> 93
            initialGrade >= 87.20 -> 92
            initialGrade >= 85.60 -> 91
            initialGrade >= 84.00 -> 90
            initialGrade >= 82.40 -> 89
            initialGrade >= 80.80 -> 88
            initialGrade >= 79.20 -> 87
            initialGrade >= 77.60 -> 86
            initialGrade >= 76.00 -> 85
            initialGrade >= 74.40 -> 84
            initialGrade >= 72.80 -> 83
            initialGrade >= 71.20 -> 82
            initialGrade >= 69.60 -> 81
            initialGrade >= 68.00 -> 80
            initialGrade >= 66.40 -> 79
            initialGrade >= 64.80 -> 78
            initialGrade >= 63.20 -> 77
            initialGrade >= 61.60 -> 76
            initialGrade >= 60.00 -> 75 // Passing grade
            else -> 74 // Below passing (technically "below 75")
        }
    }

    /**
     * Checks if a grade is passing (75 or above)
     * @param transmutedGrade The transmuted grade
     * @return True if passing, false otherwise
     */
    fun isPassing(transmutedGrade: Int): Boolean {
        return transmutedGrade >= 75
    }

    /**
     * Gets the grade remark based on the transmuted grade
     * @param transmutedGrade The transmuted grade
     * @return String remark for the grade
     */
    fun getGradeRemark(transmutedGrade: Int): String {
        return when {
            transmutedGrade >= 95 -> "Outstanding"
            transmutedGrade >= 90 -> "Very Satisfactory"
            transmutedGrade >= 85 -> "Satisfactory"
            transmutedGrade >= 80 -> "Fairly Satisfactory"
            transmutedGrade >= 75 -> "Did Not Meet Expectations"
            else -> "Failed"
        }
    }
}