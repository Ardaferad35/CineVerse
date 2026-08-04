# CineVerse R8 keep kuralları.
#
# Retrofit/OkHttp/Gson/Firebase kendi consumer kurallarıyla geliyor; burada
# sadece REFLECTION ile alan adı üzerinden doldurulan PROJE sınıfları korunuyor.
# Bu sınıflar obfuscate edilirse alan adları (ör. poster_path) değişir ve
# JSON/Firestore eşlemesi sessizce null döner — derleme hatası vermez,
# çalışma zamanında boş ekran olarak görünür.

# Gson: TMDB ve Gemini DTO'ları (alan adları JSON anahtarlarıyla birebir).
-keep class com.arda.cineverse.data.remote.dto.** { *; }

# Firestore toObject() ve Gson: domain modelleri (Comment, SavedMovie,
# AppNotification, Friend* vb. Firestore belgelerinden reflection ile okunuyor).
-keep class com.arda.cineverse.data.model.** { *; }

# Retrofit'in suspend fonksiyonları ve generic dönüş tipleri için imzalar.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
