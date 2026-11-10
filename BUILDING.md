# Building FacebookGoodies

## Requirements

- JDK 21
- Android SDK Platform 36
- Git, if you clone the repository

The Gradle Wrapper downloads and verifies Gradle 8.14.3. The first build also resolves Android Gradle Plugin 8.13.0 and the platform build dependencies from their configured repositories.

## Build a debug APK

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Linux or macOS:

```sh
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it as an LSPosed module, add Facebook, Messenger, or both to its scope, and restart each app. Both host apps stay separately installed.

## Run the local policy tests

```text
:app:testDebugUnitTest
```

The legacy Xposed API is a compile-only dependency: its classes are not bundled into FacebookGoodies and are not copied into the Facebook process.

## Check downloads on a disposable emulator

Build `:app:assembleDebugAndroidTest`, install both debug APKs, then run:

```text
adb shell am instrument -w ymg.pwcca.facebookgoodies.test/ymg.pwcca.facebookgoodies.DownloadInstrumentation
```

The platform-only runner checks attach-time application-context retention, manifest parsing, unchanged encoded video and audio samples after muxing, 1080p output, VP9 MP4 fallback with audio, module-loader mapping, silent-video handling, cancellation and MediaStore cleanup.

It creates and removes synthetic downloads in `Downloads/FacebookGoodies`, so use a disposable emulator. Fixtures ship only in test artifacts. The native Facebook sidebar and signed CDN downloads still need a check in a supported Facebook build on a device. The VP9 JVM test also covers fragmented MP4; the emulator's native reader rejects that generated fixture, so the end-to-end test uses a regular VP9 MP4.

## Publish a release

Push a commit to `main` that changes `app/build.gradle` and is named `chore(release): bump version to X.Y.Z`, with `versionName` set to that version and `versionCode` increased. The Release APK workflow finds each matching commit in the push, tests and builds that exact snapshot, signs the APK, then publishes `vX.Y.Z` with change notes and a SHA-256 checksum. Reruns keep published releases and their assets; a conflicting tag fails the run.

Signing uses the existing certificate through the Actions secrets `ANDROID_SIGNING_KEY_BASE64`, `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD`. Key files stay in runner temporary storage and are removed after the job. Workflow artifacts keep the APKs and checksums for 14 days.

Run the workflow manually against a main-branch commit or tag to check a build; publishing defaults to off. Release descriptions summarize changes without commit timestamps or links. CI uses a narrowly scoped PrivateApiDetector workaround for the existing obfuscated-name lint crash.

Run the release-planning tests locally:

```sh
python -m unittest discover -s ci/tests -v
```
