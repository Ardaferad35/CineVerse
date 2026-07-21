package com.arda.cineverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.ui.theme.CineVerseLoginBackground

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

@Composable
fun LoginScreen(

    onBack: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: (email: String) -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        emailError = when {
            email.isBlank() -> "E-posta gerekli"
            !isValidEmail(email) -> "Geçerli bir e-posta girin"
            else -> null
        }
        passwordError = if (password.isBlank()) "Şifre gerekli" else null
        if (emailError == null && passwordError == null) onLoginSuccess(email)
    }

    CineVerseLoginBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
            }
            Spacer(Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "CineVerse",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(PrimaryGradient),
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Text("Welcome back!", color = OnSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sign in to continue your movie journey",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            CVTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "Email address",
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(14.dp))
            CVTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                placeholder = "Password",
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                isError = passwordError != null,
                errorText = passwordError,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = TextSecondary),
                    )
                    Text("Remember me", color = OnSurface, style = MaterialTheme.typography.bodyMedium)
                }
                CVTextButton("Forgot Password?", onClick = onNavigateToForgotPassword)
            }

            Spacer(Modifier.height(16.dp))
            CVGradientButton("Sign In", onClick = ::validateAndSubmit)

            Spacer(Modifier.height(24.dp))
            CVDividerWithLabel("or continue with")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CVSocialButton("Google", "G", onClick = {}, modifier = Modifier.weight(1f))
                CVSocialButton("Apple", "", onClick = {}, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Don't have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                CVTextButton("Sign Up", onClick = onNavigateToRegister)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
