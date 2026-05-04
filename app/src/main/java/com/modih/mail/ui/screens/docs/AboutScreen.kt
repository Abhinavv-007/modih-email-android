package com.modih.mail.ui.screens.docs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.ui.components.*
import com.modih.mail.ui.screens.settings.SettingsItem
import com.modih.mail.ui.theme.*
import com.modih.mail.util.Constants

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

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
            Text("About", fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary)
        }

        Spacer(Modifier.height(24.dp))

        // App info card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✉️", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Modih Mail",
                    fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic, fontSize = 32.sp, color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Privacy first disposable email",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted
                )
                Spacer(Modifier.height(12.dp))
                Text("Version 1.0.0", fontSize = 13.sp, color = TextDim)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your inbox, your control.",
                    fontSize = 14.sp, color = AccentGold,
                    fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // What is Modih Mail
        Text("ABOUT", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Modih Mail provides free, disposable @modih.in email addresses. Generate a random inbox instantly — no signup required. Receive real emails, detect OTPs automatically, and enjoy complete privacy. Inboxes auto-expire after 3 hours (7 days for Pro, 30 days for Developer).",
                fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp, fontFamily = Inter
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Key Features:",
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            LegalBullet("Zero knowledge — no personal data stored")
            LegalBullet("Instant random inbox generation")
            LegalBullet("Real-time email reception with auto-refresh")
            LegalBullet("Automatic OTP detection and easy copying")
            LegalBullet("Safe HTML rendering — scripts and trackers stripped")
            LegalBullet("Reserved aliases (1 free, 3 for Pro)")
            LegalBullet("Custom prefix for Pro and Developer plans")
        }

        Spacer(Modifier.height(24.dp))

        // Links
        Text("LINKS", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Outlined.Language,
            title = "modih.in",
            subtitle = "Visit the web app",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BASE_URL))) }
        )
        SettingsItem(
            icon = Icons.Outlined.Person,
            title = "Abhinav",
            subtitle = "Creator — abhnv.in",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.AUTHOR_URL))) }
        )
        SettingsItem(
            icon = Icons.Outlined.Person,
            title = "LinkedIn",
            subtitle = "Connect on LinkedIn",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINKEDIN_URL))) }
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "Made with care by Abhinav",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp, color = TextDim, fontStyle = FontStyle.Italic
        )

        Spacer(Modifier.height(40.dp))
    }
}
