package com.modih.mail.ui.screens.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.GlassCard
import com.modih.mail.ui.components.GlassPill
import com.modih.mail.ui.components.GoldButton
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SupportScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(auth.currentUser?.displayName.orEmpty()) }
    var email by remember { mutableStateOf(auth.currentUser?.email.orEmpty()) }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedMessage = message.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedMessage.isBlank()) {
            error = "All fields are required"
            return
        }
        if (!trimmedEmail.contains("@")) {
            error = "Enter a valid email"
            return
        }
        sending = true
        error = null
        scope.launch {
            val token = prefs.getBrowserToken()
            repo.sendContactMessage(trimmedName, trimmedEmail, trimmedMessage, token)
                .onSuccess {
                    success = true
                    message = ""
                }
                .onFailure { error = it.message ?: "Could not send" }
            sending = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
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
                "Support",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(8.dp))
        GlassPill(text = "Contact us", icon = "✉️")
        Spacer(Modifier.height(12.dp))
        Text(
            "We're here to help",
            fontFamily = CormorantGaramond,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Send us a question, bug report, or upgrade request and we'll reply to your inbox within 24 hours.",
            fontFamily = Inter,
            fontSize = 13.sp,
            color = TextMuted
        )

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible = success, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Success.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.MailOutline,
                        contentDescription = null,
                        tint = Success
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Message sent — we'll get back to you soon!",
                        color = Success,
                        fontSize = 13.sp
                    )
                }
            }
        }

        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Danger.copy(alpha = 0.10f)
            ) {
                Text(
                    error.orEmpty(),
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = supportFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Reply email") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = supportFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it; error = null; success = false },
                label = { Text("Your message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                colors = supportFieldColors()
            )
            Spacer(Modifier.height(16.dp))
            GoldButton(
                text = if (sending) "Sending…" else "Send message",
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending,
                icon = Icons.Filled.Send
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Or email support@modih.in directly.",
            fontSize = 11.sp,
            color = TextDim,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun supportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = GlassBorder,
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentGold
)
