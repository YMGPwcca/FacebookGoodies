# Building FacebookGoodies

## Requirements

- JDK 21
- Android SDK Platform 36
- Git (if cloning this repository)

The Gradle Wrapper downloads and verifies Gradle 8.14.3. Android Gradle Plugin 8.13.0 and platform build dependencies are resolved from their configured repositories on first build.

## Build a debug APK

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Linux or macOS:

```sh
./gradlew :app:assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Install this APK as an LSPosed module, enable its Facebook scope, and restart Facebook. Facebook itself remains a separate installed application.

## Run the local policy tests

```text
:app:testDebugUnitTest
```

The project keeps the legacy Xposed API as a compile-only dependency: API classes are not bundled into FacebookGoodies or copied into the Facebook process.
