# FacebookGoodies

An LSPosed module for a cleaner Facebook feed.

## Features

- Hide sponsored posts and videos in News Feed, Reels and Video feeds.
- Suppress supported video ad breaks and Reels banners.
- Hide affiliate product cards over Reels.
- Hide suggested posts and People You May Know.
- Hide Threads recommendations in News Feed and between Reels/videos.
- Hide Reels cards in News Feed.
- Hide Stories at the top or between posts, with separate switches.
- Hide read receipts in Facebook chats.
- Block supported screenshot-detection signals.
- Download the current video at the highest available resolution, with audio.

Friends’ shares stay visible. Read-receipt filtering applies to chats inside Facebook and may leave chats unread.

The content filters are off by default. Restart Facebook after changing the top Stories setting; refresh the feed for the other filters.

While a video is playing, tap Download above More in the player's action sidebar. Videos save to `Downloads/FacebookGoodies`. Only the current video is selected; there is no video history. The progress dialog shows source finding, video and audio transfers, merging and saving, with elapsed time and transfer speed. Downloads can be cancelled from the dialog.

## Requirements

Android 11 or newer, LSPosed, and Facebook **582.0.0.50.54**, **583.0.0.39.55**, or **584.0.0.51.56** with a [matching build code](SUPPORT.md).

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
