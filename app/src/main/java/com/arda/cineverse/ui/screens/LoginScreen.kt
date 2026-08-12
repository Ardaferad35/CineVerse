package com.arda.cineverse.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arda.cineverse.R
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.AuthState
import com.arda.cineverse.viewmodel.AuthViewModel

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: (email: String) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    LaunchedEffect(authState) {
        val state = authState
        if (state is AuthState.Success) {
            onLoginSuccess(state.email)
            authViewModel.resetState()
        }
    }

    // validateAndSubmit() bir @Composable değil (onClick callback'i olarak
    // kullanılıyor), bu yüzden stringResource() burada değil, composable
    // gövdesinde önceden çözülüp closure ile yakalanıyor.
    val emailRequiredMessage = stringResource(R.string.auth_email_required)
    val emailInvalidMessage = stringResource(R.string.auth_email_invalid)
    val passwordRequiredMessage = stringResource(R.string.auth_password_required)

    fun validateAndSubmit() {
        emailError = when {
            email.isBlank() -> emailRequiredMessage
            !isValidEmail(email) -> emailInvalidMessage
            else -> null
        }
        passwordError = if (password.isBlank()) passwordRequiredMessage else null
        if (emailError == null && passwordError == null) {
            authViewModel.login(email, password, rememberMe)
        }
    }

    CineVerseAuthBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = OnSurface)
                }
                Spacer(Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.app_name),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(PrimaryGradient),
                            fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.login_welcome_back), color = OnSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.login_subtitle),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.login_hero_illustration),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1472f / 812f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                CVTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                    isError = emailError != null,
                    errorText = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(Modifier.height(14.dp))
                CVTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    placeholder = stringResource(R.string.auth_password_placeholder),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    isError = passwordError != null,
                    errorText = passwordError,
                )
            }

            item {
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
                        Text(stringResource(R.string.login_remember_me), color = OnSurface, style = MaterialTheme.typography.bodyMedium)
                    }
                    CVTextButton(stringResource(R.string.login_forgot_password), onClick = onNavigateToForgotPassword)
                }

                if (authState is AuthState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorColor.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            (authState as AuthState.Error).message,
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                CVGradientButton(
                    text = if (isLoading) stringResource(R.string.login_signing_in) else stringResource(R.string.login_sign_in),
                    onClick = ::validateAndSubmit,
                    enabled = !isLoading,
                    loading = isLoading,
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                CVDividerWithLabel(stringResource(R.string.auth_or_continue_with))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    CVSocialButton("Google", "G", onClick = {}, modifier = Modifier.weight(1f))
                    CVSocialButton("Apple", "", onClick = {}, modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.login_no_account), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    CVTextButton(stringResource(R.string.auth_register_link), onClick = onNavigateToRegister)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}