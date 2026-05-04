package com.modih.mail.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.model.AdminActivity
import com.modih.mail.data.model.AdminSnapshot
import com.modih.mail.data.model.AdminStats
import com.modih.mail.data.model.AdminUser
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.GlassCard
import com.modih.mail.ui.components.GlassPill
import com.modih.mail.ui.components.GoldButton
import com.modih.mail.ui.components.PlanBadge
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val rangeOptions = listOf("1d" to "Today", "7d" to "7 days", "30d" to "30 days", "lifetime" to "All time")
private val filterOptions = listOf("all" to "All", "free" to "Free", "pro" to "Pro", "developer" to "Developer")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf<AdminSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var range by remember { mutableStateOf("7d") }
    var filter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }

    // Patch state
    var pendingPatchUid by remember { mutableStateOf<String?>(null) }

    suspend fun loadSnapshot() {
        val secret = prefs.adminSecretOnce()
        if (secret.isNullOrBlank()) {
            navController.navigate(Screen.AdminLogin.route) {
                popUpTo(Screen.AdminPanel.route) { inclusive = true }
            }
            return
        }
        isLoading = true
        repo.fetchAdminSnapshot(adminSecret = secret, filter = filter, emailQuery = query, rangeKey = range)
            .onSuccess {
                snapshot = it
                error = null
            }
            .onFailure { e ->
                if (e.message?.contains("auth", ignoreCase = true) == true) {
                    prefs.clearAdminSecret()
                    navController.navigate(Screen.AdminLogin.route) {
                        popUpTo(Screen.AdminPanel.route) { inclusive = true }
                    }
                } else {
                    error = e.message ?: "Could not load admin data"
                }
            }
        isLoading = false
    }

    LaunchedEffect(filter, range, query) {
        loadSnapshot()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Admin",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    snapshot?.let { "${it.stats.totalUsers} total users" } ?: "Loading…",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            IconButton(onClick = { scope.launch { loadSnapshot() } }) {
                Icon(Icons.Outlined.Refresh, "Refresh", tint = TextPrimary)
            }
            IconButton(onClick = {
                scope.launch {
                    prefs.clearAdminSecret()
                    navController.navigate(Screen.AdminLogin.route) {
                        popUpTo(Screen.AdminPanel.route) { inclusive = true }
                    }
                }
            }) {
                Icon(Icons.Outlined.Logout, "Sign out admin", tint = TextPrimary)
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // Filters
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by email", fontSize = 13.sp, color = TextDim) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGold
                )
            )

            Spacer(Modifier.height(10.dp))
            ChipRow(
                options = filterOptions,
                selected = filter,
                onSelect = { filter = it }
            )
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = rangeOptions,
                selected = range,
                onSelect = { range = it }
            )
        }

        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = Danger.copy(alpha = 0.10f)
            ) {
                Text(
                    error.orEmpty(),
                    color = Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (isLoading && snapshot == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
                Spacer(Modifier.height(8.dp))
                Text("Loading admin data…", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                snapshot?.let { snap ->
                    item { StatsCard(snap.stats, snap.analyticsSource) }
                    item {
                        Text(
                            "USERS",
                            fontSize = 11.sp,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(snap.users, key = { it.uid }) { user ->
                        UserCard(
                            user = user,
                            isPatching = pendingPatchUid == user.uid,
                            onPlanChange = { newPlan ->
                                pendingPatchUid = user.uid
                                scope.launch {
                                    val secret = prefs.adminSecretOnce().orEmpty()
                                    repo.adminUpdatePlan(secret, user.uid, newPlan, "lifetime")
                                        .onSuccess { loadSnapshot() }
                                        .onFailure { error = it.message ?: "Plan update failed" }
                                    pendingPatchUid = null
                                }
                            }
                        )
                    }
                    if (snap.activity.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "ACTIVITY",
                                fontSize = 11.sp,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(snap.activity, key = { it.actor + it.subject + it.createdAt }) { activity ->
                            ActivityRow(activity)
                        }
                    }
                } ?: item {
                    Text(
                        "No admin data available.",
                        color = TextMuted,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (key, label) ->
            val isSel = selected == key
            FilterChip(
                selected = isSel,
                onClick = { onSelect(key) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGoldSubtle,
                    selectedLabelColor = AccentGold,
                    containerColor = GlassBgSubtle,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun StatsCard(stats: AdminStats, source: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPill(text = "Stats · ${stats.rangeKey}", icon = "📊")
            Spacer(Modifier.weight(1f))
            Text(source, fontSize = 10.sp, color = TextDim)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(modifier = Modifier.weight(1f), label = "Users", value = stats.totalUsers.toString())
            StatTile(modifier = Modifier.weight(1f), label = "Pro", value = stats.proUsers.toString())
            StatTile(modifier = Modifier.weight(1f), label = "Dev", value = stats.developerUsers.toString())
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(modifier = Modifier.weight(1f), label = "Active inboxes", value = stats.activeInboxes.toString())
            StatTile(modifier = Modifier.weight(1f), label = "Inboxes", value = stats.totalInboxesCreated.toString())
            StatTile(modifier = Modifier.weight(1f), label = "Messages", value = stats.totalMessagesReceived.toString())
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = GlassBgSubtle
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(value, color = AccentGold, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)
            Text(label, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun UserCard(
    user: AdminUser,
    isPatching: Boolean,
    onPlanChange: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = AccentGoldSubtle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.email.ifBlank { user.uid.take(10) + "…" },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "${user.totalInboxes} inboxes · ${user.totalMessages} msgs",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Box {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                    onClick = { if (!isPatching) menuOpen = true },
                    color = AccentGoldSubtle,
                    enabled = !isPatching
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPatching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = AccentGold
                            )
                        } else {
                            PlanBadge(user.plan)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    listOf("free", "pro", "developer").forEach { p ->
                        DropdownMenuItem(
                            text = { Text("Set $p") },
                            onClick = {
                                menuOpen = false
                                onPlanChange(p)
                            }
                        )
                    }
                }
            }
        }
        if (user.planExpiresAt != null && user.planExpiresAt > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Expires ${formatTimestamp(user.planExpiresAt)}",
                fontSize = 10.sp,
                color = TextDim
            )
        }
    }
}

@Composable
private fun ActivityRow(activity: AdminActivity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = GlassBgSubtle
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = AccentGoldSubtle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        when (activity.type) {
                            "inbox" -> "📥"
                            "message" -> "✉️"
                            "api" -> "⚡"
                            "login" -> "🔐"
                            else -> "•"
                        },
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.actor.ifBlank { "system" },
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    activity.subject,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
            Text(
                formatTimestamp(activity.createdAt),
                color = TextDim,
                fontSize = 10.sp
            )
        }
    }
}

private val activityFormatter = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())

private fun formatTimestamp(value: Long): String {
    if (value <= 0) return "—"
    // Some endpoints return ms, others return s. Heuristic:
    val ms = if (value < 10_000_000_000L) value * 1000 else value
    return activityFormatter.format(Date(ms))
}
