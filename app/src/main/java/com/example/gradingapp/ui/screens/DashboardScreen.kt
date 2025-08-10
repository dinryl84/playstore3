package com.example.gradingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gradingapp.ui.viewmodel.SectionViewModel
import com.example.gradingapp.ui.viewmodel.SubjectViewModel
import com.example.gradingapp.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sectionViewModel: SectionViewModel = hiltViewModel(),
    subjectViewModel: SubjectViewModel = hiltViewModel(),
    studentViewModel: StudentViewModel = hiltViewModel()
) {
    // Collect data from ViewModels
    val sections by sectionViewModel.sections.collectAsState()
    val subjects by subjectViewModel.subjects.collectAsState()
    val students by studentViewModel.students.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Senior High School",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Grading System Dashboard",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Statistics Cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            item {
                StatisticCard(
                    title = "Sections",
                    count = sections.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                StatisticCard(
                    title = "Subjects",
                    count = subjects.size,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                StatisticCard(
                    title = "Students",
                    count = students.size,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            item {
                StatisticCard(
                    title = "Total Classes",
                    count = sections.size * subjects.size,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Management Buttons
        Text(
            text = "Management Options",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ManagementButton(
                title = "Section Management",
                description = "Add, edit, and manage class sections",
                onClick = { navController.navigate("section_management") }
            )

            ManagementButton(
                title = "Subject Management",
                description = "Add, edit, and manage subjects",
                onClick = { /* TODO: Navigate to subject management */ }
            )

            ManagementButton(
                title = "Student Management",
                description = "Add, edit, and manage students",
                onClick = { /* TODO: Navigate to student management */ }
            )

            ManagementButton(
                title = "Score Input",
                description = "Record student scores and assessments",
                onClick = { /* TODO: Navigate to score input */ }
            )
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ManagementButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}