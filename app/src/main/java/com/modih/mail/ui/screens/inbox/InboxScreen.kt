package com.modih.mail.ui.screens.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.model.*
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InboxScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MailRepository() }
    val prefs = remember { PreferencesManager(context) }
    val auth = remember { FirebaseAuth.getInstance() }
    val userPlanState = rememberUserPlanState()

    var currentInbox by remember { mutableStateOf<Inbox?>(null) }
    var messages by remember { mutableStateOf<List<MailMessage>>(emptyList()) }
    var isCreating by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var customPrefix by remember { mutableStateOf("") }
    var showMailView by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf("") }
    val backToPrevious = {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                launchSingleTop = true
            }
        }
    }

    val userPlan = userPlanState.plan

    // Countdown timer
    LaunchedEffect(currentInbox) {
        currentInbox?.let { inbox ->
            while (true) {
                val remaining = inbox.expiresAt - (System.currentTimeMillis() / 1000)
                if (remaining <= 0) {
                    countdown = "Expired"
                    break
                }
                val h = remaining / 3600
                val m = (remaining % 3600) / 60
                val s = remaining % 60
                countdown = String.format("%02d:%02d:%02d", h, m, s)
                delay(1000)
            }
        }
    }

    // Auto-refresh messages
    LaunchedEffect(currentInbox, showMailView) {
        if (currentInbox != null && showMailView) {
            while (true) {
                delay(5000)
                try {
                    val token = auth.currentUser?.getIdToken(false)?.await()?.token
                    repo.getMessages(currentInbox!!.id, currentInbox!!.ownerToken, token)
                        .onSuccess { messages = it }
                } catch (_: Exception) {}
            }
        }
    }

    fun createInbox(type: String) {
        isCreating = true; error = null
        scope.launch {
            try {
                val browserToken = prefs.getBrowserToken()
                val authToken = auth.currentUser?.getIdToken(false)?.await()?.token
                val prefix = if (type == "custom" && customPrefix.isNotBlank()) customPrefix else null

                repo.createInbox(prefix, browserToken, authToken, currentInbox?.ownerToken)
                    .onSuccess { inbox ->
                        currentInbox = inbox
                        messages = emptyList()
                        prefs.saveCurrentInbox(inbox.id, inbox.email, inbox.ownerToken, inbox.createdAt, inbox.expiresAt)
                        showMailView = true
                    }
                    .onFailure { error = it.message }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            isCreating = false
        }
    }

    fun copyEmail() {
        currentInbox?.let {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("email", it.email))
            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteInbox() {
        scope.launch {
            currentInbox?.let { inbox ->
                val authToken = auth.currentUser?.getIdToken(false)?.await()?.token
                repo.deleteInbox(inbox.id, inbox.ownerToken, authToken)
                currentInbox = null
                messages = emptyList()
                showMailView = false
                prefs.clearCurrentInbox()
            }
        }
    }

    fun refreshMessages() {
        isLoading = true
        scope.launch {
            currentInbox?.let { inbox ->
                val authToken = auth.currentUser?.getIdToken(false)?.await()?.token
                repo.getMessages(inbox.id, inbox.ownerToken, authToken)
                    .onSuccess { messages = it }
                    .onFailure { error = it.message }
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        if (!showMailView) {
            // Generate screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = backToPrevious) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Inbox",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Create a fresh address in seconds",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                GlassPill(text = "Get Your Email", icon = "✦")
                Spacer(Modifier.height(16.dp))

                Text(
                    "Create your inbox",
                    fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic, fontSize = 32.sp, color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Generate a random address — your inbox will be ready instantly.",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Custom prefix input (for Pro/Dev)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPrefix,
                            onValueChange = { customPrefix = it.take(30) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Custom prefix", color = TextDim) },
                            singleLine = true,
                            enabled = userPlan != Plan.FREE,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold.copy(alpha = 0.5f),
                                unfocusedBorderColor = GlassBorder,
                                disabledBorderColor = GlassBorder.copy(alpha = 0.3f),
                                cursorColor = AccentGold,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextDim
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassBgCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "@modih.in",
                                color = AccentGoldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                            )
                        }
                    }

                    if (userPlan == Plan.FREE) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlanBadge("pro")
                            Text(
                                "Custom prefix requires Pro plan",
                                fontSize = 12.sp,
                                color = TextDim
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GhostButton(
                            text = "Custom Name",
                            onClick = { createInbox("custom") },
                            modifier = Modifier.weight(1f),
                            enabled = userPlan != Plan.FREE && !isCreating
                        )
                        GoldButton(
                            text = "Random ✦",
                            onClick = { createInbox("random") },
                            modifier = Modifier.weight(1f),
                            isLoading = isCreating
                        )
                    }
                }

                // Error
                AnimatedVisibility(visible = error != null) {
                    Text(
                        error ?: "", color = Danger, fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    )
                }

                // Show current inbox result if exists
                currentInbox?.let { inbox ->
                    Spacer(Modifier.height(24.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Your private inbox", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                inbox.email,
                                fontFamily = Inter, fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp, color = AccentGold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { copyEmail() }) {
                                Icon(Icons.Outlined.ContentCopy, "Copy", tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Countdown
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, "Timer", tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(countdown, fontSize = 14.sp, color = TextMuted, fontFamily = Inter)
                        }

                        Spacer(Modifier.height(16.dp))

                        GoldButton(
                            text = "Open Mail Window",
                            onClick = { showMailView = true; refreshMessages() },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.Email
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            GhostButton("New Address", { createInbox("random") }, Modifier.weight(1f))
                            GhostButton("Delete", { deleteInbox() }, Modifier.weight(1f), color = Danger)
                        }
                    }
                }
            }
        } else {
            // Mail view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Mail header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BgPrimary.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showMailView = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentInbox?.email ?: "",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = TextPrimary, fontFamily = Inter, maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Timer, null, Modifier.size(12.dp), tint = TextMuted)
                                Spacer(Modifier.width(4.dp))
                                Text(countdown, fontSize = 12.sp, color = TextMuted)
                            }
                        }
                        IconButton(onClick = { copyEmail() }) {
                            Icon(Icons.Outlined.ContentCopy, "Copy", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { refreshMessages() }) {
                            Icon(Icons.Outlined.Refresh, "Refresh", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { deleteInbox() }) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                if (messages.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📧", fontSize = 64.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Waiting for emails...", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Send an email to ${currentInbox?.email} and it will appear here.",
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Auto-refreshing every 5 seconds", style = MaterialTheme.typography.bodySmall, color = TextDim)

                        Spacer(Modifier.height(16.dp))
                        // Loading dots animation
                        val infiniteTransition = rememberInfiniteTransition(label = "dots")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800), repeatMode = RepeatMode.Reverse
                            ), label = "dotAlpha"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGold.copy(alpha = if (i == 0) alpha else if (i == 1) 1f - alpha + 0.3f else alpha))
                                )
                            }
                        }
                    }
                } else {
                    // Message list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageCard(
                                message = msg,
                                onClick = {
                                    navController.navigate(Screen.MessageDetail.createRoute(msg.id))
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
private fun MessageCard(message: MailMessage, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = GlassBgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.from,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = TextPrimary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    dateFormat.format(Date(message.receivedAt * 1000)),
                    fontSize = 12.sp, color = TextDim
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                message.subject,
                fontWeight = FontWeight.Medium, fontSize = 15.sp,
                color = TextSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // OTP badge
            if (!message.otp.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Success.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("OTP:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Success)
                        Text(message.otp, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Success, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}
