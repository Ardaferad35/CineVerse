package com.arda.cineverse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arda.cineverse.ui.theme.*

import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun CVGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(if (enabled) PrimaryGradient else listOf(SurfaceVariant, SurfaceVariant)),
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = OnPrimary,
                fontSize = 18.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun CVOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun CVTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            color = Primary,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun CVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = TextSecondary) },
            leadingIcon = leadingIcon,
            trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
                {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextSecondary,
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                errorContainerColor = Surface,
                focusedBorderColor = if (isError) ErrorColor else Primary,
                unfocusedBorderColor = if (isError) ErrorColor else DividerColor,
                errorBorderColor = ErrorColor,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                cursorColor = Primary,
            ),
        )
        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun CVSocialButton(label: String, glyph: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, color = OnSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(8.dp))
        Text(label, color = OnSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CVValidationRow(text: String, satisfied: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(50))
                .background(if (satisfied) Accent else DividerColor),
            contentAlignment = Alignment.Center,
        ) {
            if (satisfied) {
                Text("✓", color = Background, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (satisfied) OnSurface else TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CVDividerWithLabel(label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Divider(color = DividerColor, modifier = Modifier.weight(1f))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
        Divider(color = DividerColor, modifier = Modifier.weight(1f))
    }
}