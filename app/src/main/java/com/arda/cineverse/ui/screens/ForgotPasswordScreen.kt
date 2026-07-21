package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var linkSent by remember { mutableStateOf(false) }

    fun submit() {
        emailError = when {
            email.isBlank() -> "E-posta gerekli"
            !isValidEmail(email) -> "Geçerli bir e-posta girin"
            else -> null
        }
        if (emailError == null) linkSent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // .background(Background)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
        }
        Spacer(Modifier.height(24.dp))

        if (!linkSent) {
            Text("Forgot Password?", color = OnSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your email and we'll send you a link to reset your password",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(28.dp))
            CVTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "Email address",
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(20.dp))
            CVGradientButton("Send Reset Link", onClick = ::submit)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Remember your password? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                CVTextButton("Sign In", onClick = onNavigateToLogin)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MarkEmailRead, contentDescription = null, tint = Accent, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Check your email", color = OnSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "We've sent a password reset link to\n$email",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                CVGradientButton("Back to Sign In", onClick = onNavigateToLogin)
                Spacer(Modifier.height(16.dp))
                CVTextButton("Didn't get the email? Resend", onClick = { linkSent = true })
            }
        }
    }
}
