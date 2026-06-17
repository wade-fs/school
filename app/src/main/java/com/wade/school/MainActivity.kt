package com.wade.school

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wade.school.ui.screens.*
import com.wade.school.ui.theme.TaiwanTeacherAppTheme

import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaiwanTeacherAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeacherAppNavigation()
                }
            }
        }
    }
}

@Composable
fun TeacherAppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "role_selector") {
        composable("role_selector") {
            RoleSelectorScreen(onRoleSelected = { role ->
                if (role == "school_info") {
                    navController.navigate("school_info")
                } else {
                    navController.navigate("dashboard/$role")
                }
            })
        }
        composable("dashboard/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "guest"
            DashboardScreen(
                role = role, 
                onBack = { navController.popBackStack() },
                onNavigateToStudent = { id, name -> 
                    navController.navigate("student_detail/$id/$name")
                },
                onNavigateToMoodCheck = { classId ->
                    navController.navigate("mood_check?classId=$classId")
                },
                onNavigateToResources = {
                    navController.navigate("external_resources")
                },
                onNavigateToLessonPlans = {
                    navController.navigate("lesson_plans")
                },
                onNavigateToTagging = { classId ->
                    navController.navigate("class_tagging/$classId")
                },
                onNavigateToAssignments = { classId ->
                    navController.navigate("assignments/$classId")
                },
                onNavigateToAnalysis = { classId ->
                    navController.navigate("grade_analysis/$classId")
                },
                onNavigateToAttendance = { classId ->
                    navController.navigate("attendance/$classId")
                },
                onNavigateToAttendanceHistory = { classId ->
                    navController.navigate("attendance_history/$classId")
                },
                onNavigateToBulletins = { classId ->
                    navController.navigate("homeroom_mgmt/$classId")
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable("student_detail/{id}/{name}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            StudentDetailScreen(
                studentId = id,
                studentName = name,
                onBack = { navController.popBackStack() }
            )
        }
        composable("mood_check?classId={classId}", arguments = listOf(
            androidx.navigation.navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            MoodCheckScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = { navController.popBackStack() },
                preSelectedClassId = backStackEntry.arguments?.getString("classId")
            )
        }
        composable("external_resources") {
            ExternalResourceScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("lesson_plans") {
            LessonPlanScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("class_tagging/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassroomTaggingScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("assignments/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AssignmentManagementScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("grade_analysis/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            GradeAnalysisScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("attendance/{classId}?date={date}&period={period}", arguments = listOf(
            navArgument("date") { 
                type = NavType.LongType
                defaultValue = -1L 
            },
            navArgument("period") {
                type = NavType.StringType
                nullable = true
            }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val date = backStackEntry.arguments?.getLong("date") ?: -1L
            val period = backStackEntry.arguments?.getString("period")
            val actualDate = if (date == -1L) System.currentTimeMillis() else date
            AttendanceScreen(
                classId = classId,
                date = actualDate,
                initialPeriod = period,
                onBack = { navController.popBackStack() }
            )
        }
        composable("attendance_history/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AttendanceHistoryScreen(
                classId = classId,
                onBack = { navController.popBackStack() },
                onEditDate = { dateMillis, period ->
                    navController.navigate("attendance/$classId?date=$dateMillis&period=$period")
                }
            )
        }
        composable("homeroom_mgmt/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            HomeroomManagementScreen(
                classId = classId,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable("school_info") {
            SchoolInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("school_info/scan") {
            // Using OfficialDocumentScreen as a temporary implementation for scanning
            OfficialDocumentScreen(onBack = { navController.popBackStack() })
        }
        composable("manual") {
            ManualScreen(onBack = { navController.popBackStack() })
        }
        composable("counseling/alerts") {
            RiskAlertDashboardScreen(
                onBack = { navController.popBackStack() },
                onNavigateToStudent = { studentId -> navController.navigate("student_detail/$studentId/Unknown") }
            )
        }
        composable("counseling/sessions/active") {
            AssessmentSessionListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { sessionId -> navController.navigate("counseling/session/detail/$sessionId") }
            )
        }
        composable("counseling/session/detail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            AssessmentSessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("counseling/templates") {
            AssessmentManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSession = { templateId -> navController.navigate("counseling/session/$templateId") }
            )
        }
        composable("counseling/session/{templateId}") { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            AssessmentSessionScreen(
                templateId = templateId,
                onBack = { navController.popBackStack() },
                onNavigateToStudentPicker = { sessionId, tid, cid -> 
                    navController.navigate("counseling/session/$sessionId/$tid/$cid") 
                }
            )
        }
        composable("counseling/session/{sessionId}/{templateId}/{classId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AssessmentStudentPickerScreen(
                sessionId = sessionId,
                templateId = templateId,
                classId = classId,
                onBack = { navController.popBackStack() },
                onNavigateToResponse = { sid, tid, stid -> navController.navigate("counseling/response/$sid/$tid/$stid") }
            )
        }
        composable("counseling/response/{sessionId}/{templateId}/{studentId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            AssessmentResponseScreen(
                sessionId = sessionId,
                templateId = templateId,
                studentId = studentId,
                onBack = { navController.popBackStack() },
                onNavigateToResult = { navController.navigate("counseling/result/$sessionId/$templateId/$studentId") }
            )
        }
        composable("counseling/result/{sessionId}/{templateId}/{studentId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            AssessmentResultScreen(
                sessionId = sessionId,
                templateId = templateId,
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("admin/docs/pending") {
            OfficialDocumentScreen(onBack = { navController.popBackStack() })
        }
        composable("admin/docs/search") {
            OfficialDocumentScreen(onBack = { navController.popBackStack() })
        }
        composable("admin/docs/archive") {
            OfficialDocumentScreen(onBack = { navController.popBackStack() })
        }

        // ── 科任教師平台 (Subject Teacher Platform) ─────────────────────────
        composable("subject/grades/input?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            GradeManagementScreen(classId = classId, onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/grades/calc?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            GradeManagementScreen(classId = classId, onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/grades/analysis?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            GradeManagementScreen(classId = classId, onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/makeup") {
            MakeupExamScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/interaction?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassroomInteractionScreen(classId = classId, onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/reflection?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            TeachingReflectionScreen(classId = classId, onNavigateBack = { navController.popBackStack() })
        }
        composable("subject/attendance?classId={classId}&period={period}", arguments = listOf(
            navArgument("classId") { nullable = true },
            navArgument("period") { type = NavType.IntType; defaultValue = 1 }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val period = backStackEntry.arguments?.getInt("period") ?: 1
            SubjectAttendanceScreen(classId = classId, period = period, onBack = { navController.popBackStack() })
        }
        composable("subject/timetable") {
            // Placeholder: SubjectTeacherDashboard already has a timetable dialog.
            // For now, just go back or show a simple message.
            navController.popBackStack()
        }
        composable("subject/lesson_plans") {
            LessonPlanScreen(onBack = { navController.popBackStack() })
        }

        // ── 導師平台 (Homeroom Platform) ───────────────────────────
        composable("homeroom/leave?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            LeaveRequestScreen(classId = classId, onBack = { navController.popBackStack() })
        }
        composable("homeroom/discipline?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            DisciplineScreen(classId = classId, onBack = { navController.popBackStack() })
        }
        composable("homeroom/students?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            HomeroomStudentListScreen(
                classId = classId,
                onNavigateToDetail = { id -> navController.navigate("student_detail/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("homeroom/fund?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassFundScreen(classId = classId, onBack = { navController.popBackStack() })
        }
        composable("homeroom/conference?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ParentConferenceScreen(classId = classId, onBack = { navController.popBackStack() })
        }
        composable("homeroom/health?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            StudentHealthScreen(classId = classId, onBack = { navController.popBackStack() })
        }
        composable("homeroom/semester?classId={classId}", arguments = listOf(
            navArgument("classId") { nullable = true }
        )) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            SemesterRecordScreen(classId = classId, onBack = { navController.popBackStack() })
        }
    }
}
