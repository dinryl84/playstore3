package com.example.gradingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gradingapp.data.entity.StudentEntity
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    navController: NavController,
    studentViewModel: StudentViewModel = hiltViewModel()
) {
    val studentUiState by studentViewModel.studentUiState.collectAsState()

    var studentName by remember { mutableStateOf("") }
    var studentLrn by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var selectedSectionId by remember { mutableIntStateOf(0) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var expandedSectionDropdown by remember { mutableStateOf(false) }

    // Auto-clear messages
    LaunchedEffect(studentUiState.errorMessage, studentUiState.successMessage) {
        if (studentUiState.errorMessage?.isNotEmpty() == true || studentUiState.successMessage?.isNotEmpty() == true) {
            delay(3000)
            studentViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp) // Safe area padding
    ) {
        // Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Student Management",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add, edit, and manage students",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Add/Edit Student Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (editingStudent != null) "Edit Student" else "Add New Student",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Student Name Input
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        isError = studentName.isNotBlank() && studentName.trim().length < 2,
                        supportingText = {
                            if (studentName.isNotBlank() && studentName.trim().length < 2) {
                                Text("Name must be at least 2 characters long")
                            }
                        }
                    )

                    // LRN Input
                    OutlinedTextField(
                        value = studentLrn,
                        onValueChange = {
                            if (it.length <= 15) { // Limit to 15 characters
                                studentLrn = it.filter { char -> char.isLetterOrDigit() } // Only alphanumeric
                            }
                        },
                        label = { Text("LRN (Learner's Reference Number)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = studentLrn.isNotBlank() && (studentLrn.length < 10 || studentLrn.length > 15),
                        supportingText = {
                            if (studentLrn.isNotBlank() && (studentLrn.length < 10 || studentLrn.length > 15)) {
                                Text("LRN must be 10-15 alphanumeric characters")
                            }
                        }
                    )

                    // Gender Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedGender,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedDropdown
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            listOf("Male", "Female", "Other").forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender) },
                                    onClick = {
                                        selectedGender = gender
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Section Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedSectionDropdown,
                        onExpandedChange = { expandedSectionDropdown = !expandedSectionDropdown },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = studentUiState.sections.find { it.id == selectedSectionId }?.name ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Section") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedSectionDropdown
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            isError = selectedSectionId == 0 && studentUiState.sections.isNotEmpty(),
                            supportingText = {
                                if (selectedSectionId == 0 && studentUiState.sections.isNotEmpty()) {
                                    Text("Please select a section")
                                }
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSectionDropdown,
                            onDismissRequest = { expandedSectionDropdown = false }
                        ) {
                            studentUiState.sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text("${section.name} (${section.level})") },
                                    onClick = {
                                        selectedSectionId = section.id
                                        expandedSectionDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingStudent != null) {
                            OutlinedButton(
                                onClick = {
                                    // Cancel editing
                                    editingStudent = null
                                    studentName = ""
                                    studentLrn = ""
                                    selectedGender = ""
                                    selectedSectionId = 0
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }

                        Button(
                            onClick = {
                                val trimmedName = studentName.trim()
                                val trimmedLrn = studentLrn.trim()

                                if (editingStudent != null) {
                                    // Update student
                                    studentViewModel.updateStudent(
                                        student = editingStudent!!,
                                        newName = trimmedName,
                                        newLrn = trimmedLrn,
                                        newGender = selectedGender,
                                        newSectionId = selectedSectionId
                                    )
                                    editingStudent = null
                                } else {
                                    // Add new student
                                    studentViewModel.addStudent(
                                        name = trimmedName,
                                        lrn = trimmedLrn,
                                        gender = selectedGender,
                                        sectionId = selectedSectionId
                                    )
                                }

                                // Clear form
                                studentName = ""
                                studentLrn = ""
                                selectedGender = ""
                                selectedSectionId = 0
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !studentUiState.isLoading &&
                                    studentName.trim().length >= 2 &&
                                    studentLrn.length in 10..15 &&
                                    selectedGender.isNotEmpty() &&
                                    selectedSectionId != 0
                        ) {
                            if (studentUiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (editingStudent != null) "Update" else "Add Student")
                            }
                        }
                    }
                }
            }

            // Success/Error Messages
            studentUiState.successMessage?.let { message ->
                if (message.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            studentUiState.errorMessage?.let { message ->
                if (message.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Students List
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Students List",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${studentUiState.students.size} students",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (studentUiState.students.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No students added yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentUiState.students) { student ->
                                StudentItem(
                                    student = student,
                                    section = studentUiState.sections.find { it.id == student.sectionId },
                                    onEditClick = {
                                        editingStudent = student
                                        studentName = student.name
                                        studentLrn = student.lrn
                                        selectedGender = student.gender
                                        selectedSectionId = student.sectionId
                                    },
                                    onDeleteClick = {
                                        showDeleteDialog = student
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { student ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Student") },
            text = {
                Text("Are you sure you want to delete \"${student.name}\"?\n\nThis will also delete all associated scores and cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        studentViewModel.deleteStudent(student)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentItem(
    student: StudentEntity,
    section: SectionEntity?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "LRN: ${student.lrn}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Gender: ${student.gender}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Section: ${section?.name ?: "Unknown"} (${section?.level ?: "N/A"})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ID: ${student.id}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Student",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Student",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}