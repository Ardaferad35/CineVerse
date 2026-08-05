# Implementation Plan - Fix SecurityException: Unknown calling package name 'com.google.android.gms'

The application is experiencing a `java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'` error, which typically indicates a package visibility or identity verification issue between the app and Google Play Services (GMS). This is common on Android 11+ (API 30+) and especially with new API levels like 34/35.

## User Review Required

> [!IMPORTANT]
> This fix involves downgrading the `compileSdk` from `36` to `35`. API 36 is likely a preview/experimental version and might be causing compatibility issues with current Google Play Services libraries.
>
> Also, ensuring the SHA-1 of your signing key (even the debug one) is registered in the Firebase Console is crucial for GMS to verify your app's identity, which is often the root cause of this specific exception.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/build.gradle.kts)
- Change `compileSdk` from `release(36)` to `35`.
- Ensure `targetSdk` remains `35`.
- Verify and potentially update Firebase/GMS dependency versions if needed (currently using BOM 34.15.0 which is very recent).

### Android Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/AndroidManifest.xml)
- Enhance the `<queries>` section to include specific intents for Firebase Messaging and Google Play Services.
- This ensures the system correctly resolves the relationship between the app and GMS.

### Notification Service

#### [MODIFY] [CineVerseMessagingService.kt](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/java/com/arda/cineverse/notifications/CineVerseMessagingService.kt)
- Add a small delay and a retry mechanism when fetching the FCM token during app startup. This helps if GMS is still initializing its package visibility cache.

## Verification Plan

### Automated Tests
- Run the application and verify that the `SecurityException` no longer occurs during startup (where `registerCurrentToken` is called).
- Check Logcat for "registerCurrentToken: fcmTokens'a yazma başarılı" to confirm successful GMS interaction.

### Manual Verification
- Trigger a push notification (if possible) or check if the FCM token is successfully updated in Firestore.
- Verify that Google-related features (Auth, Firestore) continue to work as expected.
