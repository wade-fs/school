package com.wade.teacher.util

import java.util.Calendar

object AcademicUtils {
    /**
     * 計算當前民國學年度
     * 規則：8月1日之後算新學年度。
     * 民國年 = 西元年 - 1911
     */
    fun getCurrentAcademicYear(): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) // 0-indexed, Aug is 7
        
        // 8月1日之後算新學年度，之前的算前一個學年度
        val westernAcademicYear = if (month >= Calendar.AUGUST) {
            year
        } else {
            year - 1
        }
        // 民國學年度 = 西元年 - 1911
        return westernAcademicYear - 1911
    }

    /**
     * 計算當前學期
     * 規則：8月1日到1月31日為上學期 (1)，2月1日到7月31日為下學期 (2)
     */
    fun getCurrentSemester(): Int {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        
        // 8, 9, 10, 11, 12, 1 月份屬「上學期」
        return if (month >= Calendar.AUGUST || month <= Calendar.JANUARY) {
            1
        } else {
            2
        }
    }

    fun getAcademicString(): String {
        val semester = if (getCurrentSemester() == 1) "上" else "下"
        return "${getCurrentAcademicYear()}學年度 $semester 學期"
    }
}
