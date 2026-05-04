package com.modih.mail.ui.screens.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.ui.components.GlassCard
import com.modih.mail.ui.theme.*

@Composable
fun TermsScreen(navController: NavController) {
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
            Text("Back", fontSize = 14.sp, color = TextMuted)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Terms of Service",
                fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic, fontSize = 32.sp, color = AccentGold
            )
            Spacer(Modifier.height(4.dp))
            Text("Last Updated: March 2026", fontSize = 12.sp, color = TextDim, letterSpacing = 1.sp)

            Spacer(Modifier.height(24.dp))

            LegalText("Welcome to Modih Mail. By accessing or using our disposable email service, you agree to comply with and be bound by the following Terms of Service.")

            LegalHeader("1. Use of the Service")
            LegalText("Modih Mail provides temporary, disposable email addresses designed to protect your primary personal email from spam. You agree to use Modih Mail only for lawful purposes.")

            LegalHeader("2. Prohibited Conduct")
            LegalText("We actively monitor our systems to prevent abuse. You strictly agree NOT to use Modih Mail for:")
            LegalBullet("Sending spam, unsolicited bulk email, or promotional materials (Modih Mail is a receive-only service).")
            LegalBullet("Registering for services with the intent to commit fraud, evade bans, or violate the terms of third-party platforms.")
            LegalBullet("Phishing, malware distribution, or any form of cyber-attack.")
            LegalBullet("Receiving or transmitting illegal materials or engaging in illegal activities.")

            LegalHeader("3. Limitation of Liability")
            LegalText("Modih Mail is provided on an \"AS IS\" and \"AS AVAILABLE\" basis. We make no guarantees regarding the reliability, accuracy, or uninterrupted delivery of incoming emails.")

            LegalHeader("4. Account Termination")
            LegalText("For Free, Pro, and Developer users, we reserve the right to modify, suspend, or terminate your access to the service at any time, for any reason — especially in cases of abuse.")

            LegalHeader("5. Premium Plans (Pro & Developer)")
            LegalText("Purchases of Pro or Developer plans grant you access to extended retention times and custom prefixes. These premium services are subject to fair use policies.")

            LegalHeader("6. Indemnification")
            LegalText("You agree to indemnify, defend, and hold harmless Modih Mail and its respective officers, agents, partners, and employees from any claim or demand made by any third party.")

            LegalHeader("7. Modifications to Terms")
            LegalText("We reserve the right to change these Terms of Service at any time. Your continued use of the service following any changes indicates your acceptance.")

            LegalHeader("Contact Us")
            LegalText("For legal inquiries, abuse reports, or questions about these terms, please contact legal@modih.in.")
        }

        Spacer(Modifier.height(40.dp))
    }
}
