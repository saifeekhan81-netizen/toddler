package com.example.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.domain.ActivityType
import com.example.domain.ChildAge
import com.example.domain.SessionDuration
import com.example.domain.SoundLevel
import com.example.security.NativeProtectionService
import com.example.security.ParentAuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    userPreferences: UserPreferences,
    preferencesRepository: PreferencesRepository,
    protectionService: NativeProtectionService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Child Age Section
            SettingsCard(title = "Default Child Age", icon = Icons.Rounded.ChildCare) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChildAge.entries.forEach { age ->
                        FilterChip(
                            selected = userPreferences.childAge == age,
                            onClick = { scope.launch { preferencesRepository.updateChildAge(age) } },
                            label = { Text(age.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Session Duration Section
            SettingsCard(title = "Default Play Duration", icon = Icons.Rounded.Security) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SessionDuration.entries.forEach { dur ->
                        FilterChip(
                            selected = userPreferences.sessionDuration == dur,
                            onClick = { scope.launch { preferencesRepository.updateSessionDuration(dur) } },
                            label = { Text(dur.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 3. Sound & Haptics Section
            SettingsCard(title = "Sound & Feedback", icon = Icons.Rounded.VolumeUp) {
                Text(
                    text = "Sound Level",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SoundLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = userPreferences.soundLevel == lvl,
                            onClick = { scope.launch { preferencesRepository.updateSoundLevel(lvl) } },
                            label = { Text(lvl.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Soft Haptic Feedback", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = userPreferences.hapticsEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateHapticsEnabled(it) } }
                    )
                }
            }

            // 4. Enabled Activities Section
            SettingsCard(title = "Enabled Toddler Activities", icon = Icons.Rounded.ChildCare) {
                Text(
                    text = "Choose which activities appear during play sessions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActivityType.entries.forEach { activity ->
                    val isChecked = userPreferences.enabledActivities.contains(activity.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activity.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(activity.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    preferencesRepository.toggleActivityEnabled(activity.id, checked)
                                }
                            }
                        )
                    }
                }
            }

            // 5. Activity Auto-Switch
            SettingsCard(title = "Auto-Switch Activities", icon = Icons.Rounded.Refresh) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatically switch activity", fontWeight = FontWeight.Medium)
                        Text("Gently transitions to the next activity without sudden cuts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = userPreferences.autoSwitchEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateAutoSwitchEnabled(it) } }
                    )
                }
                if (userPreferences.autoSwitchEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 5).forEach { mins ->
                            FilterChip(
                                selected = userPreferences.autoSwitchIntervalMinutes == mins,
                                onClick = { scope.launch { preferencesRepository.updateAutoSwitchInterval(mins) } },
                                label = { Text("Every $mins min") }
                            )
                        }
                    }
                }
            }

            // 6. Security & Protection
            SettingsCard(title = "Security & PIN", icon = Icons.Rounded.Lock) {
                Button(
                    onClick = { showChangePinDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Parent PIN")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAdvancedDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advanced Protection Guide")
                }
            }

            // 7. Privacy & About
            SettingsCard(title = "About & Privacy", icon = Icons.Rounded.Info) {
                OutlinedButton(
                    onClick = { showAboutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Privacy & Device Philosophy")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Preferences to Default", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Dialog: Change PIN
    if (showChangePinDialog) {
        ChangePinDialog(
            userPreferences = userPreferences,
            onDismiss = { showChangePinDialog = false },
            onPinChanged = { newPin ->
                scope.launch {
                    preferencesRepository.savePin(newPin)
                    showChangePinDialog = false
                }
            }
        )
    }

    // Dialog: Advanced Protection
    if (showAdvancedDialog) {
        AlertDialog(
            onDismissRequest = { showAdvancedDialog = false },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Advanced Protection Modes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TinyHands offers layered Android protection:\n", style = MaterialTheme.typography.bodySmall)
                    Text("1. Standard Safe Mode (Active):", fontWeight = FontWeight.Bold)
                    Text("Immersive fullscreen hides status & navigation bars. In-app back buttons are disabled and exit is locked behind a 3-finger hold & PIN.", style = MaterialTheme.typography.bodySmall)

                    Text("2. Android Screen Pinning:", fontWeight = FontWeight.Bold)
                    Text("Pins TinyHands to the front of the screen. To unpin, Android requires pressing and holding Back and Overview/Home.", style = MaterialTheme.typography.bodySmall)

                    Text("3. Dedicated Device / Kiosk Mode:", fontWeight = FontWeight.Bold)
                    Text("For corporate or educational dedicated child tablets provisioned with Device Owner, Lock Task mode completely locks the device to TinyHands.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdvancedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: About & Privacy
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("TinyHands Privacy Pledge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 100% Offline: Never connects to the internet or external servers.", style = MaterialTheme.typography.bodySmall)
                    Text("• Zero Data Collection: No names, no telemetry, no analytics, no ads.", style = MaterialTheme.typography.bodySmall)
                    Text("• Zero Camera Access: Play Camera is completely simulated and never accesses hardware or saves photos.", style = MaterialTheme.typography.bodySmall)
                    Text("• Local Storage Only: Preferences and salted hashed PIN are stored only on your local device.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // Dialog: Reset Confirm
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Preferences?") },
            text = { Text("This will restore default activity selection, timer, and sound preferences. Your PIN will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesRepository.resetPreferences()
                            showResetConfirmDialog = false
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ChangePinDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Parent PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4) oldPin = it },
                    label = { Text("Current 4-digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!ParentAuthManager.verifyPin(oldPin, userPreferences.pinHash, userPreferences.pinSalt)) {
                        errorMessage = "Current PIN is incorrect."
                    } else if (newPin.length != 4) {
                        errorMessage = "New PIN must be 4 digits."
                    } else if (newPin != confirmPin) {
                        errorMessage = "New PINs do not match."
                    } else {
                        onPinChanged(newPin)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
