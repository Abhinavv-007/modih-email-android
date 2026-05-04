package com.modih.mail.ui.screens.developer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.model.ApiKey
import com.modih.mail.data.model.Plan
import com.modih.mail.data.model.UsageStats
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val repo = remember { MailRepository() }
    val scope = rememberCoroutineScope()
    val userPlanState = rememberUserPlanState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var keyName by remember { mutableStateOf("Android Key") }
    var keys by remember { mutableStateOf<List<ApiKey>>(emptyList()) }
    var usage by remember {
        mutableStateOf(
            UsageStats(
                inboxesLimit = 5000,
                readsLimit = 50000,
                apiKeysLimit = 10
            )
        )
    }
    var revealedKey by remember { mutableStateOf<ApiKey?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var isRotating by remember { mutableStateOf(false) }
    var isRevoking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun currentAuthToken(): String? =
        auth.currentUser?.getIdToken(false)?.await()?.token

    suspend fun refreshDeveloperData() {
        val authToken = currentAuthToken()
        if (authToken.isNullOrBlank()) {
            error = "Please sign in again."
            isLoading = false
            return
        }

        val usageResult = repo.getDeveloperUsage(authToken)
        val keysResult = repo.getDeveloperKeys(authToken)

        usageResult.onSuccess { usageData ->
            usage = usage.copy(
                inboxesCreated = usageData.inboxesCreated,
                inboxesLimit = usageData.inboxesLimit,
                readsUsed = usageData.readsUsed,
                readsLimit = usageData.readsLimit
            )
        }.onFailure {
            error = it.localizedMessage ?: "Could not load API usage."
        }

        keysResult.onSuccess { loadedKeys ->
            keys = loadedKeys
            usage = usage.copy(apiKeysActive = loadedKeys.count { it.isActive })
        }.onFailure {
            error = it.localizedMessage ?: "Could not load API keys."
        }

        isLoading = false
    }

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
    }

    fun createKey(rotateAfterId: String? = null) {
        scope.launch {
            val authToken = currentAuthToken()
            if (authToken.isNullOrBlank()) {
                error = "Please sign in again."
                return@launch
            }

            if (rotateAfterId == null) {
                isCreating = true
            } else {
                isRotating = true
            }
            error = null
            message = null

            val requestedName = keyName.trim().ifBlank { "Android Key" }
            val createResult = repo.createDeveloperKey(authToken, requestedName)

            createResult.onSuccess { createdKey ->
                revealedKey = createdKey
                message = if (rotateAfterId == null) {
                    "API key created. Copy it now; it will only be shown once."
                } else {
                    "API key rotated. Copy the new key now."
                }
                if (rotateAfterId != null) {
                    scope.launch {
                        repo.revokeDeveloperKey(authToken, rotateAfterId)
                        refreshDeveloperData()
                    }
                } else {
                    scope.launch { refreshDeveloperData() }
                }
            }.onFailure {
                error = it.localizedMessage ?: "Could not create API key."
            }

            isCreating = false
            isRotating = false
        }
    }

    fun revokeKey(keyId: String) {
        scope.launch {
            val authToken = currentAuthToken()
            if (authToken.isNullOrBlank()) {
                error = "Please sign in again."
                return@launch
            }

            isRevoking = true
            error = null
            message = null

            repo.revokeDeveloperKey(authToken, keyId)
                .onSuccess {
                    if (revealedKey?.id == keyId) revealedKey = null
                    message = "API key revoked."
                    refreshDeveloperData()
                }
                .onFailure {
                    error = it.localizedMessage ?: "Could not revoke API key."
                }

            isRevoking = false
        }
    }

    LaunchedEffect(userPlanState.isLoggedIn, userPlanState.plan) {
        if (userPlanState.isLoggedIn && userPlanState.plan == Plan.DEVELOPER) {
            isLoading = true
            refreshDeveloperData()
        } else {
            isLoading = false
        }
    }

    val latestActiveKey = keys.firstOrNull { it.isActive }
    val featuredKey = revealedKey ?: latestActiveKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Developer API", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary)
                Text("Manage keys, usage & webhooks", fontSize = 13.sp, color = TextMuted)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!userPlanState.isLoggedIn) {
            DeveloperGateCard(
                title = "Sign in required",
                body = "Sign in with your premium account to access developer usage and API keys.",
                action = "Sign In",
                onAction = { navController.navigate(Screen.Login.route) }
            )
            return@Column
        }

        if (userPlanState.plan != Plan.DEVELOPER) {
            DeveloperGateCard(
                title = "Developer plan required",
                body = "Your current plan is ${userPlanState.plan.label}. Upgrade to Developer to create and manage API keys.",
                action = "View Plans",
                onAction = { navController.navigate(Screen.Pricing.route) }
            )
            return@Column
        }

        PlanBadge("developer")

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
            return@Column
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message ?: "",
                color = Success,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Success.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error ?: "",
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageCard("INBOX CREATES", usage.inboxesCreated.toString(), "/ ${usage.inboxesLimit.toString().replace(",", "")} mo", Modifier.weight(1f))
            UsageCard("API READS", usage.readsUsed.toString(), "/ ${usage.readsLimit.toString().replace(",", "")} mo", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageCard("ACTIVE KEYS", usage.apiKeysActive.toString(), "/ ${usage.apiKeysLimit} max", Modifier.weight(1f))
            UsageCard("RETENTION", "30", "days", Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Text("CREATE KEY", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = keyName,
                onValueChange = { keyName = it.take(50) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Android Key", color = TextDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold.copy(alpha = 0.5f),
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = AccentGold,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldButton(
                    text = if (isCreating) "Creating..." else "Create Key",
                    onClick = { createKey() },
                    modifier = Modifier.weight(1f),
                    isLoading = isCreating,
                    icon = Icons.Outlined.VpnKey
                )
                GhostButton(
                    text = if (isRotating) "Rotating..." else "Rotate Latest",
                    onClick = { createKey(latestActiveKey?.id) },
                    modifier = Modifier.weight(1f),
                    enabled = latestActiveKey != null && !isRotating
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("API KEYS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (featuredKey == null) {
                Text(
                    "No API keys yet. Create your first key above to get started.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(featuredKey.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                    Text(
                        featuredKey.plainKey ?: featuredKey.keyPrefix,
                        fontSize = 13.sp,
                        color = ProPurple,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Created ${dateFormat.format(Date(featuredKey.createdAt * 1000))}",
                        fontSize = 12.sp,
                        color = TextDim
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    GhostButton(
                        text = "Copy Key",
                        onClick = {
                            copyToClipboard(
                                "API Key",
                                featuredKey.plainKey ?: featuredKey.keyPrefix
                            )
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.ContentCopy
                    )
                    GhostButton(
                        text = if (isRevoking) "Revoking..." else "Revoke",
                        onClick = { revokeKey(featuredKey.id) },
                        modifier = Modifier.weight(1f),
                        enabled = featuredKey.isActive && !isRevoking,
                        color = Danger,
                        icon = Icons.Outlined.Delete
                    )
                }
            }
        }

        if (keys.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("ALL KEYS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            keys.forEach { key ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = GlassBgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(key.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(key.keyPrefix, fontSize = 12.sp, color = TextMuted)
                            Text(
                                "Created ${dateFormat.format(Date(key.createdAt * 1000))}",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                        if (key.isActive) {
                            TextButton(onClick = { revokeKey(key.id) }) {
                                Text("Revoke", color = Danger)
                            }
                        } else {
                            Text("Revoked", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("QUICK START", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Create an inbox via API", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x0AFFFFFF)
            ) {
                Text(
                    "curl -X POST https://modih.in/api/inbox \\\n  -H 'X-API-Key: YOUR_KEY' \\\n  -H 'Content-Type: application/json'",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = ProPurple,
                    fontFamily = Inter,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("ENDPOINTS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        EndpointRow("POST", "/api/inbox", "Create inbox")
        EndpointRow("GET", "/api/inbox/{id}/messages", "List messages")
        EndpointRow("GET", "/api/inbox/{id}/messages/{mid}", "Get message")
        EndpointRow("DELETE", "/api/inbox/{id}", "Delete inbox")
        EndpointRow("DELETE", "/api/inbox/{id}/messages/{mid}", "Delete message")

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DeveloperGateCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(body, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
        Spacer(Modifier.height(16.dp))
        GoldButton(text = action, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun UsageCard(label: String, value: String, sublabel: String, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        Text(value, fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, color = AccentGold, lineHeight = 36.sp)
        Text(sublabel, fontSize = 12.sp, color = TextDim)
    }
}

@Composable
private fun EndpointRow(method: String, path: String, desc: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = GlassBgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val methodColor = when (method) {
                "POST" -> Success
                "GET" -> ProPurple
                "DELETE" -> Danger
                else -> TextMuted
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = methodColor.copy(alpha = 0.15f)
            ) {
                Text(
                    method,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = methodColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(path, fontSize = 12.sp, color = TextPrimary, fontFamily = Inter, fontWeight = FontWeight.Medium)
                Text(desc, fontSize = 11.sp, color = TextDim)
            }
        }
    }
}
