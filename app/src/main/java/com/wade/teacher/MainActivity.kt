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
import com.wade.teacher.ui.screens.RoleSelectorScreen
import com.wade.teacher.ui.screens.DashboardScreen
import com.wade.teacher.ui.screens.StudentDetailScreen
import com.wade.teacher.ui.screens.MoodCheckScreen
import com.wade.teacher.ui.screens.ExternalResourceScreen
import com.wade.teacher.ui.screens.LessonPlanScreen
import com.wade.teacher.ui.screens.ClassroomTaggingScreen
import com.wade.teacher.ui.screens.AssignmentManagementScreen
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
                onNavigateToMoodCheck = {
                    navController.navigate("mood_check")
                },
                onNavigateToResources = {
                    navController.navigate("external_resources")
                },
                onNavigateToLessonPlans = {
                    navController.navigate("lesson_plans")
                },
                onNavigateToTagging = { classId ->
                    navController.navigate("classroom_tagging/$classId")
                },
                onNavigateToAssignments = { classId ->
                    navController.navigate("assignment_management/$classId")
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
        composable("mood_check") {
            MoodCheckScreen(
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                onBack = { navController.popBackStack() }
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
        composable("classroom_tagging/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassroomTaggingScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("assignment_management/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            AssignmentManagementScreen(
                classId = classId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
