package com.modih.mail.ui.screens.pricing

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.modih.mail.data.model.Plan
import com.modih.mail.ui.components.*
import com.modih.mail.ui.navigation.Screen
import com.modih.mail.ui.theme.*
import com.modih.mail.util.Constants

private data class PlanData(
    val name: String,
    val tagline: String,
    val prices: Map<String, Int>, // monthly, quarterly, yearly
    val features: List<Pair<String, Boolean>>,
    val isRecommended: Boolean = false,
    val ctaText: String,
    val ctaAction: String // "generate", "upgrade_pro", "upgrade_dev"
)

@Composable
fun PricingScreen(navController: NavController) {
    val context = LocalContext.current
    val userPlanState = rememberUserPlanState()
    var billingPeriod by remember { mutableStateOf("monthly") }
    val backToPrevious = {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                launchSingleTop = true
            }
        }
    }

    val plans = remember {
        listOf(
            PlanData(
                name = "Free", tagline = "Quick & anonymous",
                prices = mapOf("monthly" to 0, "quarterly" to 0, "yearly" to 0),
                features = listOf(
                    "Random inbox only" to true,
                    "3 inboxes per day" to true,
                    "1 active inbox" to true,
                    "3-hour retention" to true,
                    "OTP detection" to true,
                    "1 reserved alias" to true,
                    "Custom prefix" to false,
                    "Message export" to false,
                ),
                ctaText = "Get Started Free", ctaAction = "generate"
            ),
            PlanData(
                name = "Pro", tagline = "For power users",
                prices = mapOf("monthly" to 5, "quarterly" to 4, "yearly" to 2),
                features = listOf(
                    "Custom name@modih.in" to true,
                    "25 inboxes per day" to true,
                    "10 active inboxes" to true,
                    "7-day retention" to true,
                    "3 reserved aliases" to true,
                    "No captcha" to true,
                    "Export .txt / .eml" to true,
                    "Sender blocklist" to true,
                    "Sync across devices" to true,
                ),
                isRecommended = true,
                ctaText = "Upgrade to Pro", ctaAction = "upgrade_pro"
            ),
            PlanData(
                name = "Developer", tagline = "API & automation",
                prices = mapOf("monthly" to 30, "quarterly" to 25, "yearly" to 15),
                features = listOf(
                    "Everything in Pro" to true,
                    "30-day retention" to true,
                    "API keys + webhooks" to true,
                    "5k inbox creates/mo" to true,
                    "50k reads/mo" to true,
                    "Key rotate & revoke" to true,
                    "Usage dashboard" to true,
                    "30-day API logs" to true,
                ),
                ctaText = "Contact Sales", ctaAction = "upgrade_dev"
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = backToPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Plans & Pricing",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        }

        GlassPill(text = "Plans & Pricing", icon = "✦", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        Text(
            "Choose your plan.",
            fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic, fontSize = 32.sp, color = TextPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Simple, transparent pricing. Start free, upgrade when you need more power.",
            style = MaterialTheme.typography.bodyMedium, color = TextMuted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Billing toggle
        BillingToggle(
            selected = billingPeriod,
            onSelect = { billingPeriod = it }
        )

        Spacer(Modifier.height(24.dp))

        // Plan cards
        plans.forEach { plan ->
            PricingCard(
                plan = plan,
                billingPeriod = billingPeriod,
                currentPlan = userPlanState.plan,
                isLoggedIn = userPlanState.isLoggedIn,
                onClick = {
                    when (plan.name) {
                        "Free" -> navController.navigate(Screen.Inbox.route) {
                            launchSingleTop = true
                        }
                        else -> {
                            if (userPlanState.isLoggedIn) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("${Constants.BASE_URL}/#pricing"))
                                )
                            } else {
                                navController.navigate(Screen.Login.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BillingToggle(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("monthly", "Monthly", null),
        Triple("quarterly", "Quarterly", "-20%"),
        Triple("yearly", "Yearly", "-60%")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x0AFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            options.forEach { (key, label, badge) ->
                val isSelected = selected == key
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(key) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AccentGoldSubtle else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AccentGold else TextMuted,
                            fontFamily = Inter,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.height(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (badge != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Success.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        badge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PricingCard(
    plan: PlanData,
    billingPeriod: String,
    currentPlan: Plan,
    isLoggedIn: Boolean,
    onClick: () -> Unit
) {
    val price = plan.prices[billingPeriod] ?: 0
    val animatedPrice by animateIntAsState(
        targetValue = price,
        animationSpec = tween(durationMillis = 240),
        label = "animatedPlanPrice"
    )
    val isCurrentPlan = when (currentPlan) {
        Plan.FREE -> plan.name == "Free"
        Plan.PRO -> plan.name == "Pro"
        Plan.DEVELOPER -> plan.name == "Developer"
    }
    val ctaText = when {
        isCurrentPlan -> "Current Plan"
        plan.name == "Free" -> "Get Started Free"
        isLoggedIn && plan.name != "Free" -> "Continue on Web"
        plan.name == "Developer" -> "Contact Sales"
        else -> "Upgrade to Pro"
    }
    val periodLabel = when (billingPeriod) {
        "quarterly" -> "/ month (billed quarterly)"
        "yearly" -> "/ month (billed yearly)"
        else -> "/ month"
    }

    val borderColor = if (plan.isRecommended) AccentGold.copy(alpha = 0.4f) else GlassBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (plan.isRecommended)
                    Modifier.border(1.5.dp, AccentGold.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        color = GlassBg,
        border = if (!plan.isRecommended) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Recommended badge
            if (plan.isRecommended) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = AccentGoldSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                ) {
                    Text(
                        "★ Recommended",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(plan.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary, fontFamily = Inter)
            Text(plan.tagline, fontSize = 13.sp, color = TextMuted)

            Spacer(Modifier.height(16.dp))

            // Price
            Text(
                text = "$$animatedPrice",
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = AccentGold,
                lineHeight = 46.sp
            )
            Text(
                if (animatedPrice == 0) "/ forever free" else periodLabel,
                fontSize = 13.sp, color = TextMuted
            )

            Spacer(Modifier.height(20.dp))

            // Features
            plan.features.forEach { (text, included) ->
                FeatureRow(text = text, included = included)
            }

            Spacer(Modifier.height(20.dp))

            // CTA
            if (plan.isRecommended && !isCurrentPlan) {
                GoldButton(
                    text = ctaText,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCurrentPlan
                )
            } else {
                GhostButton(
                    text = ctaText,
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCurrentPlan,
                    color = if (isCurrentPlan) Success else TextSecondary
                )
            }
        }
    }
}
