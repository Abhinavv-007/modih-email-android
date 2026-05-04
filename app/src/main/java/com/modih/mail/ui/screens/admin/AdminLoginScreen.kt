package com.modih.mail.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.GlassCard
import com.modih.mail.ui.components.GoldButton
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminLoginScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var secret by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val storedSecret by prefs.adminSecretFlow.collectAsState(initial = null)
    LaunchedEffect(storedSecret) {
        // If we already have a saved admin secret, jump straight to the panel.
        if (!storedSecret.isNullOrBlank()) {
            navController.navigate(Screen.AdminPanel.route) {
                popUpTo(Screen.AdminLogin.route) { inclusive = true }
            }
        }
    }

    fun submit() {
        val trimmed = secret.trim()
        if (trimmed.isBlank()) {
            error = "Enter the admin secret"
            return
        }
        keyboard?.hide()
        isLoading = true
        error = null
        scope.launch {
            repo.fetchAdminSnapshot(adminSecret = trimmed, filter = "all", rangeKey = "7d")
                .onSuccess {
                    prefs.saveAdminSecret(trimmed)
                    navController.navigate(Screen.AdminPanel.route) {
                        popUpTo(Screen.AdminLogin.route) { inclusive = true }
                    }
                }
                .onFailure { error = it.message ?: "Invalid admin secret" }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Admin",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = AccentGoldSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Restricted area",
            fontFamily = CormorantGaramond,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Enter the admin secret to manage users, inboxes, and plan changes.",
            fontFamily = Inter,
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it; error = null },
                label = { Text("Admin secret") },
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showSecret) "Hide secret" else "Show secret",
                            tint = TextMuted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = AccentGold,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGold
                )
            )

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Danger.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            error.orEmpty(),
                            color = Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            GoldButton(
                text = if (isLoading) "Verifying…" else "Unlock admin panel",
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && secret.isNotBlank()
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Tip — the secret is stored locally for this device only and is sent over HTTPS.",
            fontSize = 11.sp,
            color = TextDim,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
