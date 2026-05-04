package com.modih.mail.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.R
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*

@Composable
fun HomeScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val auth = remember { FirebaseAuth.getInstance() }
    val isLoggedIn = auth.currentUser != null

    Box(modifier = Modifier.fillMaxSize()) {
        // v1.3: single static backdrop — was a 12 MB looping mp4 +
        // ExoPlayer + 3 stacked gradient overlays. Now it's the existing
        // home_bg_poster.png with one soft top-to-bottom darkener for
        // text legibility. Drops ~14 MB from the APK and removes the
        // launch hitch from spinning up the video decoder.
        BackgroundBackdrop()

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassPill(text = "MODIH MAIL", icon = "✦")
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp)),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = if (isLoggedIn) "Settings" else "Sign In",
                        color = TextPrimary,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable {
                                navController.navigate(
                                    if (isLoggedIn) Screen.Settings.route else Screen.Login.route
                                ) {
                                    launchSingleTop = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // NEW pill
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0x14FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0x1A34D399),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatusDot(color = Success, size = 6.dp)
                            Text("New", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Success, fontFamily = Inter)
                        }
                    }
                    Text(
                        "Instant Disposable Email Arrives Now",
                        fontSize = 12.sp, color = TextSecondary, fontFamily = Inter, fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Hero Title
            Text(
                "Grab Your",
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontSize = 46.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )
            Text(
                "@modih.in",
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 46.sp,
                lineHeight = 48.sp,
                textAlign = TextAlign.Center,
                color = Color.Transparent,
                style = LocalTextStyle.current.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentGold, AccentGoldDim)
                    )
                )
            )
            Text(
                "Email Today",
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontSize = 46.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )

            Spacer(Modifier.height(20.dp))

            // Subtitle
            Text(
                "Discover privacy in ways once unimaginable. Our disposable inboxes and breakthrough encryption bring total anonymity within reach, secure and extraordinary.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // CTA Buttons
            GoldButton(
                text = "Start Your Inbox",
                onClick = {
                    navController.navigate(Screen.Inbox.route) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Filled.ArrowForward
            )

            Spacer(Modifier.height(12.dp))

            GhostButton(
                text = "View Features",
                onClick = {
                    navController.navigate(Screen.Pricing.route) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.PlayArrow
            )

            if (!isLoggedIn) {
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sign in",
                        color = AccentGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.Login.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Stats bubbles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBubble(value = "∞", label = "Free Emails")
                StatBubble(value = "3h", label = "Auto-Expire")
                StatBubble(value = "0", label = "Data Stored")
            }

            Spacer(Modifier.height(40.dp))

            // Features section
            FeaturesSection()

            Spacer(Modifier.height(40.dp))

            // Footer
            Text(
                "A project by Abhinav",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
            Text(
                "Privacy first disposable email. Your inbox, your control.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatBubble(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = CormorantGaramond,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = AccentGold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun BackgroundBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home_bg_poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Single soft darkening pass over the poster so the hero copy stays
        // legible. One full-screen draw per frame instead of three.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BgPrimary.copy(alpha = 0.10f),
                            BgPrimary.copy(alpha = 0.45f),
                            BgCard.copy(alpha = 0.78f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun FeaturesSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPill(text = "Why Modih Mail", icon = "✦", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))

        Text(
            "Privacy, redefined.",
            fontFamily = CormorantGaramond,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        FeatureCard(icon = "🔒", title = "Zero Knowledge", desc = "No signup, no personal data. Your inbox is private and yours alone for 3 hours, then vanishes.")
        FeatureCard(icon = "⚡", title = "Instant Setup", desc = "One click to generate a random address. Upgrade to Pro to choose your own custom prefix.")
        FeatureCard(icon = "📧", title = "Real Inbox", desc = "Receive real emails, read HTML content, extract OTPs, and manage messages beautifully.")
        FeatureCard(icon = "⏰", title = "3h Auto-Destruct", desc = "Free inboxes self-destruct after 3 hours. Upgrade to Pro for 7-day retention.")
        FeatureCard(icon = "✅", title = "OTP Detection", desc = "Automatically highlights verification codes and OTPs from incoming emails for easy copying.")
        FeatureCard(icon = "🛡️", title = "Safe Rendering", desc = "Email HTML is sanitized. Scripts, iframes, and tracking pixels are all stripped before display.")
    }
}

@Composable
private fun FeatureCard(icon: String, title: String, desc: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            desc,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            lineHeight = 20.sp
        )
    }
}

// v1.3: VideoBackground composable removed along with the 12 MB
// res/raw/mobile_bg_loop.mp4 and the Media3 / ExoPlayer dependencies.
// The static BackgroundBackdrop above is now the only Home backdrop.
