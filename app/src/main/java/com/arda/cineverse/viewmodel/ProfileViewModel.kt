package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.di.AppGraph
import com.arda.cineverse.notifications.CineVerseMessagingService
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.round
import javax.inject.Inject

import com.arda.cineverse.data.local.dao.SavedMovieDao
import com.arda.cineverse.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

data class ProfileUiState(
    val isLoading: Boolean = true,
    val username: String = "",
    val email: String = "",
    val avatarId: String = "default",
    val favoritesCount: Int = 0,
    val watchlistCount: Int = 0,
    val averageRatingGiven: Double = 0.0,
    val ratingsGivenCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val friendRepository: FriendRepository,
    private val savedMovieDao: SavedMovieDao,
    private val userPreferencesRepository: UserPreferencesRepository,
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

            // 1. Önce DataStore ve Room'daki yerel önbellek verisini yükleyelim (çevrimdışı anında açılış için)
            val prefs = runCatching { userPreferencesRepository.userPreferences.first() }.getOrNull()
            val localFavCount = runCatching { savedMovieDao.countByListType("FAVORITE") }.getOrDefault(0)
            val localWatchCount = runCatching { savedMovieDao.countByListType("WATCHLIST") }.getOrDefault(0)

            val fallbackUsername = prefs?.cachedUsername?.takeIf { it.isNotBlank() }
                ?: user?.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifBlank { "Kullanıcı" }
            val fallbackAvatarId = prefs?.cachedAvatarId?.takeIf { it.isNotBlank() } ?: "default"
            val fallbackRatingSum = prefs?.cachedRatingSum ?: 0L
            val fallbackRatingsCount = prefs?.cachedRatingCount ?: 0
            val fallbackAvgRating = if (fallbackRatingsCount > 0) round((fallbackRatingSum.toDouble() / fallbackRatingsCount) * 10) / 10.0 else 0.0

            _uiState.value = ProfileUiState(
                isLoading = false,
                username = fallbackUsername,
                email = email,
                avatarId = fallbackAvatarId,
                favoritesCount = localFavCount,
                watchlistCount = localWatchCount,
                averageRatingGiven = fallbackAvgRating,
                ratingsGivenCount = fallbackRatingsCount,
            )

            // 2. İnternet varsa Firestore'dan güncel veriyi paralel çek ve DataStore'a kaydet
            if (uid != null) {
                runCatching {
                    coroutineScope {
                        val userDocDeferred = async { runCatching { firestore.collection("users").document(uid).get().await() }.getOrNull() }
                        val favoritesCountDeferred = async {
                            runCatching {
                                firestore.collection("users").document(uid).collection("favorites")
                                    .count().get(AggregateSource.SERVER).await().count.toInt()
                            }.getOrNull()
                        }
                        val watchlistCountDeferred = async {
                            runCatching {
                                firestore.collection("users").document(uid).collection("watchlist")
                                    .count().get(AggregateSource.SERVER).await().count.toInt()
                            }.getOrNull()
                        }

                        val userDoc = userDocDeferred.await()
                        val remoteFavs = favoritesCountDeferred.await()
                        val remoteWatch = watchlistCountDeferred.await()

                        if (userDoc != null) {
                            val username = userDoc.getString("username")?.takeIf { it.isNotBlank() } ?: fallbackUsername
                            val avatarId = userDoc.getString("avatarId")?.takeIf { it.isNotBlank() } ?: fallbackAvatarId
                            val ratingSum = userDoc.getLong("ratingSum") ?: fallbackRatingSum
                            val ratingsCount = userDoc.getLong("ratingCount")?.toInt() ?: fallbackRatingsCount
                            val avgRating = if (ratingsCount > 0) round((ratingSum.toDouble() / ratingsCount) * 10) / 10.0 else 0.0

                            val finalFavCount = remoteFavs ?: localFavCount
                            val finalWatchCount = remoteWatch ?: localWatchCount

                            _uiState.value = ProfileUiState(
                                isLoading = false,
                                username = username,
                                email = email,
                                avatarId = avatarId,
                                favoritesCount = finalFavCount,
                                watchlistCount = finalWatchCount,
                                averageRatingGiven = avgRating,
                                ratingsGivenCount = ratingsCount,
                            )

                            userPreferencesRepository.saveCachedProfile(username, avatarId, ratingSum, ratingsCount)
                        }
                    }
                }
            }
        }
    }

    fun updateAvatar(avatarId: String) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(avatarId = avatarId)
        viewModelScope.launch {
            runCatching { firestore.collection("users").document(uid).update("avatarId", avatarId).await() }
        }
    }

    /** Kullanıcı adını değiştirir (bkz. FriendRepository.changeUsername — tekliği "usernames" claim belgesiyle garanti eder). */
    fun updateUsername(newUsername: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            friendRepository.changeUsername(newUsername).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(username = newUsername)
                    onResult(true, null)
                },
                onFailure = { error -> onResult(false, error.message ?: "Kullanıcı adı güncellenemedi") },
            )
        }
    }

    /**
     * Room'daki hesaba özel cache (favoriler/izleme geçmişi/kişisel öneriler)
     * önce temizlenip ANCAK ONDAN SONRA [onComplete] çağrılır — aksi halde
     * çağıran taraf hemen navigasyon yapıp bu ViewModel'i temizletirse,
     * temizleme coroutine'i yarıda iptal edilebilir.
     *
     * FCM token'ının silinmesi MUTLAKA auth.signOut()'tan ÖNCE olmalı —
     * silme işlemi firestore.rules'ta isOwner(uid) gerektiriyor, çıkış
     * yapıldıktan sonra kimlik geçersiz olacağından silme izni reddedilir.
     */
    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { CineVerseMessagingService.deleteCurrentToken() }
            runCatching { AppGraph.clearUserScopedCache() }
            runCatching { auth.signOut() }
            onComplete()
        }
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