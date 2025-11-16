package com.example.bodyscanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import com.example.bodyscanapp.utils.PerformanceLogger
import kotlinx.coroutines.delay

/**
 * PerformanceDebugScreen - View and test performance logging
 * 
 * Features:
 * - Display all performance logs
 * - Show statistics
 * - Test logging functionality
 * - Clear logs
 * - Auto-refresh capability
 */
@Composable
fun PerformanceDebugScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val performanceLogger = remember { PerformanceLogger.getInstance(context) }
    
    // State for logs and statistics
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var autoRefresh by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    
    // Function to refresh data
    fun refreshData() {
        logs = performanceLogger.getAllLogs()
        stats = performanceLogger.getStatistics()
    }
    
    // Initial load
    LaunchedEffect(Unit) {
        refreshData()
    }
    
    // Auto-refresh every 2 seconds if enabled
    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            delay(2000)
            refreshData()
        }
    }
    
    // Test dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text("Test Performance Logger") },
            text = { 
                Column {
                    Text("This will generate test log entries:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Button click action", style = MaterialTheme.typography.bodySmall)
                    Text("• Timed action (1 second)", style = MaterialTheme.typography.bodySmall)
                    Text("• Navigation event", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Generate test logs
                        performanceLogger.logAction("button_click", "test_button", "test metadata")
                        performanceLogger.startAction("test_action")
                        
                        // Simulate some work and end action
                        Thread {
                            Thread.sleep(1000)
                            performanceLogger.endAction("test_action", "test completed")
                        }.start()
                        
                        performanceLogger.logNavigation("TestScreen1", "TestScreen2")
                        performanceLogger.markScreenVisible("TestScreen2")
                        
                        showTestDialog = false
                        refreshData()
                    }
                ) {
                    Text("Run Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Performance Logs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Back button
            TextButton(onClick = onBackClick) {
                Text("Back")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { refreshData() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
            
            Button(
                onClick = { showTestDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test")
            }
            
            Button(
                onClick = {
                    performanceLogger.clearLogs()
                    refreshData()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Auto-refresh toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto-refresh (2s)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Switch(
                checked = autoRefresh,
                onCheckedChange = { autoRefresh = it }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (stats.isEmpty()) {
                    Text(
                        text = "No statistics available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    stats.forEach { (actionType, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = actionType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logs header
        Text(
            text = "Logs (${logs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Logs list
        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No logs available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use the app to generate performance logs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showTestDialog = true }) {
                            Text("Run Test")
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs.reversed()) { log ->
                        LogEntry(log)
                    }
                }
            }
        }
    }
}

/**
 * Individual log entry display
 */
@Composable
fun LogEntry(log: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = log,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Color.Black,
            modifier = Modifier.padding(8.dp)
        )
    }
}



