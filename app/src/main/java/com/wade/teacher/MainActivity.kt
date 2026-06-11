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
            DashboardScreen(role = role, onBack = {
                navController.popBackStack()
            })
        }
    }
}
