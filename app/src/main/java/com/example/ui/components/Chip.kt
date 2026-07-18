package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.ui.theme.spacing

@Composable
fun FloraFlowChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onActiveColor: Color = MaterialTheme.colorScheme.onPrimary,
    inactiveColor: Color = Color.Transparent,
    onInactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) activeColor else inactiveColor)
            .border(
                width = 1.dp,
                color = if (selected) activeColor else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) onActiveColor else onInactiveColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
