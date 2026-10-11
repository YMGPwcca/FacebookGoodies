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

## Download media checks on a disposable Android emulator

Build `:app:assembleDebugAndroidTest`, install both debug APKs, then run:

```text
adb shell am instrument -w ymg.pwcca.facebookgoodies.test/ymg.pwcca.facebookgoodies.DownloadInstrumentation
```

The platform-only runner verifies attach-time application-context retention, manifest parsing, unchanged encoded video/audio samples after muxing, 1080p output, VP9 MP4 fallback with audio, module-loader mapping, silent-video handling, cancellation and MediaStore cleanup. It creates and removes synthetic downloads in `Downloads/FacebookGoodies`, so run it on a disposable emulator. Fixtures are included only in test artifacts. The native Facebook sidebar and signed CDN downloads require verification in the supported Facebook build on a device. The VP9 JVM test also covers fragmented MP4; the disposable emulator's native reader rejects that generated fragmented VP9 fixture, so its end-to-end test uses a regular VP9 MP4.
