package com.modih.mail.ui.screens.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
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
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.MessageStore
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.repository.MailRepository
import com.modih.mail.ui.components.*
import com.modih.mail.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageDetailScreen(navController: NavController, messageId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val repo = remember { MailRepository() }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd yyyy 'at' HH:mm", Locale.getDefault()) }
    val messages by MessageStore.messages.collectAsState()
    val message = remember(messages, messageId) { MessageStore.get(messageId) }
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Off by default per the handoff doc — "Don't auto-load remote images.
    // Email pixels are tracking. Show a 'Load images' button."
    var loadImages by remember { mutableStateOf(false) }

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
                IconButton(
                    enabled = !isDeleting && message != null,
                    onClick = {
                        val msg = message ?: return@IconButton
                        scope.launch {
                            val savedInbox = prefs.currentInboxOnce()
                            val ownerToken = savedInbox?.ownerToken.orEmpty()
                            val inboxId = savedInbox?.id.orEmpty().ifBlank { msg.inboxId }
                            if (ownerToken.isBlank() || inboxId.isBlank()) {
                                error = "Inbox session missing — open the inbox again."
                                return@launch
                            }
                            isDeleting = true
                            val authToken = auth.currentUser?.getIdToken(false)?.await()?.token
                            repo.deleteMessage(inboxId, msg.id, ownerToken, authToken)
                                .onSuccess {
                                    MessageStore.remove(msg.id)
                                    navController.popBackStack()
                                }
                                .onFailure { error = it.message ?: "Delete failed" }
                            isDeleting = false
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        if (message == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✉️", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Message no longer available",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "It may have been deleted or your session timed out. Pull-to-refresh the inbox to reload.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(24.dp))
                GoldButton(
                    text = "Back to inbox",
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Danger.copy(alpha = 0.12f)
            ) {
                Text(
                    error.orEmpty(),
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

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
                        "From: ${message.displaySender}",
                        fontSize = 13.sp, color = TextMuted, fontFamily = Inter,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        dateFormat.format(Date(message.receivedAt * 1000)),
                        fontSize = 12.sp, color = TextDim
                    )
                }

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
                                    message.otp,
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

            // Email body — rendered in a sandboxed WebView (JS off, no network).
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val htmlContent = remember(message.bodyHtml, message.bodyText) {
                    val rawBody = message.bodyHtml.takeIf { it.isNotBlank() }
                        ?: "<pre style='font-family:monospace;white-space:pre-wrap;'>" +
                            android.text.Html.escapeHtml(message.bodyText.ifBlank { "(empty body)" }) +
                            "</pre>"
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
                    <body>$rawBody</body>
                    </html>
                    """.trimIndent()
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            // Hardened per the security handoff:
                            //  - JS disabled to defeat any sanitiser-bypass
                            //    payload that the server may have missed.
                            //  - blockNetworkLoads + loadsImagesAutomatically
                            //    off so tracking pixels can't beacon out.
                            //  - mixedContent NEVER_ALLOW so any sneaky http://
                            //    URL embedded in the body is dropped.
                            //  - DOM storage off so a stale message can't
                            //    leave traces between opens.
                            //  - File / content URIs blocked so a body can't
                            //    reach into the device's filesystem.
                            settings.javaScriptEnabled = false
                            settings.domStorageEnabled = false
                            settings.allowContentAccess = false
                            settings.allowFileAccess = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.blockNetworkLoads = !loadImages
                            settings.loadsImagesAutomatically = loadImages
                            webViewClient = WebViewClient()
                            // "about:blank" base URL means relative links in
                            // the body resolve to nothing rather than to the
                            // app's package — defence in depth.
                            loadDataWithBaseURL("about:blank", htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.settings.blockNetworkLoads = !loadImages
                        webView.settings.loadsImagesAutomatically = loadImages
                        webView.loadDataWithBaseURL("about:blank", htmlContent, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Off-by-default "Load images" toggle. Most marketing email is
            // tracking pixels; we let the user opt in per message.
            if (!loadImages) {
                GhostButton(
                    text = "Load images",
                    onClick = { loadImages = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Image
                )
                Spacer(Modifier.height(8.dp))
            }

            // Copy plain body button — matches the `Copy text` action on web.
            GhostButton(
                text = "Copy email body",
                onClick = {
                    val plain = message.bodyText.ifBlank { android.text.Html.fromHtml(message.bodyHtml, 0).toString() }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("email", plain))
                    Toast.makeText(context, "Body copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.ContentCopy
            )

            Spacer(Modifier.height(8.dp))

            // Delete button
            GhostButton(
                text = if (isDeleting) "Deleting…" else "Delete Message",
                onClick = {
                    scope.launch {
                        val savedInbox = prefs.currentInboxOnce()
                        val ownerToken = savedInbox?.ownerToken.orEmpty()
                        val inboxId = savedInbox?.id.orEmpty().ifBlank { message.inboxId }
                        if (ownerToken.isBlank() || inboxId.isBlank()) {
                            error = "Inbox session missing — open the inbox again."
                            return@launch
                        }
                        isDeleting = true
                        val authToken = auth.currentUser?.getIdToken(false)?.await()?.token
                        repo.deleteMessage(inboxId, message.id, ownerToken, authToken)
                            .onSuccess {
                                MessageStore.remove(message.id)
                                navController.popBackStack()
                            }
                            .onFailure { error = it.message ?: "Delete failed" }
                        isDeleting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = Danger,
                icon = Icons.Outlined.Delete,
                enabled = !isDeleting
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
