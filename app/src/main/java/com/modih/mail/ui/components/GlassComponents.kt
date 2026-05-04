package com.modih.mail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modih.mail.ui.theme.*

// ==================== GLASS CARD ====================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, GlassBorder, RoundedCornerShape(cornerRadius))
            .shadow(20.dp, RoundedCornerShape(cornerRadius), ambientColor = Color.Black.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = GlassBg),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

// ==================== GLASS PILL ====================
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = Color(0x14FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ==================== PRIMARY BUTTON (Gold glow) ====================
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(if (isPressed) 2.dp else 8.dp, label = "elevation")

    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .shadow(elevation, RoundedCornerShape(14.dp), ambientColor = AccentGold.copy(alpha = 0.35f)),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (enabled) listOf(
                            AccentGold.copy(alpha = 0.95f),
                            AccentGoldDim.copy(alpha = 0.9f)
                        ) else listOf(
                            AccentGold.copy(alpha = 0.3f),
                            AccentGoldDim.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BgPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BgPrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        color = BgPrimary,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== GHOST BUTTON ====================
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = TextSecondary,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (enabled) GlassBorderHover else GlassBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            containerColor = Color.Transparent
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== SECTION HEADER ====================
@Composable
fun SectionHeader(
    pill: String,
    title: String,
    accentWord: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassPill(text = pill, icon = "✦")
        Spacer(Modifier.height(16.dp))

        // Title with accent word in italic gold
        val parts = title.split(accentWord)
        Text(
            text = title, // fallback
            style = MaterialTheme.typography.displayMedium.copy(
                color = TextPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// ==================== STATUS DOT ====================
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

// ==================== PLAN BADGE ====================
@Composable
fun PlanBadge(
    plan: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (plan.lowercase()) {
        "pro" -> Triple(AccentGoldSubtle, AccentGold, "PRO")
        "developer" -> Triple(DevPurpleSubtle, ProPurple, "DEV")
        else -> Triple(Color(0x14FFFFFF), TextMuted, "FREE")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            StatusDot(color = textColor, size = 6.dp)
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textColor,
                fontFamily = Inter
            )
        }
    }
}

// ==================== FEATURE ROW (for pricing) ====================
@Composable
fun FeatureRow(
    text: String,
    included: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (included) "✓" else "✗",
            color = if (included) Success else Danger.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (included) TextSecondary else TextDim
        )
    }
}

// ==================== TOAST ====================
@Composable
fun ModihToast(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (visible) {
        Surface(
            modifier = modifier
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(14.dp),
            color = GlassBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}
