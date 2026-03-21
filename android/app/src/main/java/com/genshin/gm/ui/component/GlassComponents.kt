package com.genshin.gm.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ==================== Glass Colors ====================

val GlassTextColor = Color(0xFF333333)
val GlassSecondaryText = Color(0xFF666666)
val GlassPrimary = Color(0xFF667EEA)
val GlassPrimaryDark = Color(0xFF764BA2)
val GlassSuccess = Color(0xFF28a745)
val GlassError = Color(0xFFdc3545)
val GlassWarning = Color(0xFFffc107)

val GlassGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFE091C8),
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
)

val GlassButtonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
)

// ==================== Glass Card ====================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.45f,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = alpha),
        contentColor = GlassTextColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ==================== Glass Text Field Colors ====================

@Composable
fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextColor,
    unfocusedTextColor = GlassTextColor,
    focusedBorderColor = GlassPrimary,
    unfocusedBorderColor = Color(0xFFCCCCCC),
    focusedLabelColor = GlassPrimary,
    unfocusedLabelColor = GlassSecondaryText,
    cursorColor = GlassPrimary,
    focusedLeadingIconColor = GlassPrimary,
    unfocusedLeadingIconColor = GlassSecondaryText,
    focusedPlaceholderColor = GlassSecondaryText,
    unfocusedPlaceholderColor = Color(0xFF999999),
)

// ==================== Glass Tab Row ====================

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
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.7f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    color = if (isSelected) GlassPrimary else GlassSecondaryText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ==================== Glass Gradient Button ====================

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
        content = content
    )
}

// ==================== Glass Chip ====================

@Composable
fun GlassChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) GlassPrimary.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.35f)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) GlassPrimary else GlassTextColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
