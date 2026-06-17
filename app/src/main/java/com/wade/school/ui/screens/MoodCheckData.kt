package com.wade.school.ui.screens

data class MoodCheckItemData(
    val selectedEmotion: String? = null,
    val emotionQuadrant: String? = null,
    val bsrsScores: List<Int>? = null, // List of 6 scores (0-4)
    val note: String = ""
) {
    val isComplete: Boolean
        get() = selectedEmotion != null && emotionQuadrant != null && (bsrsScores == null || bsrsScores.size == 6)
}

val EMOTION_YELLOW = listOf("興奮", "快樂", "充滿希望", "熱情", "驚喜", "自豪", "充滿活力", "自信", "樂觀")
val EMOTION_RED = listOf("憤怒", "焦慮", "驚恐", "煩躁", "緊張", "挫折", "嫉妒", "擔憂", "激動")
val EMOTION_GREEN = listOf("平靜", "放鬆", "滿足", "感恩", "自在", "安心", "包容", "輕鬆", "和諧")
val EMOTION_BLUE = listOf("憂鬱", "疲憊", "悲傷", "孤單", "無聊", "失望", "無助", "沮喪", "提不起勁")

val BSRS_QUESTIONS = listOf(
    "1. 感覺睡眠困難，譬如難以入睡、易醒或早醒",
    "2. 感覺緊張不安",
    "3. 感覺容易苦惱或動怒",
    "4. 感覺憂鬱、心情低落",
    "5. 覺得比不上別人",
    "6. 有自殺的想法"
)
