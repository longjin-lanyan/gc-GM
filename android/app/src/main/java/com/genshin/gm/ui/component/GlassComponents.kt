package com.genshin.gm.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================== Glass Colors ====================

val GlassTextColor = Color(0xFF333333)
val GlassSecondaryText = Color(0xFF666666)
val GlassPrimary = Color(0xFF667EEA)
val GlassPrimaryDark = Color(0xFF764BA2)
val GlassSuccess = Color(0xFF28a745)
val GlassError = Color(0xFFdc3545)
val GlassWarning = Color(0xFFffc107)

// Blue gradient fallback (when no bg image is synced)
val GlassGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFD6E4F0),
        Color(0xFF8EC5FC),
        Color(0xFF4A9BE8),
        Color(0xFF2878D0),
    ),
    start = Offset(0f, 0f),
    end = Offset(1500f, 2500f)
)

val GlassButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
)

// ==================== Glass Card ====================
// Frosted glass style matching web CSS:
//   background: rgba(255, 255, 255, 0.35)
//   backdrop-filter: blur(12px) (simulated with alpha)
//   border: 1px solid rgba(255, 255, 255, 0.5)
//   border-radius: 16px

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.35f,
    elevation: Dp = 2.dp,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = shape,
        color = Color.White.copy(alpha = alpha),
        contentColor = GlassTextColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

// ==================== Glass Text Field Colors ====================
// Matches web: border rgba(102,126,234,0.25), bg rgba(255,255,255,0.5)

@Composable
fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextColor,
    unfocusedTextColor = GlassTextColor,
    focusedContainerColor = Color.White.copy(alpha = 0.4f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.25f),
    focusedBorderColor = GlassPrimary,
    unfocusedBorderColor = GlassPrimary.copy(alpha = 0.25f),
    focusedLabelColor = GlassPrimary,
    unfocusedLabelColor = GlassSecondaryText,
    cursorColor = GlassPrimary,
    focusedLeadingIconColor = GlassPrimary,
    unfocusedLeadingIconColor = GlassSecondaryText,
    focusedPlaceholderColor = GlassSecondaryText,
    unfocusedPlaceholderColor = Color(0xFF999999),
)

// ==================== Glass Tab Row ====================
// Matches web .menu: rgba(255,255,255,0.25) + blur, rounded 16dp

@Composable
fun GlassTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Brush.linearGradient(
                            colors = listOf(GlassPrimary, GlassPrimaryDark),
                            start = Offset(0f, 0f),
                            end = Offset(300f, 300f)
                        )
                        else Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    color = if (isSelected) Color.White else GlassPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ==================== Glass Gradient Button ====================
// Matches web: gradient(135deg, #667eea, #764ba2), rounded 12dp

@Composable
fun GlassGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassPrimary,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFB0B0C0),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        ),
        content = content
    )
}

// ==================== Glass Chip ====================
// Matches web .sub-btn: frosted glass + border, active: gradient

@Composable
fun GlassChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .shadow(if (selected) 2.dp else 0.dp, shape)
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(
                    colors = listOf(GlassPrimary, GlassPrimaryDark)
                )
                else Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.5f),
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else GlassPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ==================== Form Label ====================

@Composable
fun GlassFormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = GlassTextColor,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// ==================== Info Card ====================
// Frosted glass with left accent border (blue stripe)
// background: rgba(255,255,255,0.35), border-left 4px #667eea, rounded 16dp

@Composable
fun GlassInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = shape,
        color = Color.White.copy(alpha = 0.35f),
        contentColor = GlassTextColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent border (matches web: border-left 4px solid #667eea)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(GlassPrimary)
            )
            Column(modifier = Modifier.padding(15.dp), content = content)
        }
    }
}
