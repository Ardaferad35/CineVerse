@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arda.cineverse.viewmodel

import com.arda.cineverse.util.MainDispatcherRule
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * register()'daki Firestore transaction gövdesi (kullanıcı adı claim'i +
 * profil yazımı) burada KASITLI OLARAK test edilmiyor — Transaction API'yi
 * anlamlı şekilde mock'lamak bir emulator/instrumented test gerektirir.
 * Burada register()'ın çevresindeki kontrol akışı (doğrulama, hesap geri
 * alma) test ediliyor.
 */
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth: FirebaseAuth = mockk()
    private val firestore: FirebaseFirestore = mockk()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        viewModel = AuthViewModel(auth, firestore)
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    private fun <T> taskReturning(value: T): Task<T> {
        val task = mockk<Task<T>>()
        coEvery { task.await() } returns value
        return task
    }

    private fun <T> taskFailingWith(message: String): Task<T> {
        val task = mockk<Task<T>>()
        coEvery { task.await() } throws Exception(message)
        return task
    }

    private fun errorState(): AuthState.Error {
        assertTrue(viewModel.authState.value is AuthState.Error)
        return viewModel.authState.value as AuthState.Error
    }

    @Test
    fun `login basarili olunca Success durumuna geciyor`() = runTest {
        val authResult = mockk<AuthResult>()
        every { auth.signInWithEmailAndPassword("a@b.com", "pw") } returns taskReturning(authResult)

        viewModel.login("a@b.com", "pw")
        advanceUntilIdle()

        assertEquals(AuthState.Success("a@b.com"), viewModel.authState.value)
    }

    @Test
    fun `login basarisiz olunca Firebase hata mesaji Turkce mesaja ceviriliyor`() = runTest {
        every { auth.signInWithEmailAndPassword(any(), any()) } returns
            taskFailingWith<AuthResult>("There is no user record corresponding to this identifier")

        viewModel.login("a@b.com", "pw")
        advanceUntilIdle()

        val state = errorState()
        assertEquals("Bu e-posta ile kayıtlı kullanıcı yok", state.message)
    }

    @Test
    fun `login sirasinda taninmayan hata mesaji oldugu gibi gosteriliyor`() = runTest {
        every { auth.signInWithEmailAndPassword(any(), any()) } returns
            taskFailingWith<AuthResult>("something unexpected happened")

        viewModel.login("a@b.com", "pw")
        advanceUntilIdle()

        val state = errorState()
        assertEquals("something unexpected happened", state.message)
    }

    @Test
    fun `login cagrilir cagrilmaz Loading durumuna geciyor`() = runTest {
        // _authState.value = Loading, viewModelScope.launch { } BLOGUNA
        // GIRMEDEN, senkron olarak set ediliyor — StandardTestDispatcher
        // ilerletilmeden (advanceUntilIdle cagrilmadan) bu deger zaten
        // gozlemlenebilir olmali.
        viewModel.login("a@b.com", "pw")

        assertEquals(AuthState.Loading, viewModel.authState.value)
    }

    @Test
    fun `register gecersiz kullanici adinda Firebase'e hic istek atmadan Error donuyor`() = runTest {
        viewModel.register(username = "AB", email = "a@b.com", password = "pw")
        advanceUntilIdle()

        val state = errorState()
        assertEquals(
            "Kullanıcı adı 3-20 karakter olmalı, sadece küçük harf/rakam/alt çizgi içerebilir",
            state.message,
        )
        verify(exactly = 0) { auth.createUserWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `register hesap olusturma basarisiz olunca hesap silme denenmiyor`() = runTest {
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns
            taskFailingWith<AuthResult>("email address is already in use")
        every { auth.currentUser } returns null

        viewModel.register(username = "validuser", email = "a@b.com", password = "pw")
        advanceUntilIdle()

        val state = errorState()
        assertEquals("Bu e-posta zaten kayıtlı", state.message)
        // accountCreated hicbir zaman true olmadi (createUser basarisiz oldu),
        // bu yuzden rollback (currentUser.delete()) denenmemeli.
        verify(exactly = 0) { auth.currentUser?.delete() }
    }

    @Test
    fun `sendPasswordResetEmail basarili olunca callback true donuyor`() = runTest {
        every { auth.sendPasswordResetEmail("a@b.com") } returns taskReturning<Void?>(null)

        var success: Boolean? = null
        var errorMessage: String? = null
        viewModel.sendPasswordResetEmail("a@b.com") { ok, message ->
            success = ok
            errorMessage = message
        }
        advanceUntilIdle()

        assertEquals(true, success)
        assertNull(errorMessage)
    }

    @Test
    fun `sendPasswordResetEmail basarisiz olunca callback false ve mesaj donuyor`() = runTest {
        every { auth.sendPasswordResetEmail(any()) } returns
            taskFailingWith<Void>("network error occurred")

        var success: Boolean? = null
        var errorMessage: String? = null
        viewModel.sendPasswordResetEmail("a@b.com") { ok, message ->
            success = ok
            errorMessage = message
        }
        advanceUntilIdle()

        assertEquals(false, success)
        assertEquals("İnternet bağlantınızı kontrol edin", errorMessage)
    }

    @Test
    fun `resetState Idle durumuna donduruyor`() {
        viewModel.resetState()
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun `currentUserEmail giris yapilmis kullanicinin e-postasini donduruyor`() {
        val user = mockk<FirebaseUser> { every { email } returns "a@b.com" }
        every { auth.currentUser } returns user

        assertEquals("a@b.com", viewModel.currentUserEmail())
    }

    @Test
    fun `currentUserEmail giris yapilmamissa null donduruyor`() {
        every { auth.currentUser } returns null

        assertNull(viewModel.currentUserEmail())
    }

    @Test
    fun `signOut auth signOut'u cagirip onComplete'i tetikliyor`() = runTest {
        every { auth.signOut() } returns Unit

        var completed = false
        viewModel.signOut { completed = true }
        advanceUntilIdle()

        verify { auth.signOut() }
        assertEquals(true, completed)
    }
}
