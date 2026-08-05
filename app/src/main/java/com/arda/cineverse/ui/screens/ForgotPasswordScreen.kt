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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arda.cineverse.R
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.AuthViewModel

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var linkSent by remember { mutableStateOf(false) }

    // submit() bir @Composable değil (onClick callback'i olarak kullanılıyor),
    // bu yüzden stringResource() burada değil, composable gövdesinde önceden
    // çözülüp closure ile yakalanıyor.
    val emailRequiredMessage = stringResource(R.string.auth_email_required)
    val emailInvalidMessage = stringResource(R.string.auth_email_invalid)
    val genericErrorMessage = stringResource(R.string.forgot_password_generic_error)

    fun submit() {
        emailError = when {
            email.isBlank() -> emailRequiredMessage
            !isValidEmail(email) -> emailInvalidMessage
            else -> null
        }
        if (emailError != null) return

        sendError = null
        isLoading = true
        authViewModel.sendPasswordResetEmail(email) { success, error ->
            isLoading = false
            if (success) {
                linkSent = true
            } else {
                sendError = error ?: genericErrorMessage
            }
        }
    }

    CineVerseAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = OnSurface)
            }
            Spacer(Modifier.height(24.dp))

            if (!linkSent) {
                Text(stringResource(R.string.login_forgot_password), color = OnSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.forgot_password_subtitle),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(28.dp))
                CVTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null; sendError = null },
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                    isError = emailError != null,
                    errorText = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                if (sendError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(sendError!!, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(20.dp))
                CVGradientButton(
                    text = if (isLoading) stringResource(R.string.forgot_password_sending) else stringResource(R.string.forgot_password_send_link),
                    onClick = ::submit,
                    enabled = !isLoading,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.forgot_password_remember), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    CVTextButton(stringResource(R.string.login_sign_in), onClick = onNavigateToLogin)
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
                    Text(stringResource(R.string.forgot_password_check_email_title), color = OnSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.forgot_password_link_sent, email),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    CVGradientButton(stringResource(R.string.forgot_password_back_to_login), onClick = onNavigateToLogin)
                    Spacer(Modifier.height(16.dp))
                    CVTextButton(
                        text = if (isLoading) stringResource(R.string.forgot_password_resending) else stringResource(R.string.forgot_password_resend),
                        onClick = ::submit,
                    )
                }
            }
        }
    }
}