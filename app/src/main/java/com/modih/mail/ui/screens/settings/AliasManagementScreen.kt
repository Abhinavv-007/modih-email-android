package com.modih.mail.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.data.model.Plan
import com.modih.mail.data.model.ReservedAlias
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*

@Composable
fun AliasManagementScreen(navController: NavController) {
    val userPlanState = rememberUserPlanState()

    // Plan determines max aliases: Free=1, Pro=3, Developer=3
    val userPlan = userPlanState.plan
    val maxAliases = userPlan.maxReservedAliases

    var aliases by remember { mutableStateOf<List<ReservedAlias>>(emptyList()) }
    var newPrefix by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Header
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
                Text(
                    "Reserved Aliases",
                    fontFamily = Inter, fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp, color = TextPrimary
                )
                Text(
                    "${aliases.size} of $maxAliases used",
                    fontSize = 13.sp, color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Plan info card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bookmark, null, tint = AccentGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Reserved Aliases",
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary
                    )
                    Text(
                        when (userPlan) {
                            Plan.FREE -> "Free plan: 1 reserved alias. Upgrade to Pro for 3."
                            Plan.PRO -> "Pro plan: 3 reserved aliases included."
                            Plan.DEVELOPER -> "Developer plan: 3 reserved aliases included."
                        },
                        fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Usage bar
            LinearProgressIndicator(
                progress = { aliases.size.toFloat() / maxAliases.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (aliases.size >= maxAliases) Warning else AccentGold,
                trackColor = GlassBgSubtle,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${aliases.size} / $maxAliases aliases used",
                fontSize = 12.sp, color = TextDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }

        Spacer(Modifier.height(24.dp))

        // Aliases list
        if (aliases.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📧", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No reserved aliases yet",
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Reserve a custom email alias that persists across sessions. Your alias won't expire like regular inboxes.",
                        fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center, lineHeight = 18.sp
                    )
                }
            }
        } else {
            aliases.forEach { alias ->
                AliasCard(
                    alias = alias,
                    onDelete = { aliases = aliases.filter { a -> a.id != alias.id } }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Add alias form
        AnimatedVisibility(visible = showAddForm) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Reserve New Alias", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPrefix,
                    onValueChange = { newPrefix = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '-' }.take(30) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("your-alias", color = TextDim) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text("@modih.in", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold.copy(alpha = 0.5f),
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = AccentGold,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                AnimatedVisibility(visible = error != null) {
                    Text(
                        error ?: "", color = Danger, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    GhostButton("Cancel", { showAddForm = false; newPrefix = ""; error = null }, Modifier.weight(1f))
                    GoldButton(
                        text = "Reserve",
                        onClick = {
                            if (newPrefix.isBlank()) {
                                error = "Please enter an alias prefix."
                                return@GoldButton
                            }
                            if (aliases.size >= maxAliases) {
                                error = "You've reached your limit. ${if (userPlan == Plan.FREE) "Upgrade to Pro for 3 aliases." else "Maximum aliases reached."}"
                                return@GoldButton
                            }
                            if (aliases.any { it.prefix == newPrefix }) {
                                error = "This alias is already reserved."
                                return@GoldButton
                            }
                            aliases = aliases + ReservedAlias(
                                id = java.util.UUID.randomUUID().toString(),
                                prefix = newPrefix,
                                email = "$newPrefix@modih.in",
                                createdAt = System.currentTimeMillis() / 1000
                            )
                            newPrefix = ""
                            showAddForm = false
                            error = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Add button
        if (!showAddForm) {
            GoldButton(
                text = if (aliases.size >= maxAliases) "Upgrade for More" else "Reserve New Alias",
                onClick = {
                    if (aliases.size >= maxAliases && userPlan == Plan.FREE) {
                        navController.navigate(Screen.Pricing.route)
                    } else {
                        showAddForm = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Add
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AliasCard(alias: ReservedAlias, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassBgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = Success, size = 8.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alias.email,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentGold, fontFamily = Inter
                )
                Text(
                    "Reserved • Active",
                    fontSize = 12.sp, color = Success.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = Danger.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
