# Supported Facebook build

Current hooks target Facebook for Android `com.facebook.katana`, version **582.0.0.50.54**, version code **475417129**. The module checks the installed package and version and leaves the Facebook hooks disabled when the host does not match. Its LSPosed scope targets Facebook only.

## Implemented features

- Filters sponsored items in the classic feed insertion path, feed model lists, CSR cache inputs, and News Feed rendering.
- Filters sponsored selections from the Reels, Watch and VideoHome ad pools. Organic video pools remain unchanged.
- Completes Reels banner/post-loop ad responses through Facebook's own no-ad handler and hides the dedicated banner component.
- Sends legacy, modern and extended video ad-break responses through their native empty-result completion paths for recognized Reels, News Feed and Watch player origins. These response hooks leave unknown origins and Story players unchanged.
- Filters the three verified embedded `InstreamVideoAdBreak` fields on media models, without changing their cached originals or other video fields.
- Suppresses dedicated non-interruptive/context ad cards and the Reels affiliate attachment, including merchant product cards. Ordinary Reels remain playable.
- Disables in-content-ad and shopping eligibility and enables banner suppression while Reels launch options are built. Other launch options, host-video data and the native cached-break flag are retained.
- Hides Facebook suggestion and people-you-may-know categories. Ordinary posts and friends' shares are preserved.
- Adds independent switches for Threads recommendations in News Feed and between Reels/videos, the top Stories tray, Stories between posts, and Reels cards in News Feed.
- Adds switches to suppress the in-app Messenger mark-thread-read call and supported screenshot-detection callbacks.
- Adds Download immediately above More in the native Reels/video action sidebar, for the current visible video in the verified `X.58I` player. It snapshots that video's sources on tap, selects the largest pixel resolution from its DASH manifest (bandwidth breaks ties), downloads the highest-bandwidth audio track, and muxes encoded samples into MP4 without re-encoding. When no adaptive manifest exists, it uses the player's HD URL, then SD URL.

Unknown categories and unsupported model shapes remain visible. Counts in the settings screen are interception actions, not unique posts.

The optional content filters default to off. In News Feed, the Threads filter uses the `PROMOTION` category used by the reference mod, so other units in that category may also be hidden. In Reels/videos, it removes only the typed `THREADS_MIDCARD` promotion from incoming lists before a page is created; unknown or unreadable items remain visible. The Reels filter removes `FB_SHORTS` and `FB_SHORTS_FALLBACK` feed cards without hiding the Reels tab. The middle Stories filter removes `MULTI_FB_STORIES_TRAY` feed units and the inline Stories viewer. The top Stories filter separately skips both top-tray adapter builders and requires a Facebook restart when changed.

Read-receipt suppression can leave a chat locally unread. It is scoped to Facebook's in-app Messenger pipeline, and its recipient-side effect has not been tested with a second account. Screenshot-detection suppression covers its registered callbacks and the identified observer; it does not suppress every screen-recording API, bypass DRM, or force protected content to allow screenshots.

Download metadata is held only for the active visible player and cleared on player disappearance, activity pause/destruction, or disabling the control. An explicit download owns its immutable source snapshot until it finishes or is cancelled. There is no app-wide capture, persistent URL/ID cache, browser downloader, cookie export, or external resolver. Temporary media files are removed after a job, and interrupted-job files are cleaned before the next download. Only numeric download counters are reported.

The progress dialog keeps source-finding, transfer, merge, save and cleanup stages visible. Separate video and audio files download in parallel. Transfer rows show elapsed time, size and speed; percentages appear only when the full transfer size is known. Signed source URLs are never displayed.

Downloads support static single-period MP4 DASH with complete `SegmentBase` URLs, `SegmentList`, and finite `SegmentTimeline` templates using unformatted Number/Time placeholders. VP9 video in MP4 uses a lossless container merge when Android's native MP4 writer rejects that codec; the video resolution and encoded audio/video samples are preserved. Its parser mapping is bundled with the module for LSPosed class loading. Live, protected, WebM/HLS, multi-period and other unsupported manifest forms fail with a message; a supported lower resolution is not silently substituted for an unsupported highest source. Signed media URLs can expire: play the video again and retry. Downloads run in Facebook's process and can be interrupted if Android kills it. Android 10+ uses pending MediaStore entries; Android 9 needs Facebook's existing storage permission. Native device checks and actual Facebook feed/Reels download acceptance are tracked separately in the local analysis report.

Ads inside the Stories viewer, Marketplace and games, other suggested-content categories, the separate Messenger app, repost actions, account import/export, remote configuration, donations, and updates are outside this project.
