package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── 學生獎懲記錄 ──────────────────────────────────────────────

enum class DisciplineType(val label: String, val score: Int) {
    COMMENDATION("嘉獎",   1),
    MINOR_MERIT("小功",    3),
    MAJOR_MERIT("大功",    9),
    WARNING("警告",       -1),
    MINOR_DEMERIT("小過", -3),
    MAJOR_DEMERIT("大過", -9),
    SPECIAL_AWARD("獎狀",  0),  // 競賽/校外獎項
    ADMONITION("申誡",    -1)   // 口頭告誡
}

@Entity(tableName = "discipline_records")
data class DisciplineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val type: DisciplineType,
    val reason: String,                  // 事由
    val sourceType: String = "HOMEROOM", // HOMEROOM / SUBJECT / ADMIN / SELF
    val recordDate: Long = System.currentTimeMillis(),
    val academicYear: Int,
    val semester: Int,
    val isReported: Boolean = false,     // 是否已送學務處
    val reportedAt: Long? = null,
    val note: String? = null
)

// ── 請假申請管理 ──────────────────────────────────────────────

enum class LeaveType(val label: String) {
    SICK("病假"),
    PERSONAL("事假"),
    OFFICIAL("公假"),
    FUNERAL("喪假"),
    LATE("遲到"),
    EARLY_LEAVE("早退"),
    ABSENCE("曠課")
}

enum class LeaveStatus(val label: String) {
    PENDING("待審核"),
    APPROVED("已核准"),
    REJECTED("未核准"),
    CANCELLED("已取消")
}

@Entity(tableName = "leave_requests")
data class LeaveRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val leaveType: LeaveType,
    val startDate: Long,
    val endDate: Long,
    val periodNames: String,             // 請假節次，逗號分隔："第一節,第二節"
    val totalPeriods: Int,
    val reason: String,
    val applicantType: String = "PARENT",// PARENT / STUDENT / TEACHER
    val attachmentPath: String? = null,  // 診斷書等附件
    val status: LeaveStatus = LeaveStatus.PENDING,
    val reviewedAt: Long? = null,
    val reviewNote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ── 出缺席異常通報 ──────────────────────────────────────────────

enum class AlertRuleType(val label: String) {
    ABSENT_3_PERIODS("單日曠課 3 節以上"),
    ABSENT_3_DAYS("連續曠課 3 日"),
    LATE_5_TIMES("本月遲到 5 次"),
    ABSENT_RATE("出缺席率低於 80%"),
    CUSTOM("自訂規則")
}

@Entity(tableName = "attendance_alerts")
data class AttendanceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val ruleType: AlertRuleType,
    val description: String,             // 觸發原因說明
    val triggeredAt: Long = System.currentTimeMillis(),
    val isHandled: Boolean = false,
    val handledNote: String? = null,
    val notifiedParent: Boolean = false, // 是否已通知家長
    val notifiedAt: Long? = null
)

// ── 學生健康資訊 ──────────────────────────────────────────────

@Entity(
    tableName = "student_health_info",
    indices = [Index(value = ["studentId"], unique = true)]
)
data class StudentHealthInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String = "",        // Added for easier display
    val bloodType: String? = null,
    val allergies: String? = null,       // 過敏原（逗號分隔）
    val chronicDisease: String? = null,  // 慢性病記錄
    val medication: String? = null,      // 長期用藥
    val emergencyContact1: String? = null,   // 緊急聯絡人1
    val emergencyPhone1: String? = null,
    val emergencyContact2: String? = null,
    val emergencyPhone2: String? = null,
    val specialNeeds: String? = null,    // 特殊需求說明
    val iepStudent: Boolean = false,     // 是否有個別化教育計畫
    val counselingReferral: Boolean = false, // 已轉介輔導
    val updatedAt: Long = System.currentTimeMillis()
)

// ── 操行成績與學期評語 ──────────────────────────────────────────────

@Entity(
    tableName = "semester_records",
    indices = [Index(value = ["studentId", "academicYear", "semester"], unique = true)]
)
data class SemesterRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val academicYear: Int,
    val semester: Int,
    // 操行
    val conductScore: Float? = null,     // 操行成績
    val conductBase: Float = 85f,        // 操行基準分
    val conductNote: String? = null,     // 操行說明
    // 學期評語
    val teacherComment: String? = null,  // 導師評語
    val strengthNote: String? = null,    // 優點特質
    val improvementNote: String? = null, // 待加強處
    // 其他
    val absenceDays: Int = 0,
    val lateTimes: Int = 0,
    val isFinalized: Boolean = false,    // 是否已送出
    val finalizedAt: Long? = null
)

// ── 班費管理 ──────────────────────────────────────────────

enum class FundTransactionType(val label: String) {
    INCOME("收入"),
    EXPENSE("支出")
}

@Entity(tableName = "class_fund_transactions")
data class ClassFundTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val type: FundTransactionType,
    val amount: Int,                     // 金額（元）
    val category: String,               // 班費收繳/文具/活動/其他
    val description: String,
    val receiptPath: String? = null,     // 收據照片
    val transactionDate: Long = System.currentTimeMillis(),
    val recordedBy: String = "導師"
)

// ── 親師座談記錄 ──────────────────────────────────────────────

@Entity(tableName = "parent_teacher_conferences")
data class ParentTeacherConference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val conferenceDate: Long,
    val attendees: String,               // 出席者姓名（逗號分隔）
    val academicDiscussion: String = "", // 學業討論
    val behaviorDiscussion: String = "", // 行為品德
    val parentConcerns: String = "",     // 家長反映事項
    val followUpActions: String = "",    // 後續追蹤事項
    val followUpDate: Long? = null,
    val isFollowUpDone: Boolean = false
)
