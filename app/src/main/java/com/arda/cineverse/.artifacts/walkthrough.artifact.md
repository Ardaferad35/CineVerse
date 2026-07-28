# Walkthrough - Fixing 'onProfileClick' Parameter Error

I have resolved the compilation error where the `onProfileClick` parameter was not found in the `SearchScreen` composable call within `CineVerseNavGraph.kt`.

## Changes

### [SearchScreen.kt](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/java/com/arda/cineverse/ui/screens/SearchScreen.kt)

- Added `onProfileClick: () -> Unit = {}` parameter to the `SearchScreen` composable function.
- Passed the `onProfileClick` callback to the `HomeTopBar` component inside the `SearchScreen`.

## Verification Results

### Automated Tests
- Successfully ran `./gradlew :app:compileDebugKotlin` to verify that the project builds without errors.

```
$ ./gradlew :app:compileDebugKotlin
BUILD SUCCESSFUL in 12s
```
