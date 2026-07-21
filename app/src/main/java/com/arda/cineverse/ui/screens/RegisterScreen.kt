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
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
private fun hasNumberOrSymbol(s: String) = s.any { it.isDigit() || !it.isLetterOrDigit() }
private fun hasUpperAndLower(s: String) = s.any { it.isUpperCase() } && s.any { it.isLowerCase() }

@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onRegisterSuccess: (email: String) -> Unit = {},
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val lengthOk = password.length >= 8
    val numberOrSymbolOk = hasNumberOrSymbol(password)
    val caseOk = hasUpperAndLower(password)
    val passwordStrongEnough = lengthOk && numberOrSymbolOk && caseOk

    fun validateAndSubmit() {
        nameError = if (fullName.isBlank()) "İsim gerekli" else null
        emailError = when {
            email.isBlank() -> "E-posta gerekli"
            !isValidEmail(email) -> "Geçerli bir e-posta girin"
            else -> null
        }
        confirmError = if (confirmPassword != password) "Şifreler eşleşmiyor" else null

        if (nameError == null && emailError == null && confirmError == null && passwordStrongEnough && agreedToTerms) {
            onRegisterSuccess(email)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
         //   .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
        }
        Spacer(Modifier.height(12.dp))

        Text("Create Account", color = OnSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Join CineVerse and explore thousands of movies",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(28.dp))

        CVTextField(
            value = fullName,
            onValueChange = { fullName = it; nameError = null },
            placeholder = "Full name",
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary) },
            isError = nameError != null,
            errorText = nameError,
        )
        Spacer(Modifier.height(14.dp))
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
            onValueChange = { password = it },
            placeholder = "Password",
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        )
        Spacer(Modifier.height(14.dp))
        CVTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmError = null },
            placeholder = "Confirm password",
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary) },
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
            isError = confirmError != null,
            errorText = confirmError,
        )

        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CVValidationRow("At least 8 characters", lengthOk)
            CVValidationRow("Contains number or symbol", numberOrSymbolOk)
            CVValidationRow("Mix of uppercase and lowercase", caseOk)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreedToTerms,
                onCheckedChange = { agreedToTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = TextSecondary),
            )
            Text("I agree to the Terms & Conditions", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        CVGradientButton("Sign Up", onClick = ::validateAndSubmit, enabled = agreedToTerms)

        Spacer(Modifier.height(24.dp))
        CVDividerWithLabel("or continue with")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            CVSocialButton("Google", "G", onClick = {}, modifier = Modifier.weight(1f))
            CVSocialButton("Apple", "", onClick = {}, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            CVTextButton("Sign In", onClick = onNavigateToLogin)
        }
        Spacer(Modifier.height(24.dp))
    }
}
