package com.arda.cineverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.AuthState
import com.arda.cineverse.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
private fun hasNumberOrSymbol(s: String) = s.any { it.isDigit() || !it.isLetterOrDigit() }
private fun hasUpperAndLower(s: String) = s.any { it.isUpperCase() } && s.any { it.isLowerCase() }
private const val USERNAME_CHECK_DEBOUNCE_MS = 400L

@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onRegisterSuccess: (email: String) -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val friendRepository = remember { FriendRepository() }
    val usernameFormatValid = username.matches(FriendRepository.USERNAME_REGEX)

    LaunchedEffect(username) {
        usernameAvailable = null
        if (usernameFormatValid) {
            delay(USERNAME_CHECK_DEBOUNCE_MS)
            friendRepository.checkUsernameAvailable(username)
                .onSuccess { usernameAvailable = it }
        }
    }

    val lengthOk = password.length >= 8
    val numberOrSymbolOk = hasNumberOrSymbol(password)
    val caseOk = hasUpperAndLower(password)
    val passwordStrongEnough = lengthOk && numberOrSymbolOk && caseOk

    val authState by authViewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    LaunchedEffect(authState) {
        val state = authState
        if (state is AuthState.Success) {
            onRegisterSuccess(state.email)
            authViewModel.resetState()
        }
    }

    fun validateAndSubmit() {
        usernameError = when {
            username.isBlank() -> "Kullanıcı adı gerekli"
            !usernameFormatValid -> "3-20 karakter, sadece küçük harf/rakam/_ kullanın"
            usernameAvailable == false -> "Bu kullanıcı adı alınmış"
            else -> null
        }
        emailError = when {
            email.isBlank() -> "E-posta gerekli"
            !isValidEmail(email) -> "Geçerli bir e-posta girin"
            else -> null
        }
        confirmError = if (confirmPassword != password) "Şifreler eşleşmiyor" else null

        if (usernameError == null && emailError == null && confirmError == null &&
            passwordStrongEnough && agreedToTerms
        ) {
            authViewModel.register(username, email, password)
        }
    }

    CineVerseAuthBackground {
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

            Text("Hesap Oluştur", color = OnSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "CineVerse'e katılın ve binlerce film ile diziyi keşfedin",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(28.dp))

            CVTextField(
                value = username,
                onValueChange = { username = it.lowercase(); usernameError = null },
                placeholder = "Kullanıcı adı",
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary) },
                isError = usernameError != null,
                errorText = usernameError,
            )
            if (usernameError == null && username.isNotBlank() && usernameFormatValid) {
                Text(
                    text = when (usernameAvailable) {
                        true -> "Kullanılabilir"
                        false -> "Bu kullanıcı adı alınmış"
                        null -> "Kontrol ediliyor..."
                    },
                    color = if (usernameAvailable == true) Primary else TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            CVTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "E-posta adresi",
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextSecondary) },
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(14.dp))
            CVTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Şifre",
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
            )
            Spacer(Modifier.height(14.dp))
            CVTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmError = null },
                placeholder = "Şifreyi tekrar edin",
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
                isPassword = true,
                passwordVisible = confirmPasswordVisible,
                onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                isError = confirmError != null,
                errorText = confirmError,
            )

            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CVValidationRow("En az 8 karakter", lengthOk)
                CVValidationRow("Rakam veya sembol içeriyor", numberOrSymbolOk)
                CVValidationRow("Büyük ve küçük harf içeriyor", caseOk)
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = TextSecondary),
                )
                Text("Kullanım Şartları & Gizlilik Politikasını kabul ediyorum", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    (authState as AuthState.Error).message,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(16.dp))
            CVGradientButton(
                text = if (isLoading) "Hesap oluşturuluyor..." else "Kayıt Ol",
                onClick = ::validateAndSubmit,
                enabled = agreedToTerms && !isLoading,
            )

            Spacer(Modifier.height(24.dp))
            CVDividerWithLabel("veya şununla devam et")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CVSocialButton("Google", "G", onClick = {}, modifier = Modifier.weight(1f))
                CVSocialButton("Apple", "", onClick = {}, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Zaten hesabınız var mı? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                CVTextButton("Giriş Yap", onClick = onNavigateToLogin)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}