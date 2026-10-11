# FacebookGoodies

An LSPosed module for a cleaner Facebook feed.

## Features

- Hide sponsored posts and videos in News Feed, Reels and Video feeds.
- Suppress supported video ad breaks and Reels banners.
- Hide affiliate product cards over Reels.
- Hide suggested posts and People You May Know.
- Hide Threads recommendations and Reels cards in News Feed.
- Hide Stories at the top or between posts, with separate switches.
- Hide read receipts in Facebook chats.
- Block supported screenshot-detection signals.

Friends’ shares stay visible. Read-receipt filtering applies to chats inside Facebook and may leave chats unread.

The content filters are off by default. Restart Facebook after changing the top Stories setting; refresh the feed for the other filters.

## Requirements

Android 9 or newer, LSPosed, and Facebook **582.0.0.50.54**. See [compatibility details](SUPPORT.md) for the matching build.

## Setup

1. Install the FacebookGoodies APK.
2. Enable it in LSPosed and select **Facebook** as its scope.
3. Restart Facebook.
4. Open FacebookGoodies to choose your settings.

## Build

```sh
./gradlew :app:assembleDebug
```

The APK is saved to `app/build/outputs/apk/debug/app-debug.apk`. See [BUILDING.md](BUILDING.md) for requirements and Windows instructions.

## License

[Apache-2.0](LICENSE).
