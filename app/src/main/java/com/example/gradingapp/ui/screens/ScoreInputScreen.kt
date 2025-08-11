// File path: app/src/main/java/com/example/gradingapp/ui/screens/ScoreInputScreen.kt

package com.example.gradingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gradingapp.ui.viewmodel.ScoreInputViewModel
import com.example.gradingapp.ui.viewmodel.StudentScoreData
import com.example.gradingapp.ui.viewmodel.ScoreEntry
import com.example.gradingapp.ui.viewmodel.ComputedGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreInputScreen(
    navController: NavController,
    viewModel: ScoreInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-dismiss error messages after 3 seconds
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Score Input",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Filter Section
        FilterSection(
            uiState = uiState,
            onSectionSelected = viewModel::onSectionSelected,
            onSubjectSelected = viewModel::onSubjectSelected,
            onSemesterSelected = viewModel::onSemesterSelected,
            onQuarterSelected = viewModel::onQuarterSelected
        )

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Students Score Table
        if (uiState.studentsScoreData.isNotEmpty() && !uiState.isLoading) {
            ScoreTable(
                studentsScoreData = uiState.studentsScoreData,
                onWrittenWorkScoreChanged = viewModel::updateWrittenWorkScore,
                onPerformanceTaskScoreChanged = viewModel::updatePerformanceTaskScore,
                onExamScoreChanged = viewModel::updateQuarterlyAssessmentScore
            )
        } else if (!uiState.isLoading && uiState.selectedSectionId != null && uiState.selectedSubjectId != null) {
            // Show message when filters are selected but no data
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students found for the selected section and subject.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    uiState: com.example.gradingapp.ui.viewmodel.ScoreInputUiState,
    onSectionSelected: (Int) -> Unit,
    onSubjectSelected: (Int) -> Unit,
    onSemesterSelected: (Int) -> Unit,
    onQuarterSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section Dropdown
            var sectionExpanded by remember { mutableStateOf(false) }
            val selectedSection = uiState.sections.find { it.id == uiState.selectedSectionId }

            ExposedDropdownMenuBox(
                expanded = sectionExpanded,
                onExpandedChange = { sectionExpanded = !sectionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSection?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Section") },
                    placeholder = { Text("Select Section") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 8.dp)
                )
                ExposedDropdownMenu(
                    expanded = sectionExpanded,
                    onDismissRequest = { sectionExpanded = false }
                ) {
                    uiState.sections.forEach { section ->
                        DropdownMenuItem(
                            text = { Text("${section.name} (${section.level})") },
                            onClick = {
                                onSectionSelected(section.id)
                                sectionExpanded = false
                            }
                        )
                    }
                }
            }

            // Subject Dropdown (enabled only when section is selected)
            var subjectExpanded by remember { mutableStateOf(false) }
            val selectedSubject = uiState.availableSubjectsForSection.find { it.id == uiState.selectedSubjectId }

            ExposedDropdownMenuBox(
                expanded = subjectExpanded,
                onExpandedChange = {
                    if (uiState.selectedSectionId != null) {
                        subjectExpanded = !subjectExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = selectedSubject?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    enabled = uiState.selectedSectionId != null,
                    label = { Text("Subject") },
                    placeholder = { Text("Select Subject") },
                    trailingIcon = {
                        if (uiState.selectedSectionId != null) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 8.dp)
                )
                ExposedDropdownMenu(
                    expanded = subjectExpanded,
                    onDismissRequest = { subjectExpanded = false }
                ) {
                    uiState.availableSubjectsForSection.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                onSubjectSelected(subject.id)
                                subjectExpanded = false
                            }
                        )
                    }
                }
            }

            // Semester and Quarter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Semester Dropdown
                var semesterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = !semesterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Semester ${uiState.selectedSemester}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Semester") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = semesterExpanded,
                        onDismissRequest = { semesterExpanded = false }
                    ) {
                        listOf(1, 2).forEach { semester ->
                            DropdownMenuItem(
                                text = { Text("Semester $semester") },
                                onClick = {
                                    onSemesterSelected(semester)
                                    semesterExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quarter Dropdown
                var quarterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = quarterExpanded,
                    onExpandedChange = { quarterExpanded = !quarterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Quarter ${uiState.selectedQuarter}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Quarter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quarterExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = quarterExpanded,
                        onDismissRequest = { quarterExpanded = false }
                    ) {
                        listOf(1, 2).forEach { quarter ->
                            DropdownMenuItem(
                                text = { Text("Quarter $quarter") },
                                onClick = {
                                    onQuarterSelected(quarter)
                                    quarterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreTable(
    studentsScoreData: List<StudentScoreData>,
    onWrittenWorkScoreChanged: (Int, Int, Int, Int) -> Unit,
    onPerformanceTaskScoreChanged: (Int, Int, Int, Int) -> Unit,
    onExamScoreChanged: (Int, Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(studentsScoreData) { studentData ->
            StudentScoreCard(
                studentData = studentData,
                onWrittenWorkScoreChanged = onWrittenWorkScoreChanged,
                onPerformanceTaskScoreChanged = onPerformanceTaskScoreChanged,
                onExamScoreChanged = onExamScoreChanged
            )
        }
    }
}

@Composable
fun StudentScoreCard(
    studentData: StudentScoreData,
    onWrittenWorkScoreChanged: (Int, Int, Int, Int) -> Unit,
    onPerformanceTaskScoreChanged: (Int, Int, Int, Int) -> Unit,
    onExamScoreChanged: (Int, Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Student Header
            Text(
                text = "${studentData.student.name} (LRN: ${studentData.student.lrn})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Written Works Section
            Text(
                text = "Written Works (25%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ScoreInputRow(
                scoreType = "WW",
                scores = studentData.writtenWorks,
                maxItems = 5, // Show first 5 WW
                onScoreChanged = { number, rawScore, maxScore ->
                    onWrittenWorkScoreChanged(studentData.student.id, number, rawScore, maxScore)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Tasks Section
            Text(
                text = "Performance Tasks (50%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ScoreInputRow(
                scoreType = "PT",
                scores = studentData.performanceTasks,
                maxItems = 5, // Show first 5 PT
                onScoreChanged = { number, rawScore, maxScore ->
                    onPerformanceTaskScoreChanged(studentData.student.id, number, rawScore, maxScore)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quarterly Assessment Section
            Text(
                text = "Quarterly Assessment (25%)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExamScoreInput(
                score = studentData.quarterlyAssessment,
                onScoreChanged = { rawScore, maxScore ->
                    onExamScoreChanged(studentData.student.id, rawScore, maxScore)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Computed Grade Display
            studentData.computedGrade?.let { grade ->
                ComputedGradeDisplay(grade = grade)
            }
        }
    }
}

@Composable
fun ScoreInputRow(
    scoreType: String,
    scores: Map<Int, ScoreEntry>,
    maxItems: Int,
    onScoreChanged: (Int, Int, Int) -> Unit
) {
    Column {
        for (number in 1..maxItems) {
            val scoreEntry = scores[number]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$scoreType$number:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(50.dp)
                )

                var rawScore by remember { mutableStateOf(scoreEntry?.rawScore?.toString() ?: "") }
                var maxScore by remember { mutableStateOf(scoreEntry?.maxScore?.toString() ?: "100") }

                // Update state when scoreEntry changes
                LaunchedEffect(scoreEntry) {
                    rawScore = scoreEntry?.rawScore?.toString() ?: ""
                    maxScore = scoreEntry?.maxScore?.toString() ?: "100"
                }

                OutlinedTextField(
                    value = rawScore,
                    onValueChange = { newValue ->
                        rawScore = newValue
                        val rawScoreInt = newValue.toIntOrNull() ?: 0
                        val maxScoreInt = maxScore.toIntOrNull() ?: 100
                        if (newValue.isNotBlank() && rawScoreInt >= 0) {
                            onScoreChanged(number, rawScoreInt, maxScoreInt)
                        }
                    },
                    label = { Text("Score") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Text(text = "/", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { newValue ->
                        maxScore = newValue
                        val rawScoreInt = rawScore.toIntOrNull() ?: 0
                        val maxScoreInt = newValue.toIntOrNull() ?: 100
                        if (newValue.isNotBlank() && maxScoreInt > 0) {
                            onScoreChanged(number, rawScoreInt, maxScoreInt)
                        }
                    },
                    label = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun ExamScoreInput(
    score: ScoreEntry?,
    onScoreChanged: (Int, Int) -> Unit
) {
    var rawScore by remember { mutableStateOf(score?.rawScore?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(score?.maxScore?.toString() ?: "100") }

    // Update state when score changes
    LaunchedEffect(score) {
        rawScore = score?.rawScore?.toString() ?: ""
        maxScore = score?.maxScore?.toString() ?: "100"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Exam:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp)
        )

        OutlinedTextField(
            value = rawScore,
            onValueChange = { newValue ->
                rawScore = newValue
                val rawScoreInt = newValue.toIntOrNull() ?: 0
                val maxScoreInt = maxScore.toIntOrNull() ?: 100
                if (newValue.isNotBlank() && rawScoreInt >= 0) {
                    onScoreChanged(rawScoreInt, maxScoreInt)
                }
            },
            label = { Text("Score") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        Text(text = "/", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = maxScore,
            onValueChange = { newValue ->
                maxScore = newValue
                val rawScoreInt = rawScore.toIntOrNull() ?: 0
                val maxScoreInt = newValue.toIntOrNull() ?: 100
                if (newValue.isNotBlank() && maxScoreInt > 0) {
                    onScoreChanged(rawScoreInt, maxScoreInt)
                }
            },
            label = { Text("Max") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
fun ComputedGradeDisplay(grade: ComputedGrade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (grade.transmutedGrade >= 75)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Computed Grade",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("WW: ${String.format("%.2f", grade.wwAverage)}%", fontSize = 12.sp)
                    Text("PT: ${String.format("%.2f", grade.ptAverage)}%", fontSize = 12.sp)
                    Text("Exam: ${String.format("%.2f", grade.examScore)}%", fontSize = 12.sp)
                }
                Column {
                    Text("Initial: ${String.format("%.2f", grade.initialGrade)}%", fontSize = 12.sp)
                    Text(
                        text = "Final: ${grade.transmutedGrade}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (grade.transmutedGrade >= 75)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}