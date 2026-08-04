package com.arda.cineverse.data.common

/** Favoriye ekleme/çıkarma gibi yazma işlemleri internet olmadan denendiğinde fırlatılır. */
class OfflineWriteException : Exception("Bu işlem için internet bağlantısı gerekiyor")

/**
 * İyimser UI değişikliği geri alınırken, hata [OfflineWriteException] DEĞİLSE
 * (ör. Firestore izin/zaman aşımı hatası) gösterilen genel mesaj — offline
 * olmayan hatalarda da kullanıcının işlemin kaydedilmediğini görmesi gerekiyor.
 */
const val GENERIC_WRITE_FAILURE_MESSAGE = "İşlem kaydedilemedi, tekrar deneyin"
