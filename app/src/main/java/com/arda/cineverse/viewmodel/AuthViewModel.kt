package com.arda.cineverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arda.cineverse.di.AppGraph
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success(email)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    /**
     * Yeni kullanıcı oluşturur (Firebase Authentication) ve
     * profil bilgilerini (isim, e-posta, kayıt tarihi) Firestore'daki
     * "users" koleksiyonuna kaydeder.
     */
    fun register(fullName: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Kullanıcı oluşturulamadı")

                val userProfile = hashMapOf(
                    "uid" to uid,
                    "fullName" to fullName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                firestore.collection("users").document(uid).set(userProfile).await()

                _authState.value = AuthState.Success(email)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapFirebaseError(e))
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
     */
    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
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