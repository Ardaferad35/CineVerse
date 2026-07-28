# Implementation Plan - Fix Trailer Playback (White Screen in WebView)

The trailer player in `MovieDetailScreen.kt` is showing a white empty box instead of playing the YouTube video. This is typically caused by software rendering mode being active for video content or hardware acceleration issues within the WebView.

## Proposed Changes

### [Component Name] UI Screen - Movie Detail

#### [MODIFY] [MovieDetailScreen.kt](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/java/com/arda/cineverse/ui/screens/MovieDetailScreen.kt)
- **Switch to Hardware Rendering:** Change `setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)` to `setLayerType(WebView.LAYER_TYPE_HARDWARE, null)`. Software rendering often fails to render video streams correctly in modern Android WebViews.
- **Transparent Background:** Set `setBackgroundColor(Color.TRANSPARENT)` on the WebView. This prevents the initial white flicker/box before the video content loads.
- **User Agent Update:** Set a mobile-specific User-Agent. YouTube sometimes restricts playback or shows a "unsupported browser" state if the default WebView User-Agent is used.
- **Enhanced Settings:** Ensure `domStorageEnabled` and `javaScriptEnabled` are correctly configured (already present but will verify placement).

## Verification Plan

### Automated Tests
- Build and run the app.
- Monitor Logcat for `CVTrailerWebView` to ensure the page loads without JS errors.

### Manual Verification
- Open a movie detail screen (e.g., "Supergirl" or "İfşa Günü").
- Click the "Fragmanı İzle" button.
- Verify that the YouTube player appears and starts playback instead of showing a white box.
- Verify that the "YouTube uygulamasında aç" link still works.
