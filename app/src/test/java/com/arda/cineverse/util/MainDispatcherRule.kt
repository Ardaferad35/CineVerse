package com.arda.cineverse.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * ViewModel'ler `viewModelScope` (= Dispatchers.Main) üzerinden coroutine
 * başlatıyor; JVM unit testlerinde gerçek bir Android Main dispatcher yok
 * (Looper mevcut değil). Bu rule, testler süresince Dispatchers.Main'i
 * deterministik bir TestDispatcher'a çeviriyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
