package com.arda.cineverse.data.common

/** Favoriye ekleme/çıkarma gibi yazma işlemleri internet olmadan denendiğinde fırlatılır. */
class OfflineWriteException : Exception("Bu işlem için internet bağlantısı gerekiyor")
