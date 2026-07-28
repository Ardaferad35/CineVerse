package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.round

data class ProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val email: String = "",
    val avatarId: String = "default",
    val favoritesCount: Int = 0,
    val watchlistCount: Int = 0,
    val averageRatingGiven: Double = 0.0,
    val ratingsGivenCount: Int = 0,
)

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        val user = auth.currentUser
        val uid = user?.uid
        val email = user?.email ?: ""

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val userDoc = uid?.let {
                runCatching { firestore.collection("users").document(it).get().await() }.getOrNull()
            }
            val fullName = userDoc?.getString("fullName") ?: email.substringBefore("@")
            val avatarId = userDoc?.getString("avatarId") ?: "default"

            val favoritesCount = uid?.let {
                runCatching {
                    firestore.collection("users").document(it).collection("favorites").get().await().documents.size
                }.getOrNull()
            } ?: 0

            val watchlistCount = uid?.let {
                runCatching {
                    firestore.collection("users").document(it).collection("watchlist").get().await().documents.size
                }.getOrNull()
            } ?: 0

            // ratingSum/ratingCount, her yorum eklendikçe/düzenlendikçe/silindikçe
            // CommentRepository tarafından anlık güncellenen alanlar — burada sadece
            // okuyup ortalamayı hesaplıyoruz, hiçbir tarama sorgusu gerekmiyor.
            val ratingSum = userDoc?.getLong("ratingSum") ?: 0L
            val ratingsCount = userDoc?.getLong("ratingCount")?.toInt() ?: 0
            val avgRating = if (ratingsCount > 0) round((ratingSum.toDouble() / ratingsCount) * 10) / 10.0 else 0.0

            _uiState.value = ProfileUiState(
                isLoading = false,
                fullName = fullName,
                email = email,
                avatarId = avatarId,
                favoritesCount = favoritesCount,
                watchlistCount = watchlistCount,
                averageRatingGiven = avgRating,
                ratingsGivenCount = ratingsCount,
            )
        }
    }

    fun updateAvatar(avatarId: String) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(avatarId = avatarId)
        viewModelScope.launch {
            runCatching { firestore.collection("users").document(uid).update("avatarId", avatarId).await() }
        }
    }

    fun updateFullName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        if (newName.isBlank()) return
        _uiState.value = _uiState.value.copy(fullName = newName)
        viewModelScope.launch {
            runCatching { firestore.collection("users").document(uid).update("fullName", newName).await() }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Firebase, şifre değiştirme gibi hassas işlemler için kullanıcının
     * "yakın zamanda giriş yapmış" olmasını şart koşuyor. Bu yüzden önce
     * mevcut şifreyle yeniden kimlik doğrulaması (reauthenticate) yapıp,
     * ardından yeni şifreyi ayarlıyoruz.
     */
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onResult(false, "Kullanıcı bulunamadı")
            return
        }
        viewModelScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, mapPasswordError(e))
            }
        }
    }

    private fun mapPasswordError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("password is invalid", ignoreCase = true) -> "Mevcut şifreniz hatalı"
            message.contains("credential is incorrect", ignoreCase = true) -> "Mevcut şifreniz hatalı"
            message.contains("weak", ignoreCase = true) -> "Yeni şifre çok zayıf, en az 6 karakter olmalı"
            message.contains("network", ignoreCase = true) -> "İnternet bağlantınızı kontrol edin"
            message.contains("recent", ignoreCase = true) -> "Güvenlik nedeniyle tekrar giriş yapmanız gerekiyor"
            else -> message.ifBlank { "Bir hata oluştu, tekrar deneyin" }
        }
    }
}