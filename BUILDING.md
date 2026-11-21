# Building FacebookGoodies

## Requirements

- JDK 21
- Android SDK Platform 36
- Git, if you clone the repository

The Gradle Wrapper downloads and verifies Gradle 8.14.3. The first build also resolves Android Gradle Plugin 8.13.0 and the platform build dependencies from their configured repositories.

## Build a debug APK

Windows PowerShell:

```powershell
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
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

It creates and removes synthetic downloads in `Downloads/FacebookGoodies`, so use a disposable emulator. Fixtures ship only in test artifacts. Add `-e mappingOnly true` after `-w` to check the provider's MP4 mapping and verify that the portable muxer preserves audio and video samples for both fragmented and unfragmented VP9 input.

## Confirm hook startup on a device

Install the signed release with `adb install -r <path-to-release.apk>`, then restart Facebook and Messenger. To inspect the module's reports over ADB, run:

```sh
adb shell content call --uri content://ymg.pwcca.facebookgoodies.settings --method status --arg facebook
adb shell content call --uri content://ymg.pwcca.facebookgoodies.settings --method status --arg messenger
```

Check that the report updated after the restart. On v1.5.2, Facebook 588.0.0.42.55 reported 46 hooks and Messenger 588.1.0.66.98 reported 37, with zero errors in both reports. The Messenger count includes the Meta AI button hook when **Hide Meta AI button** is enabled at startup.

## Publish a release

Push a commit to `main` that changes `app/build.gradle` and is named `chore(release): bump version to X.Y.Z`, with `versionName` set to that version and `versionCode` increased. The Release APK workflow finds each matching commit in the push, tests and builds that exact snapshot, signs the APK, then publishes `vX.Y.Z` with change notes and a SHA-256 checksum. Reruns keep published releases and their assets; a conflicting tag fails the run.

Signing uses the existing certificate through the Actions secrets `ANDROID_SIGNING_KEY_BASE64`, `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD`. Key files stay in runner temporary storage and are removed after the job. Workflow artifacts keep the APKs and checksums for 14 days.

Run the workflow manually against a main-branch commit or tag to check a build; publishing defaults to off. Release descriptions summarize changes without commit timestamps or links. CI uses a narrowly scoped PrivateApiDetector workaround for the existing obfuscated-name lint crash.

Run the release-planning tests locally:

```sh
python -m unittest discover -s ci/tests -v
```
