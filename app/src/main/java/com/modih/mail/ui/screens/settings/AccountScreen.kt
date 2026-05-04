package com.modih.mail.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.R
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AccountScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }
    val user = auth.currentUser
    val scope = rememberCoroutineScope()
    val userPlanState = rememberUserPlanState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var resetFeedback by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

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
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Text("Account", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary)
        }

        Spacer(Modifier.height(16.dp))

        // Profile card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(50),
                    color = AccentGoldSubtle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            (user?.email?.firstOrNull() ?: '?').uppercase(),
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentGold
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        user?.displayName ?: user?.email?.split("@")?.firstOrNull() ?: "User",
                        fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary
                    )
                    Text(user?.email ?: "", fontSize = 14.sp, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    if (userPlanState.isLoading && userPlanState.plan == com.modih.mail.data.model.Plan.FREE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentGold
                        )
                    } else {
                        PlanBadge(userPlanState.plan.name.lowercase())
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Account details
        Text("DETAILS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        DetailRow("Email", user?.email ?: "Not set")
        DetailRow("User ID", user?.uid?.take(12)?.plus("...") ?: "N/A")
        DetailRow("Email Verified", if (user?.isEmailVerified == true) "Yes" else "No")
        DetailRow("Provider", user?.providerData?.firstOrNull()?.providerId ?: "Unknown")
        DetailRow("Created", user?.metadata?.creationTimestamp?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: "Unknown")

        Spacer(Modifier.height(28.dp))

        // Actions
        Text("ACTIONS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Outlined.Star,
            title = "Upgrade Plan",
            subtitle = "Current plan: ${userPlanState.plan.label}",
            onClick = { navController.navigate(Screen.Pricing.route) }
        )

        SettingsItem(
            icon = Icons.Outlined.Lock,
            title = "Change Password",
            subtitle = if (isSendingReset) {
                "Sending reset email..."
            } else if (supportsPasswordReset(user)) {
                "Send password reset email"
            } else {
                "This account signs in with Google"
            },
            onClick = {
                scope.launch {
                    val email = userPlanState.email ?: user?.email
                    if (!supportsPasswordReset(user)) {
                        resetFeedback = "This account uses Google sign-in, so there is no password to reset."
                    } else if (email.isNullOrBlank()) {
                        resetFeedback = "No registered email address was found for this account."
                    } else {
                        isSendingReset = true
                        resetFeedback = null
                        try {
                            auth.sendPasswordResetEmail(email).await()
                            resetFeedback = "Reset mail successfully sent to your registered email."
                        } catch (error: Exception) {
                            resetFeedback = error.localizedMessage ?: "Could not send reset email."
                        } finally {
                            isSendingReset = false
                        }
                    }
                }
            }
        )

        val resetWasSuccess = resetFeedback?.contains("success", ignoreCase = true) == true

        if (!resetFeedback.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = resetFeedback ?: "",
                color = if (resetWasSuccess) Success else Danger,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (resetWasSuccess) Success.copy(alpha = 0.1f) else Danger.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        // Danger zone
        Text("DANGER ZONE", style = MaterialTheme.typography.labelMedium, color = Danger, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Danger.copy(alpha = 0.06f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Delete Account", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Danger)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Permanently delete your account and all associated data. This action cannot be undone.",
                    fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                if (!showDeleteConfirm) {
                    GhostButton(
                        text = "Delete My Account",
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        color = Danger
                    )
                } else {
                    Text(
                        "Are you sure? This is irreversible.",
                        color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        GhostButton("Cancel", { showDeleteConfirm = false }, Modifier.weight(1f))
                        Button(
                            onClick = {
                                scope.launch {
                                    val currentUser = auth.currentUser
                                    if (currentUser == null) {
                                        deleteError = "Please sign in again before deleting your account."
                                        return@launch
                                    }

                                    try {
                                        isDeleting = true
                                        deleteError = null
                                        val authToken = currentUser.getIdToken(false).await()?.token
                                        try {
                                            currentUser.delete().await()
                                        } catch (error: Exception) {
                                            if (needsRecentLogin(error) && supportsGoogleReauth(currentUser)) {
                                                reauthenticateGoogleUser(context, auth)
                                                currentUser.delete().await()
                                            } else {
                                                throw error
                                            }
                                        }
                                        if (!authToken.isNullOrBlank()) {
                                            repo.deleteAccountData(authToken)
                                        }
                                        prefs.clearUser()
                                        auth.signOut()
                                        navController.navigate(Screen.Home.route) { popUpTo(0) }
                                    } catch (error: Exception) {
                                        deleteError = when {
                                            error.message?.contains("recent", ignoreCase = true) == true ->
                                                "For security, sign in again before deleting your account."
                                            else -> error.localizedMessage ?: "Could not delete your account."
                                        }
                                    } finally {
                                        isDeleting = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Danger),
                            enabled = !isDeleting
                        ) {
                            Text(
                                if (isDeleting) "Deleting..." else "Confirm Delete",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (!deleteError.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = deleteError ?: "",
                color = Danger,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

private fun supportsPasswordReset(user: com.google.firebase.auth.FirebaseUser?): Boolean =
    user?.providerData?.any { it.providerId == "password" } == true

private fun supportsGoogleReauth(user: com.google.firebase.auth.FirebaseUser?): Boolean =
    user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

private fun needsRecentLogin(error: Exception): Boolean =
    error.message?.contains("recent", ignoreCase = true) == true

private suspend fun reauthenticateGoogleUser(context: Context, auth: FirebaseAuth) {
    val clientId = context.getString(R.string.default_web_client_id)
    val googleClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
    )
    val account = googleClient.silentSignIn().await()
    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
    auth.currentUser?.reauthenticate(credential)?.await()
}

@Composable
private fun DetailRow(label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = GlassBgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = TextMuted, fontFamily = Inter)
            Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = Inter)
        }
    }
}
