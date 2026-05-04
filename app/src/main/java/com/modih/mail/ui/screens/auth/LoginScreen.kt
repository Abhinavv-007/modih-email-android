package com.modih.mail.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showVerifyWarning by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential).await()
                    navController.navigate(Screen.Home.route) { popUpTo(0) }
                } catch (e: Exception) {
                    error = "Google sign-in failed: ${e.localizedMessage}"
                }
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            error = "Google sign-in could not be completed."
        }
    }

    fun signInEmail() {
        if (email.isBlank() || password.isBlank()) {
            error = "Please fill in all fields."
            return
        }
        isLoading = true
        error = null
        scope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                if (result.user?.isEmailVerified == false) {
                    showVerifyWarning = true
                    auth.signOut()
                    isLoading = false
                    return@launch
                }
                navController.navigate(Screen.Home.route) { popUpTo(0) }
            } catch (e: Exception) {
                error = friendlyError(e)
                isLoading = false
            }
        }
    }

    fun signInGoogle() {
        val clientId = context.getString(R.string.default_web_client_id)
        if (clientId.isBlank() || clientId.contains("placeholder")) {
            error = "Google sign-in is not configured yet. Add a valid Firebase web client ID first."
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun forgotPassword() {
        if (email.isBlank()) {
            error = "Please enter your email address first."
            return
        }
        scope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                error = null
                // Show success inline
            } catch (e: Exception) {
                error = "Could not send reset email: ${e.localizedMessage}"
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
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Header
            Text("🔒", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Welcome back",
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sign in to manage your premium inbox",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(Modifier.height(28.dp))

            // OAuth Buttons
            OAuthButton(
                text = "Continue with Google",
                onClick = { signInGoogle() }
            )

            Spacer(Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                Text(
                    "  or  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            }

            Spacer(Modifier.height(20.dp))

            // Email field
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )

            Spacer(Modifier.height(12.dp))

            // Password field
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Your password",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { signInEmail() },
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible }
            )

            // Forgot password
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { forgotPassword() }) {
                    Text("Forgot password?", fontSize = 12.sp, color = TextMuted)
                }
            }

            // Error
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Danger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }

            // Verify warning
            AnimatedVisibility(visible = showVerifyWarning) {
                Text(
                    text = "Please verify your email first. Check your inbox for the verification link.",
                    color = Warning,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Warning.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Sign In button
            GoldButton(
                text = "Sign In",
                onClick = { signInEmail() },
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                enabled = !isLoading
            )

            Spacer(Modifier.height(24.dp))

            // Sign up link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = TextMuted, fontSize = 14.sp)
                Text(
                    "Sign up free",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.SignUp.route) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Free tier requires no account. Sign in only needed for Pro & Developer plans.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Legal links
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    "Privacy Policy",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable { navController.navigate(Screen.Privacy.route) }
                )
                Text(
                    "Terms of Service",
                    color = TextMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable { navController.navigate(Screen.Terms.route) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun OAuthButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderHover),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GlassBgSubtle,
            contentColor = TextPrimary
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(28.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.96f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                GoogleMark()
            }
        }
        Text(text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun GoogleMark(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(16.dp)
    ) {
        val strokeWidth = size.minDimension * 0.18f
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -42f,
            sweepAngle = 88f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 46f,
            sweepAngle = 74f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 120f,
            sweepAngle = 102f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 214f,
            sweepAngle = 104f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = center.copy(x = center.x + strokeWidth * 0.2f, y = center.y),
            end = center.copy(x = size.width - strokeWidth * 0.5f, y = center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, color = TextDim)
            },
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() }
            ),
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            "Toggle password",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold.copy(alpha = 0.5f),
                unfocusedBorderColor = GlassBorder,
                cursorColor = AccentGold,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = GlassBgSubtle,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

private fun friendlyError(e: Exception): String {
    val msg = e.message ?: return "Something went wrong."
    return when {
        "no user record" in msg.lowercase() || "user-not-found" in msg -> "No account found with that email."
        "wrong-password" in msg || "invalid-credential" in msg -> "Invalid email or password."
        "too-many-requests" in msg -> "Too many attempts. Please wait a moment."
        "user-disabled" in msg -> "This account has been disabled."
        "network" in msg.lowercase() -> "Network error — check your connection."
        else -> "Something went wrong. Please try again."
    }
}
