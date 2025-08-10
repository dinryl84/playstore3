package com.example.gradingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gradingapp.data.entity.SectionEntity
import com.example.gradingapp.ui.viewmodel.SectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionManagementScreen(
    navController: NavController,
    sectionViewModel: SectionViewModel = hiltViewModel()
) {
    val uiState by sectionViewModel.sectionUiState.collectAsState()
    val sections = uiState.sections
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage

    var sectionName by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var editingSection by remember { mutableStateOf<SectionEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<SectionEntity?>(null) }

    // Clear form when editing is cancelled
    LaunchedEffect(editingSection) {
        if (editingSection == null) {
            sectionName = ""
            gradeLevel = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .height(56.dp)
        ) {
            IconButton(
                onClick = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Section Management",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Success Message
        successMessage?.let { message ->
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

        // Error Message
        errorMessage?.let { message ->
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

        // Add/Edit Section Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (editingSection == null) "Add New Section" else "Edit Section",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("Section Name") },
                    placeholder = { Text("e.g., Grade 11 - Einstein") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text("Grade Level") },
                    placeholder = { Text("e.g., Grade 11") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (editingSection == null) {
                                sectionViewModel.addSection(sectionName.trim(), gradeLevel.trim())
                            } else {
                                sectionViewModel.updateSection(
                                    editingSection!!,
                                    sectionName.trim(),
                                    gradeLevel.trim()
                                )
                                editingSection = null
                            }
                            sectionName = ""
                            gradeLevel = ""
                        },
                        enabled = !isLoading && sectionName.isNotBlank() && gradeLevel.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (editingSection == null) "Add Section" else "Update Section")
                        }
                    }

                    if (editingSection != null) {
                        OutlinedButton(
                            onClick = {
                                editingSection = null
                                sectionName = ""
                                gradeLevel = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        // Sections List
        Text(
            text = "Existing Sections (${sections.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sections) { section ->
                SectionItem(
                    section = section,
                    onEdit = {
                        editingSection = section
                        sectionName = section.name
                        gradeLevel = section.level
                        sectionViewModel.clearMessages()
                    },
                    onDelete = {
                        showDeleteDialog = section
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { section ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Section") },
            text = {
                Text("Are you sure you want to delete \"${section.name}\"?\n\nThis action cannot be undone and may affect students assigned to this section.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        sectionViewModel.deleteSection(section)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear messages after some time
    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            sectionViewModel.clearMessages()
        }
    }
}

@Composable
fun SectionItem(
    section: SectionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = section.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = section.level,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Section",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Section",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}