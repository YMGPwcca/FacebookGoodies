# FacebookGoodies

An LSPosed module for Facebook feed controls and Messenger privacy controls.

## Features

- Hide sponsored posts and videos in News Feed, Reels and Video feeds.
- Suppress supported video ad breaks and Reels banners.
- Hide affiliate product cards over Reels.
- Hide suggested posts and People You May Know.
- Hide Threads recommendations in News Feed and between Reels/videos.
- Hide Reels cards in News Feed.
- Hide Stories at the top or between posts, with separate switches.
- Hide read receipts in Facebook chats.
- Hide the Meta AI floating button in Messenger.
- Suppress Messenger read receipts, typing status and story views on the supported build.
- Keep Messenger chats locally read while automatic seen is suppressed; a small dot on emoji marks a pending receipt, and holding emoji sends seen manually.
- Block supported screenshot-detection signals.
- Download the current video at the highest available resolution, with audio.

Friends’ shares stay visible. Facebook chat read-receipt filtering can leave chats unread. Suppressed Messenger story views can stay unread locally.

The content filters are off by default. Restart Facebook after changing the top Stories setting; refresh the feed for the other filters.

While a video is playing, tap Download above More in the player's action sidebar. Videos save to `Downloads/FacebookGoodies`. Only the current video is selected; there is no video history. The progress dialog shows source finding, video and audio transfers, merging and saving, with elapsed time and transfer speed. Downloads can be cancelled from the dialog.

## Requirements

Android 11 or newer and LSPosed. Facebook features require Facebook **582.0.0.50.54**, **583.0.0.39.55**, or **584.0.0.51.56**; Messenger features require Messenger **584.1.0.61.98**. Both check their [matching build codes](SUPPORT.md).

## Setup

1. Install the FacebookGoodies APK.
2. Enable it in LSPosed and select **Facebook** and/or **Messenger** as its scope.
3. Restart each selected app.
4. Open FacebookGoodies and use the Facebook or Messenger tab for that app's controls and diagnostics.

## Build

```sh
./gradlew :app:assembleDebug
```

The APK is saved to `app/build/outputs/apk/debug/app-debug.apk`. See [BUILDING.md](BUILDING.md) for requirements and Windows instructions.

## License

[Apache-2.0](LICENSE).
