package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.Student
import com.wade.school.data.local.entity.StudentHealthInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeroomStudentListScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    val students by viewModel.homeroomStudents.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredStudents = students.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.studentId.contains(searchQuery) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 學生名冊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("搜尋姓名或學號...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(filteredStudents) { student ->
                    StudentHomeroomCard(student, viewModel) {
                        onNavigateToDetail(student.studentId)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentHomeroomCard(student: Student, viewModel: CounselorViewModel, onClick: () -> Unit) {
    var healthInfo by remember { mutableStateOf<StudentHealthInfo?>(null) }
    
    LaunchedEffect(student.studentId) {
        healthInfo = viewModel.getStudentHealth(student.studentId)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${student.seatNo}", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (healthInfo?.iepStudent == true) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 8.dp)) { Text("IEP") }
                    }
                }
                Text("學號: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
                
                if (healthInfo != null) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        healthInfo?.bloodType?.let { Text("血型: $it ", style = MaterialTheme.typography.labelSmall) }
                        healthInfo?.allergies?.let { Text("| 過敏: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
