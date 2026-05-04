package com.modih.mail.ui.screens.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.modih.mail.data.model.MailMessage
import com.modih.mail.ui.components.*
import com.modih.mail.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageDetailScreen(navController: NavController, messageId: String) {
    // In a real app, this would fetch from ViewModel/repo.
    // For now, we show a placeholder that works with passed data.
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd yyyy 'at' HH:mm", Locale.getDefault()) }

    // Placeholder message — real implementation would use shared ViewModel
    var message by remember {
        mutableStateOf(
            MailMessage(
                id = messageId,
                inboxId = "",
                from = "Loading...",
                subject = "Loading...",
                bodyHtml = "<p style='color:#888;font-family:sans-serif;'>Loading message content...</p>",
                bodyText = "Loading...",
                receivedAt = System.currentTimeMillis() / 1000
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgPrimary.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Text(
                    "Back to inbox",
                    fontSize = 14.sp, color = TextMuted, fontFamily = Inter,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    // Delete message
                    navController.popBackStack()
                }) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Subject + meta
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    message.subject,
                    fontFamily = Inter, fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp, color = TextPrimary, lineHeight = 26.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "From: ${message.from}",
                        fontSize = 13.sp, color = TextMuted, fontFamily = Inter,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        dateFormat.format(Date(message.receivedAt * 1000)),
                        fontSize = 12.sp, color = TextDim
                    )
                }

                // OTP Detection
                if (!message.otp.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Success.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("OTP Detected:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Success)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    message.otp!!,
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                    color = Success, letterSpacing = 4.sp
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("OTP", message.otp))
                                Toast.makeText(context, "OTP copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, "Copy OTP", tint = Success)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Email body — rendered in WebView
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val htmlContent = remember(message.bodyHtml) {
                    """
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                background: transparent;
                                color: #C8B89A;
                                font-family: -apple-system, sans-serif;
                                font-size: 15px;
                                line-height: 1.6;
                                padding: 0;
                                margin: 0;
                                word-wrap: break-word;
                            }
                            a { color: #D4A76A; }
                            img { max-width: 100%; height: auto; border-radius: 8px; }
                            pre, code {
                                background: rgba(255,255,255,0.05);
                                border-radius: 6px;
                                padding: 2px 6px;
                                font-size: 13px;
                                overflow-x: auto;
                            }
                        </style>
                    </head>
                    <body>${message.bodyHtml}</body>
                    </html>
                    """.trimIndent()
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Delete button
            GhostButton(
                text = "Delete Message",
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                color = Danger,
                icon = Icons.Outlined.Delete
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
