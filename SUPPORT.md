# Supported host builds

FacebookGoodies checks each host app's installed version name and version code, then installs that build's hook bindings. An unlisted build keeps its hooks disabled, and its tab reports **Unsupported**.

## Facebook

These hooks target Facebook for Android (`com.facebook.katana`):

| Version | Version code | Minimum Android |
| --- | ---: | --- |
| **582.0.0.50.54** | **475417129** | Android 11 |
| **583.0.0.39.55** | **475614366** | Android 11 |
| **584.0.0.51.56** | **475817510, 476015532** | Android 11 |
| **585.0.0.47.54** | **476015694** | Android 11 |
| **586.0.0.52.53** | **476217123** | Android 11 |
| **587.0.0.39.55** | **707414483** | Android 11 |
| **588.0.0.42.55** | **707615136** | Android 11 |

## Messenger

| Version | Version code | Minimum Android |
| --- | ---: | --- |
| **584.1.0.61.98** | **346817021** | Android 9 |
| **585.0.0.53.94** | **347015062** | Android 9 |
| **586.0.0.55.92** | **347215273, 347215343** | Android 9 |
| **588.0.0.20.98** | **347610371** | Android 9 |
| **588.1.0.66.98** | **347617585** | Android 9 |

Add Messenger to the module's LSPosed scope separately.

## Feature targeting

Starting with 1.5.0, new features target the newest host builds. Older supported builds retain their existing features. New work is not backported to Facebook 582–585 or Messenger 584–585.

## Facebook features

| Feature | Default | What it does |
| --- | --- | --- |
| **Hide ads** | On | Filters sponsored posts and ads in News Feed, Reels, and video feeds, including supported ad breaks, Reels banners, and affiliate product cards. |
| **Hide suggested posts** | On | Hides suggested posts and People You May Know. Friends' shares stay visible. |
| **Hide Threads** | Off | Hides Threads recommendations in News Feed and between videos. It may also hide other promotions in News Feed. |
| **Hide Reels** | Off | Hides Reels cards in News Feed. The Reels tab remains available. |
| **Hide Stories at the top** | Off | Hides the top Stories tray. Restart Facebook after changing it. |
| **Hide Stories in the middle** | Off | Hides Stories trays between News Feed posts. |
| **Hide read receipts** | On | Blocks read receipts for chats inside Facebook. These chats may stay unread. |
| **Block screenshot detection** | On | Blocks supported signals Facebook uses to detect screenshots. Android's screenshot restrictions still apply. |
| **True video color range** | Off | Corrects washed-out 8-bit SDR video. Reopen the video after changing it. |
| **Download current video** | On | Tap **Download** above **More** in the video sidebar. Saves the video on screen with its audio as an MP4 in `Downloads/FacebookGoodies`. Uses the highest available resolution on supported sources without re-encoding. Cancel from the progress dialog. Live streams and protected videos are unsupported. |

## Messenger features

| Feature | Default | What it does |
| --- | --- | --- |
| **Hide read receipts** | On | Blocks automatic seen receipts. To send one manually, press and hold the composer button with the pending receipt dot. |
| **Mark chats read locally** | On | Marks chats read on this device without sending a receipt. Restart Messenger after changing it. |
| **Show message status icons** | On | Shows a crossed-out eye next to incoming messages with a pending receipt, including messages the sender unsent. |
| **Hide typing status** | On | Hides your typing activity from other people. |
| **Hide story views** | On | Keeps your name off the Story viewer list. Stories may stay unread on your device. |
| **Hide active status** | On | Hides your online status. Friends' active status may also disappear. |
| **Hide inbox ads** | On | Removes sponsored conversations from the chat list. |
| **Per-chat read receipts** | Off | Long-press a conversation and choose **Read receipts: On/Off** to let that chat send seen while **Hide read receipts** is on. |
| **Hide Meta AI button** | On | Removes the floating Meta AI button. Restart Messenger after changing it. |
| **Hide AI summary pill** | On | Removes the **Summarize unread messages** prompt in chats. |

## Outside the feature set

Ads in the Stories viewer, Marketplace and games, other suggested-content categories, repost actions, account import/export, remote configuration, donations and updates are outside the module's feature set.
