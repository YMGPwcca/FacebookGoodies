# FacebookGoodies

FacebookGoodies is an LSPosed module for Facebook on Android. It filters sponsored and suggested News Feed items inside Facebook and provides selected privacy controls. Facebook remains a separate installed application.

## Features

- Remove sponsored News Feed units from classic feeds, cached results, CSR caches, and feed rendering.
- Hide Facebook's “suggested” and “People You May Know” feed categories while keeping ordinary posts and friends' shares.
- Suppress Facebook in-app Messenger's thread-read call. Chats can remain locally unread; the separate Messenger app is outside this module's scope.
- Suppress supported Facebook screenshot-detection callbacks. This does not bypass DRM or every screen-recording API.

The feed filter keeps unknown models. Its counters report filter actions, not unique posts. Other ad surfaces and supported-build details are listed in [SUPPORT.md](SUPPORT.md).

## Compatibility

The current hooks target Facebook `com.facebook.katana` **582.0.0.50.54 (475417129)**. The module checks the installed package and build, then leaves its hooks disabled on a mismatch. Enable FacebookGoodies for Facebook in LSPosed.

The module's package ID is **`ymg.pwcca.facebookgoodies`**. Installing or updating FacebookGoodies does not patch or replace Facebook. Gradle's debug build is suitable for local testing. For a release, sign the module APK with your own key and keep that key for future updates.

## Build

Requirements, wrapper details and complete Windows, Linux and macOS instructions are in [BUILDING.md](BUILDING.md). The short command is:

```sh
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install that module APK, enable its Facebook scope in LSPosed, and restart Facebook. Its settings app controls four switches and reports aggregate hook status.

## License

FacebookGoodies is licensed under Apache-2.0. See [LICENSE](LICENSE) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
