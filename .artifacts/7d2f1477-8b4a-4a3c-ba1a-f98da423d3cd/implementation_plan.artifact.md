# Fix for "Failed to get service from broker" SecurityException

The application is experiencing a `java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'` when interacting with Google/Firebase services. This error typically occurs due to package visibility restrictions introduced in newer Android versions or due to using an experimental `targetSdk`.

## Proposed Changes

### [Component Name] Android Manifest and Build Configuration

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/AndroidManifest.xml)
- Add a `<queries>` block to explicitly declare visibility for the Google Play Services package (`com.google.android.gms`). This helps the system resolve the service broker connection correctly.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/build.gradle.kts)
- Downgrade `compileSdk` and `targetSdk` from `36` (Android 16 preview) to `35` (Android 15 stable). Using a preview SDK can lead to unexpected security exceptions if the underlying Google Play Services on the device/emulator are not yet fully compatible with the new security models of that SDK.

## Verification Plan

### Automated Tests
- Run the application and attempt to perform an action that triggers Firebase (e.g., loading the Login screen or trying to log in).
- Monitor Logcat for the `GoogleApiManager` tag to ensure the "Failed to get service from broker" error no longer appears.

### Manual Verification
- Verify that Firebase Auth and Firestore operations work correctly without crashing.
