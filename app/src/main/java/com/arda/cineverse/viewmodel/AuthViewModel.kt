package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.data.repository.FriendRepository
import com.arda.cineverse.data.repository.UsernameTakenException
import com.arda.cineverse.di.AppGraph
import com.arda.cineverse.notifications.CineVerseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                runCatching { CineVerseMessagingService.registerCurrentToken() }
                _authState.value = AuthState.Success(email)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    /**
     * Yeni kullanıcı oluşturur (Firebase Authentication) ve profil bilgilerini
     * (kullanıcı adı, e-posta, kayıt tarihi) Firestore'daki "users"
     * koleksiyonuna kaydeder. Kullanıcı adı, "usernames/{username}" koleksiyonunda
     * tek bir "claim" belgesiyle (bkz. FriendRepository) tekliği garanti edilerek
     * AYNI transaction içinde alınır.
     *
     * Ayrı bir "ad soyad" alanı YOK — kullanıcının kimliği tamamen kullanıcı
     * adı. "fullName" alanı yine de yazılıyor (değeri = username) çünkü
     * CommentRepository/FriendRepository gibi mevcut kod hâlâ bu alanı okuyup
     * görüntülüyor; ayrı bir şema değişikliği/geri uyumluluk işi çıkarmamak
     * için alan adı korunuyor, sadece kullanıcıdan artık ayrıca istenmiyor.
     *
     * Auth hesabı oluşturma ile profil/kullanıcı-adı yazımı iki ayrı adım
     * olduğundan (Firestore transaction Auth hesabını kapsayamıyor), kullanıcı
     * adı alınmışsa ya da profil yazımı başka bir sebeple başarısız olursa az
     * önce oluşturulan Auth hesabı GERİ ALINIR — aksi halde hesap yetim kalır
     * ve kullanıcı aynı email ile tekrar kayıt olmaya çalıştığında "already in
     * use" hatasına düşer.
     */
    fun register(username: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            var accountCreated = false
            try {
                require(username.matches(FriendRepository.USERNAME_REGEX)) {
                    "Kullanıcı adı 3-20 karakter olmalı, sadece küçük harf/rakam/alt çizgi içerebilir"
                }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Kullanıcı oluşturulamadı")
                accountCreated = true

                firestore.runTransaction { txn ->
                    val usernameRef = firestore.collection("usernames").document(username)
                    if (txn.get(usernameRef).exists()) throw UsernameTakenException()
                    txn.set(usernameRef, mapOf("uid" to uid))
                    // "email" alanı BİLEREK yazılmıyor: users/{uid} belgesi
                    // firestore.rules gereği giriş yapmış HERKESE okunabilir
                    // (yorum avatarları için gerekli) — e-posta buraya yazılsaydı
                    // tüm kullanıcıların e-postası birbirine sızardı. E-postayı
                    // okuyan tüm kod zaten auth.currentUser.email kullanıyor.
                    txn.set(
                        firestore.collection("users").document(uid),
                        hashMapOf(
                            "uid" to uid,
                            "username" to username,
                            "fullName" to username,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }.await()

                runCatching { CineVerseMessagingService.registerCurrentToken() }
                _authState.value = AuthState.Success(email)
            } catch (e: Exception) {
                if (accountCreated) {
                    runCatching { auth.currentUser?.delete()?.await() }
                }
                _authState.value = AuthState.Error(
                    if (e is UsernameTakenException) e.message ?: "Bu kullanıcı adı zaten alınmış"
                    else mapFirebaseError(e)
                )
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, mapFirebaseError(e))
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun currentUserEmail(): String? = auth.currentUser?.email

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
            auth.signOut()
            onComplete()
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        return when {
            e.message?.contains("badly formatted", ignoreCase = true) == true -> "Geçerli bir e-posta girin"
            e.message?.contains("password is invalid", ignoreCase = true) == true -> "Şifre hatalı"
            e.message?.contains("no user record", ignoreCase = true) == true -> "Bu e-posta ile kayıtlı kullanıcı yok"
            e.message?.contains("already in use", ignoreCase = true) == true -> "Bu e-posta zaten kayıtlı"
            e.message?.contains("network", ignoreCase = true) == true -> "İnternet bağlantınızı kontrol edin"
            else -> e.message ?: "Bir hata oluştu, tekrar deneyin"
        }
    }
}