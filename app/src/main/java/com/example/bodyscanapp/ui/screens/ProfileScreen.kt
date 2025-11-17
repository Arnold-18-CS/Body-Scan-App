package com.example.bodyscanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bodyscanapp.data.AuthManager
import com.example.bodyscanapp.data.BiometricAuthManager
import com.example.bodyscanapp.data.BiometricAuthStatus
import com.example.bodyscanapp.data.DatabaseModule
import com.example.bodyscanapp.data.UserPreferencesRepository
import com.example.bodyscanapp.repository.ScanRepository
import com.example.bodyscanapp.repository.UserRepository
import com.example.bodyscanapp.ui.theme.BodyScanBackground
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ProfileScreen - Displays user information and settings
 * 
 * Features:
 * - User information (username, email, account creation date)
 * - Scan statistics (total scans, last scan date)
 * - Settings (biometric auth toggle, export preferences)
 * - Account actions (change username, update email, logout, delete account)
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onShowSuccessMessage: (String) -> Unit = {},
    onShowErrorMessage: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize repositories
    val database = remember { DatabaseModule.getDatabase(context) }
    val scanRepository = remember { ScanRepository(database.scanDao()) }
    val userRepository = remember { UserRepository(database.userDao()) }
    val authManager = remember { AuthManager(context) }
    val userPrefsRepo = remember { UserPreferencesRepository(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    // State
    var user by remember { mutableStateOf<com.example.bodyscanapp.data.entity.User?>(null) }
    var totalScans by remember { mutableStateOf(0) }
    var lastScanDate by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    
    // Get current Firebase user
    val firebaseUser = authManager.getCurrentUser()
    
    // Load user data
    LaunchedEffect(Unit) {
        if (firebaseUser != null) {
            // Get or create Room user
            val roomUser = userRepository.getOrCreateUser(
                firebaseUid = firebaseUser.uid,
                username = firebaseUser.displayName ?: "User",
                email = firebaseUser.email
            )
            user = roomUser
            
            // Load scan statistics
            val scanCount = scanRepository.getScanCount(roomUser.id).first()
            totalScans = scanCount
            
            // Get last scan date
            val recentScans = scanRepository.getRecentScans(roomUser.id, 1).first()
            if (recentScans.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                lastScanDate = dateFormat.format(Date(recentScans[0].timestamp))
            }
            
            // Check biometric settings
            biometricEnabled = userPrefsRepo.isBiometricEnabled(firebaseUser.uid)
            val biometricStatus = biometricAuthManager.checkBiometricSupport()
            biometricAvailable = biometricStatus == BiometricAuthStatus.SUCCESS
            
            isLoading = false
        } else {
            isLoading = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BodyScanBackground)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFE0E0E0), CircleShape)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User avatar",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray
                            )
                        }
                        
                        Text(
                            text = user?.username ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        if (firebaseUser?.email != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = firebaseUser.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Divider()
                        
                        // Account creation date
                        user?.let {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text(
                                text = "Member since: ${dateFormat.format(Date(it.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Statistics card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider()
                        
                        Text(
                            text = "Total Scans: $totalScans",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (lastScanDate != null) {
                            Text(
                                text = "Last Scan: $lastScanDate",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                text = "Last Scan: Never",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Settings card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider()
                        
                        // Biometric authentication toggle
                        if (biometricAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Biometric Authentication",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { enabled ->
                                        biometricEnabled = enabled
                                        firebaseUser?.uid?.let { uid ->
                                            userPrefsRepo.setBiometricEnabled(uid, enabled)
                                            onShowSuccessMessage(
                                                if (enabled) "Biometric authentication enabled" 
                                                else "Biometric authentication disabled"
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = "Biometric Authentication (Not Available)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // Account actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider()
                        
                        // Change username button
                        OutlinedButton(
                            onClick = {
                                newUsername = user?.username ?: ""
                                showEditUsernameDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Change Username")
                        }
                        
                        // Logout button
                        OutlinedButton(
                            onClick = onLogoutClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Logout")
                        }
                        
                        // Delete account button
                        Button(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Delete Account")
                        }
                    }
                }
            }
        }
    }
    
    // Edit username dialog
    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Change Username") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.isNotBlank() && firebaseUser != null) {
                            coroutineScope.launch {
                                try {
                                    userPrefsRepo.setUsername(firebaseUser.uid, newUsername)
                                    // Update Room user
                                    user?.let {
                                        val updatedUser = it.copy(username = newUsername)
                                        userRepository.updateUser(updatedUser)
                                        user = updatedUser
                                    }
                                    showEditUsernameDialog = false
                                    onShowSuccessMessage("Username updated successfully")
                                } catch (e: Exception) {
                                    onShowErrorMessage("Failed to update username: ${e.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete account dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = {
                Text(
                    "Are you sure you want to delete your account? " +
                    "This will permanently delete all your scans and data. " +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting && firebaseUser != null && user != null) {
                            isDeleting = true
                            coroutineScope.launch {
                                try {
                                    // Delete all scans first
                                    val scans = scanRepository.getScansByUser(user!!.id).first()
                                    scans.forEach { scan ->
                                        scanRepository.deleteScan(scan)
                                        // Delete mesh file
                                        try {
                                            val meshFile = java.io.File(scan.meshPath)
                                            if (meshFile.exists()) {
                                                meshFile.delete()
                                            }
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                    
                                    // Delete Room user
                                    userRepository.deleteUser(user!!)
                                    
                                    // Delete Firebase account
                                    authManager.deleteAccount().collect { result ->
                                        when (result) {
                                            is com.example.bodyscanapp.data.AuthResult.Success -> {
                                                isDeleting = false
                                                showDeleteAccountDialog = false
                                                onLogoutClick() // This will navigate to login
                                            }
                                            is com.example.bodyscanapp.data.AuthResult.Error -> {
                                                isDeleting = false
                                                onShowErrorMessage("Failed to delete account: ${result.message}")
                                            }
                                            else -> {}
                                        }
                                    }
                                } catch (e: Exception) {
                                    isDeleting = false
                                    onShowErrorMessage("Failed to delete account: ${e.message}")
                                }
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Delete", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isDeleting) showDeleteAccountDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

