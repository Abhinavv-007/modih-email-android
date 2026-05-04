package com.modih.mail.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.modih.mail.R
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSentState by remember { mutableStateOf(false) }
    var sentEmail by remember { mutableStateOf("") }
    var passwordStrength by remember { mutableIntStateOf(0) }

    fun calcStrength(pw: String): Int {
        var score = 0
        if (pw.length >= 8) score++
        if (pw.length >= 12) score++
        if (pw.any { it.isUpperCase() }) score++
        if (pw.any { it.isDigit() }) score++
        if (pw.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(0, 5)
    }

    val strengthLabel = when (passwordStrength) {
        1 -> "Very weak"
        2 -> "Weak"
        3 -> "Fair"
        4 -> "Strong"
        5 -> "Very strong"
        else -> ""
    }
    val strengthColor = when (passwordStrength) {
        1 -> Danger
        2 -> Color(0xFFFB923C)
        3 -> Warning
        4 -> Success
        5 -> SuccessDim
        else -> Color.Transparent
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                try {
                    val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        .getResult(ApiException::class.java)
                    val cred = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(cred).await()
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                } catch (e: Exception) {
                    error = "Google sign-up failed: ${e.localizedMessage}"
                }
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            error = "Google sign-up could not be completed."
        }
    }

    fun signUpEmail() {
        if (email.isBlank() || password.length < 8) {
            error = if (password.length < 8) "Password must be at least 8 characters." else "Please fill in all fields."
            return
        }
        isLoading = true; error = null
        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.sendEmailVerification()?.await()
                auth.signOut()
                sentEmail = email
                showSentState = true
                isLoading = false
            } catch (e: Exception) {
                error = when {
                    "email-already-in-use" in (e.message ?: "") -> "An account with that email already exists. Try signing in."
                    "weak-password" in (e.message ?: "") -> "Password should be at least 8 characters."
                    else -> e.localizedMessage ?: "Something went wrong."
                }
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(targetState = showSentState, label = "signup_state") { sent ->
                if (!sent) {
                    // Form state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Join Modih",
                            fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic, fontSize = 32.sp, color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Create your premium account in seconds", style = MaterialTheme.typography.bodyMedium, color = TextMuted)

                        Spacer(Modifier.height(28.dp))

                        OAuthButton("Continue with Google") {
                            val clientId = context.getString(R.string.default_web_client_id)
                            if (clientId.isBlank() || clientId.contains("placeholder")) {
                                error = "Google sign-in is not configured yet. Add a valid Firebase web client ID first."
                            } else {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(clientId)
                                    .requestEmail()
                                    .build()
                                googleLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                            Text("  or  ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                        }
                        Spacer(Modifier.height(20.dp))

                        AuthTextField(email, { email = it }, "Email Address", "you@example.com",
                            KeyboardType.Email, ImeAction.Next, { focusManager.moveFocus(FocusDirection.Down) })
                        Spacer(Modifier.height(12.dp))
                        AuthTextField(password, {
                            password = it; passwordStrength = calcStrength(it)
                        }, "Password", "Min. 8 characters", KeyboardType.Password, ImeAction.Done,
                            { signUpEmail() }, isPassword = true, passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible })

                        // Strength bar
                        if (password.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { passwordStrength / 5f },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = strengthColor,
                                trackColor = GlassBgSubtle,
                            )
                            Text(strengthLabel, fontSize = 12.sp, color = strengthColor, modifier = Modifier.padding(top = 4.dp))
                        }

                        AnimatedVisibility(visible = error != null) {
                            Text(error ?: "", color = Danger, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(12.dp))
                        }

                        Spacer(Modifier.height(20.dp))
                        GoldButton("Create Account", { signUpEmail() }, Modifier.fillMaxWidth(), !isLoading, isLoading)
                        Spacer(Modifier.height(20.dp))

                        Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                            Text("Already have an account? ", color = TextMuted, fontSize = 14.sp)
                            Text("Sign in", color = AccentGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { navController.navigate(Screen.Login.route) })
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("By signing up you agree to our Terms of Service and Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall, color = TextDim, textAlign = TextAlign.Center)
                    }
                } else {
                    // Sent state
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                        Text("📬", fontSize = 56.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Check your inbox", fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic, fontSize = 28.sp, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("We sent a verification link to", color = TextMuted, fontSize = 15.sp)
                        Text(sentEmail, color = AccentGoldLight, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Check spam/junk folder if you don't see it", color = Danger.copy(alpha = 0.7f), fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Click the link to verify — then sign in.", color = TextMuted, fontSize = 14.sp)
                        Spacer(Modifier.height(32.dp))
                        GhostButton("Back to Sign In", { navController.navigate(Screen.Login.route) { popUpTo(0) } }, Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
