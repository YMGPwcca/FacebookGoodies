# FacebookGoodies

An LSPosed module with feed filters, video downloads and privacy switches for the Facebook Android app, plus privacy switches for Messenger.

## Requirements

- Android 11 or newer
- LSPosed
- A supported Facebook or Messenger build, listed in [Supported host builds](SUPPORT.md)

The module checks the installed version name and version code against that list. An unlisted build runs with its hooks disabled, and its tab reports **Unsupported**. New features from 1.5.0 target only the newest supported builds (Facebook 586 and Messenger 586).

## Setup

1. Install the FacebookGoodies APK.
2. In LSPosed, enable FacebookGoodies and add Facebook, Messenger, or both to its scope.
3. Restart each app you added.
4. Open FacebookGoodies and use the Facebook or Messenger tab for that app's switches and diagnostics.

## Facebook

### Feed

- **Hide ads** filters sponsored posts and videos from News Feed, Reels and video feeds, including supported in-video ad breaks, Reels banners and affiliate product cards.
- **Hide suggested posts** removes suggested posts and People You May Know. Friends' shares stay visible.
- **Feed content** switches remove Threads recommendations, Reels cards, the Stories tray and Stories cards between posts. All four are off by default.

### Privacy

- **Hide read receipts** stops chats inside Facebook from sending read receipts. Chats may stay unread.
- **Block screenshot detection** blocks the supported screenshot-detection signals.

### Downloads

- **Download current video** adds **Download** above **More** in the player's action sidebar. The video and its audio are saved as one MP4 in `Downloads/FacebookGoodies`. Only the video on screen is offered; the progress dialog shows finding, transfer, merging and saving, and can cancel the download.

### Video

- **True video color range** tags 8-bit SDR video as limited range so hardware overlays stop washing the picture out. It is off by default. Reopen the video after changing it.

## Messenger

- **Hide read receipts** stops Messenger from sending read receipts. A dot on the emoji button marks a pending receipt, and holding the emoji button sends seen.
- **Mark chats read locally** keeps the chat list read on this device without sending a receipt. It is on by default.
- **Show message status icons** marks incoming messages that still wait for a read receipt, and incoming messages the sender unsent. It is on by default.
- **Hide typing status** stops Messenger from sending typing activity.
- **Hide story views** stops Messenger from marking stories seen. Stories may stay unread.
- **Hide Meta AI button** removes the floating Meta AI button. It is on by default; restart Messenger after changing it.
- **Hide active status** stops Messenger from reporting that you are active. Friends may stop showing as active too, as when Active Status is off.
- **Hide inbox ads** removes sponsored conversations from the chat list.
- **Per-chat read receipts** adds a **Read receipts: On/Off** item to the long-press menu of a conversation. It is available while **Hide read receipts** is on and lets that chat send seen receipts.
- **Hide AI summary pill** removes the **Summarize N unread messages** pill in chats.

## Settings behavior

- Feed filters apply after a feed refresh. The top Stories switch needs a Facebook restart.
- Each tab has a restart button for its host app. Restarting from the module requires root access.
- Diagnostics counts actions in the current app process, so the numbers reset when that app restarts and do not count unique posts or messages.

## Build

```sh
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. See [Build instructions](BUILDING.md) for requirements, Windows commands and the local tests.

## License

[Apache-2.0](LICENSE).
