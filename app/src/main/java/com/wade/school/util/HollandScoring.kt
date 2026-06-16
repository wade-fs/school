package com.wade.school.util

import org.json.JSONObject

object HollandScoring {
    // RIASEC 類型與題號對應 (假設題目已排序)
    // 範例：題目 1-5 (R), 6-10 (I), 11-15 (A), 16-20 (S), 21-25 (E), 26-30 (C)
    private val mapping = mapOf(
        "R" to listOf(1, 2, 3, 4, 5),
        "I" to listOf(6, 7, 8, 9, 10),
        "A" to listOf(11, 12, 13, 14, 15),
        "S" to listOf(16, 17, 18, 19, 20),
        "E" to listOf(21, 22, 23, 24, 25),
        "C" to listOf(26, 27, 28, 29, 30)
    )

    fun calculateScores(answersJson: String): Map<String, Int> {
        val json = JSONObject(answersJson)
        val scores = mutableMapOf<String, Int>()
        
        mapping.forEach { (type, questions) ->
            var typeScore = 0
            questions.forEach { qOrder ->
                // json key 是 qId (Int) 轉 String，這裡簡化處理假設 qId 對應 order
                val score = json.optInt(qOrder.toString(), 0)
                typeScore += score
            }
            scores[type] = typeScore
        }
        return scores
    }
}
