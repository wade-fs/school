package com.wade.teacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wade.teacher.ui.screens.*
import com.wade.teacher.ui.theme.TaiwanTeacherAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                navController.navigate("dashboard/$role")
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
                onNavigateToBulletins = { classId ->
                    navController.navigate("bulletins/$classId")
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
        composable("attendance/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AttendanceScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("bulletins/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassBulletinScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
