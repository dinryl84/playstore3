package com.example.gradingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gradingapp.ui.viewmodel.ScoreInputViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreInputScreen(
    onBackClick: () -> Unit,
    viewModel: ScoreInputViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Score Input System") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters Card
            item {
                FiltersCard(
                    sections = uiState.sections,
                    availableSubjects = uiState.availableSubjects,
                    selectedSection = uiState.selectedSection,
                    selectedSubject = uiState.selectedSubject,
                    selectedSemester = uiState.selectedSemester,
                    selectedQuarter = uiState.selectedQuarter,
                    onSectionSelected = viewModel::onSectionSelected,
                    onSubjectSelected = viewModel::onSubjectSelected,
                    onSemesterSelected = viewModel::onSemesterSelected,
                    onQuarterSelected = viewModel::onQuarterSelected
                )
            }

            // Student Score Cards
            items(uiState.studentsWithScores) { studentData ->
                StudentScoreCard(
                    studentData = studentData,
                    onUpdateWrittenWork = viewModel::updateWrittenWorkScore,
                    onUpdatePerformanceTask = viewModel::updatePerformanceTaskScore,
                    onUpdateQuarterlyAssessment = viewModel::updateQuarterlyAssessmentScore
                )
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersCard(
    sections: List<Any>, // Using Any for now to avoid type errors
    availableSubjects: List<Any>,
    selectedSection: Any?,
    selectedSubject: Any?,
    selectedSemester: Int?,
    selectedQuarter: Int?,
    onSectionSelected: (Int) -> Unit,
    onSubjectSelected: (Int) -> Unit,
    onSemesterSelected: (Int) -> Unit,
    onQuarterSelected: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍",
                    fontSize = 18.sp
                )
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // First Row - Section and Subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Dropdown
                var sectionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Unity", // Placeholder from your mockup
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Section") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        // Placeholder dropdown items
                        DropdownMenuItem(
                            text = { Text("Unity") },
                            onClick = {
                                onSectionSelected(1)
                                sectionExpanded = false
                            }
                        )
                    }
                }

                // Subject Dropdown
                var subjectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "General Math", // Placeholder from your mockup
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Math") },
                            onClick = {
                                onSubjectSelected(1)
                                subjectExpanded = false
                            }
                        )
                    }
                }
            }

            // Second Row - Semester and Quarter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Semester Dropdown
                var semesterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = semesterExpanded,
                    onExpandedChange = { semesterExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Sem 1", // Placeholder from your mockup
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Semester") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
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
                    onExpandedChange = { quarterExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Q1", // Placeholder from your mockup
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quarter") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
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
private fun StudentScoreCard(
    studentData: Any, // Using Any for now to avoid type errors
    onUpdateWrittenWork: (Int, Int, Int, Int) -> Unit,
    onUpdatePerformanceTask: (Int, Int, Int, Int) -> Unit,
    onUpdateQuarterlyAssessment: (Int, Int, Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Header - Using placeholder data from mockup
            StudentHeader(
                studentName = "Basisgig One",
                studentLrn = "1111111111"
            )

            // Written Works Section
            WrittenWorksSection(
                onUpdateScore = { number, rawScore, maxScore ->
                    onUpdateWrittenWork(1, number, rawScore, maxScore) // Using placeholder student ID
                }
            )

            // Performance Tasks Section
            PerformanceTasksSection(
                onUpdateScore = { number, rawScore, maxScore ->
                    onUpdatePerformanceTask(1, number, rawScore, maxScore) // Using placeholder student ID
                }
            )

            // Quarterly Assessment Section
            QuarterlyAssessmentSection(
                onUpdateScore = { rawScore, maxScore ->
                    onUpdateQuarterlyAssessment(1, rawScore, maxScore) // Using placeholder student ID
                }
            )

            // Computed Grade Section
            ComputedGradeSection()
        }
    }
}

@Composable
private fun StudentHeader(
    studentName: String,
    studentLrn: String
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "👤",
                fontSize = 16.sp
            )
            Text(
                text = "Student: $studentName (LRN: $studentLrn)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WrittenWorksSection(
    onUpdateScore: (Int, Int, Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "✍️", fontSize = 16.sp)
                Text(
                    text = "Written Works (25%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // Score Input Grid - matching your mockup
            ScoreInputGrid(
                prefix = "WW",
                onUpdateScore = onUpdateScore,
                mockupScores = mapOf(
                    1 to Pair(50, 50),
                    2 to Pair(50, 100),
                    3 to Pair(20, 40)
                )
            )
        }
    }
}

@Composable
private fun PerformanceTasksSection(
    onUpdateScore: (Int, Int, Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🏆", fontSize = 16.sp)
                Text(
                    text = "Performance Tasks (50%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // Score Input Grid - showing PT3, PT4, PT5 as in your mockup
            ScoreInputGrid(
                prefix = "PT",
                onUpdateScore = onUpdateScore,
                mockupScores = emptyMap(), // Empty as shown in mockup
                startNumber = 3 // Start from PT3 as shown in mockup
            )
        }
    }
}

@Composable
private fun QuarterlyAssessmentSection(
    onUpdateScore: (Int, Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🧪", fontSize = 16.sp)
                Text(
                    text = "Quarterly Assessment (25%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Exam:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(48.dp)
                )

                var rawScoreText by remember { mutableStateOf("30") } // From your mockup
                var maxScoreText by remember { mutableStateOf("40") } // From your mockup

                OutlinedTextField(
                    value = rawScoreText,
                    onValueChange = { newValue ->
                        rawScoreText = newValue
                        val rawScore = newValue.toIntOrNull()
                        val maxScore = maxScoreText.toIntOrNull()
                        if (rawScore != null && maxScore != null && maxScore > 0) {
                            onUpdateScore(rawScore, maxScore)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                Text(
                    text = "/",
                    style = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = maxScoreText,
                    onValueChange = { newValue ->
                        maxScoreText = newValue
                        val rawScore = rawScoreText.toIntOrNull()
                        val maxScore = newValue.toIntOrNull()
                        if (rawScore != null && maxScore != null && maxScore > 0) {
                            onUpdateScore(rawScore, maxScore)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
private fun ScoreInputGrid(
    prefix: String,
    onUpdateScore: (Int, Int, Int) -> Unit,
    mockupScores: Map<Int, Pair<Int, Int>> = emptyMap(),
    startNumber: Int = 1
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in startNumber..startNumber + 2) {
                ScoreInputItem(
                    number = i,
                    prefix = prefix,
                    onUpdateScore = onUpdateScore,
                    modifier = Modifier.weight(1f),
                    initialRawScore = mockupScores[i]?.first,
                    initialMaxScore = mockupScores[i]?.second
                )
            }
        }

        // Second row - only for items 4 and 5
        if (startNumber == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 4..5) {
                    ScoreInputItem(
                        number = i,
                        prefix = prefix,
                        onUpdateScore = onUpdateScore,
                        modifier = Modifier.weight(1f),
                        initialRawScore = mockupScores[i]?.first,
                        initialMaxScore = mockupScores[i]?.second
                    )
                }
                // Empty space for alignment
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreInputItem(
    number: Int,
    prefix: String,
    onUpdateScore: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    initialRawScore: Int? = null,
    initialMaxScore: Int? = null
) {
    var rawScoreText by remember { mutableStateOf(initialRawScore?.toString() ?: "") }
    var maxScoreText by remember { mutableStateOf(initialMaxScore?.toString() ?: "") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$prefix$number:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = rawScoreText,
                onValueChange = { newValue ->
                    rawScoreText = newValue
                    val rawScore = newValue.toIntOrNull()
                    val maxScore = maxScoreText.toIntOrNull()
                    if (rawScore != null && maxScore != null && maxScore > 0) {
                        onUpdateScore(number, rawScore, maxScore)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(60.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            )

            Text(
                text = "/",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = maxScoreText,
                onValueChange = { newValue ->
                    maxScoreText = newValue
                    val rawScore = rawScoreText.toIntOrNull()
                    val maxScore = newValue.toIntOrNull()
                    if (rawScore != null && maxScore != null && maxScore > 0) {
                        onUpdateScore(number, rawScore, maxScore)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(60.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun ComputedGradeSection() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color(0xFFE8F5E8) // Light green from your mockup
        ),
        border = BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📊", fontSize = 16.sp)
                Text(
                    text = "Computed Grade",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // Component scores row - using values from your mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComponentScore(label = "WW", percentage = "66.67%")
                ComponentScore(label = "PT", percentage = "89.00%")
                ComponentScore(label = "Exam", percentage = "75.00%")
            }

            // Final grades row - using values from your mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FinalGradeScore(label = "Initial", value = "79.92%")
                FinalGradeScore(
                    label = "Final",
                    value = "87",
                    isHighlighted = true,
                    isPassing = true
                )
            }
        }
    }
}

@Composable
private fun ComponentScore(label: String, percentage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = percentage,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FinalGradeScore(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    isPassing: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = if (isHighlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) {
                if (isPassing) Color(0xFF4CAF50) else Color(0xFFF44336)
            } else LocalContentColor.current
        )
    }
}