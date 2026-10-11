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

## Automatic release APKs

Push a commit named `chore(release): bump version to X.Y.Z` to `main`, changing `versionName` to that version and increasing `versionCode` in `app/build.gradle`. The Release APK workflow detects each matching commit in a push, tests and builds its exact snapshot, signs the APK, then publishes `vX.Y.Z` with change notes and a SHA-256 checksum. Published releases and their assets are preserved on reruns; conflicting tags fail the run.

Signing uses the existing certificate through the Actions secrets `ANDROID_SIGNING_KEY_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, and `ANDROID_KEY_PASSWORD`. Key files stay in runner temporary storage and are removed after the job. APKs and checksums are retained as workflow artifacts for 14 days.

The workflow can also be run manually against a main-branch commit or tag. Publishing defaults to off, allowing a build check without creating a release. Release descriptions summarize changes without commit timestamps or links. CI uses a narrowly scoped PrivateApiDetector workaround for the existing obfuscated-name lint crash.

Run the release-planning tests locally with `python -m unittest discover -s ci/tests -v`.
