package com.example.gradingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gradingapp.ui.screens.DashboardScreen
import com.example.gradingapp.ui.screens.SectionManagementScreen
import com.example.gradingapp.ui.screens.SubjectManagementScreen
import com.example.gradingapp.ui.screens.StudentManagementScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }

        composable("section_management") {
            SectionManagementScreen(navController = navController)
        }

        composable("subject_management") {
            SubjectManagementScreen(navController = navController)
        }

        composable("student_management") {
            StudentManagementScreen(navController = navController)
        }

        // TODO: Add other screen destinations
        // composable("score_input") { ScoreInputScreen(navController) }
    }
}