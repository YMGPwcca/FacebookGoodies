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

## Messenger

| Version | Version code |
| --- | ---: |
| **584.1.0.61.98** | **346817021** |
| **585.0.0.53.94** | **347015062** |
| **586.0.0.55.92** | **347215273** |

Each Messenger (`com.facebook.orca`) build uses exact, build-specific bindings. Add Messenger to the module's LSPosed scope separately. Supported Messenger features are hiding the Meta AI floating button, suppressing read receipts, typing status and story views, and the local-read and manual-seen controls.

## Verification status

### Facebook 583 and 584

The 583 and 584 bindings were checked against their matching APKs' DEX. On Facebook 584, News Feed loaded with the module active, and the user confirmed ads were absent and video downloads worked. Threads cards appeared intermittently in Reels on an earlier build and have not been seen on 584, so that fix is not confirmed. Reels-specific behavior on 584 and all feature behavior on 583 still need on-device checks.

Static inspection of Facebook 584.0.0.51.56 / 476015532 found its one-argument Threads response binding. The installed Messenger 585.0.0.53.94 / 347015062 hook targets were matched against its DEX. Neither target has been live-verified with LSPosed.

### Facebook 585

The 585 bindings cover the existing Facebook features, including video downloads. They were checked against the target APK's DEX declarations and selected method bodies. Runtime behavior on 585 has not been verified.

### Facebook and Messenger 586

The 586 bindings keep the existing features and were checked against the installed APKs using DEX declarations and selected method bodies. Runtime feature behavior on these builds has not been verified.

The Facebook 586 video port uses `05U4`/`03En` for Reels pools and `06I0`/`06Hz` for Watch pools. Instream responses use `0RTj`, `0UUH` and `0UUA`, which keep their native null-result completion paths and PlayerOrigin source/subsource filtering. Reels completion calls the concrete `0ReI.A02(0ARi)` and `0ReJ.A02(0ARi)` receivers' native `A01()` no-ad handler instead of reusing the 585 callback owner. The metadata node is `04Ae`, limited to the existing media field and type IDs. Launch options use `096o.A01`; banner rendering uses `0RUD.A1E`; instream cards use `0TLE.A1E` and `0TLe.A1E` (case-sensitive); the affiliate attachment uses `0dWQ.render`; empty components use `035j`.

Checks on this port:

- 42 static declaration and semantic checks passed.
- The debug APK assembled, and 13 video policy and scoped-option tests passed.
- A disposable JVM smoke run drove the real response-filter and metadata callbacks with synthetic host input: unknown and null origins, Reels suborigins, cached-list preservation, disabled filtering, launch-time metadata and host exceptions.

None of this is an Android or Xposed installation check. Playback and UI behavior remain unverified.

## Facebook features

### Feed and ads

- Filters sponsored items in the classic feed insertion path, feed model lists, CSR cache inputs and News Feed rendering.
- Filters sponsored selections from the Reels, Watch and VideoHome ad pools. Organic video pools are unchanged.
- Completes Reels banner and post-loop ad responses through Facebook's own no-ad handler, and hides the dedicated banner component.
- Sends legacy, modern and extended video ad-break responses through their native empty-result completion paths for recognized Reels, News Feed and Watch player origins. Unknown origins and Story players are left unchanged.
- Filters the three verified embedded `InstreamVideoAdBreak` fields on media models without changing their cached originals or other video fields.
- Suppresses dedicated non-interruptive and context ad cards and the Reels affiliate attachment, including merchant product cards. Ordinary Reels stay playable.
- Disables in-content-ad and shopping eligibility and enables banner suppression while Reels launch options are built. Other launch options, host-video data and the native cached-break flag are kept.

Unknown categories and unsupported model shapes stay visible. Counts in the settings screen are interception actions, not unique posts.

### Content filters

The optional content filters default to off.

- **Hide Threads** uses the `PROMOTION` category that the reference mod uses in News Feed, so other units in that category may also be hidden. In Reels and videos it removes only the typed `THREADS_MIDCARD` promotion from incoming lists before a page is created; unknown or unreadable items stay visible.
- **Hide Reels** removes `FB_SHORTS` and `FB_SHORTS_FALLBACK` feed cards without hiding the Reels tab.
- **Hide Stories in the middle** removes `MULTI_FB_STORIES_TRAY` feed units and the inline Stories viewer.
- **Hide Stories at the top** skips both top-tray adapter builders and needs a Facebook restart when changed.

### Privacy

Read-receipt suppression can leave a chat locally unread. It is scoped to Facebook's in-app Messenger pipeline, and its recipient-side effect has not been tested with a second account. Screenshot-detection suppression covers the registered callbacks and the identified observer; it does not suppress every screen-recording API, bypass DRM, or force protected content to allow screenshots.

### Video color range

Facebook ships `skipSetDataSpaceForSDR = true`, so its renderer never calls `ANativeWindow_setBuffersDataSpace` for SDR and the 8-bit YV12 buffer keeps a full-range sRGB tag. Hardware overlays then expand 16-235 to 0-255 and the video looks washed out until a GPU-composited overlay forces the limited-to-full conversion shader.

**True video color range** clears that flag on the `exoplayer2.av1.src.Dav1dMediaCodecAdapterSetting` constructor, so Dav1d declares its SDR output as limited range. It covers every supported Facebook build, is off by default, and needs the video reopened after a change. Diagnostics counts each patched codec setting.

## Messenger features

- The read-receipt, typing and story-view switches cover the version-specific paths and default to on when Messenger is in the LSPosed scope. Suppressed story views may stay unread locally.
- Threads stay visually read on the device while automatic seen is blocked, including the inbox row and the in-chat unread divider. A small dot appears on the chat's emoji icon while a read receipt is pending; hold emoji to send it, and a short tap still opens emoji.
- Local read watermarks live in Messenger's private preferences under hashed thread keys. Message text is not stored.
- Updating the module keeps existing switch values.
- Diagnostics report loaded hook groups and intercepted read, typing and story signals. Counts are actions in the current process, not unique messages or viewers.

## Downloads

- **Download** sits above **More** in the native Reels and video action sidebar for the visible video in the `X.58I` player on 582, `X.5B3` on 583, `X.55R` on 584, `X.05gS` on 585 and `X.05iT` on 586.
- A tap snapshots that video's sources, takes the largest pixel resolution from its DASH manifest (bandwidth breaks ties), downloads the highest-bandwidth audio track and muxes the encoded samples into MP4 without re-encoding. Without an adaptive manifest it uses the player's HD URL, then the SD URL.
- The progress dialog keeps source-finding, transfer, merge, save and cleanup visible. Video and audio download in parallel, and each transfer row shows elapsed time, size and speed; a percentage appears only when the full transfer size is known. Signed source URLs are never displayed.
- Supported input is static single-period MP4 DASH with complete `SegmentBase` URLs, `SegmentList`, and finite `SegmentTimeline` templates using unformatted Number/Time placeholders. VP9 video in MP4 uses a lossless container merge when Android's native MP4 writer rejects the codec, so resolution and encoded samples are preserved. That parser mapping is bundled with the module for LSPosed class loading.
- Live, protected, WebM/HLS, multi-period and other unsupported manifest forms fail with a message. A lower resolution is never substituted for an unsupported highest source.
- Download metadata exists only for the active visible player. It is cleared when the player disappears, the activity pauses or is destroyed, or the control is disabled. A running download owns its immutable source snapshot until it finishes or is cancelled. There is no app-wide capture, persistent URL or ID cache, browser downloader, cookie export, or external resolver. Temporary files are removed after a job, and an interrupted job is cleaned up before the next download. Only numeric counters are reported.
- Signed media URLs can expire: play the video again and retry. Downloads run in Facebook's process, so Android can kill one mid-transfer.
- Android 11 and newer publish the file through MediaStore, so the module needs no storage permission. Older releases write the file directly and need Facebook's storage permission.

## Outside the feature set

Ads in the Stories viewer, Marketplace and games, other suggested-content categories, Messenger inbox ads, repost actions, account import/export, remote configuration, donations and updates are not handled.

A second account confirmed that automatic Messenger seen stayed hidden in the tested chat and that holding emoji sent seen there. Typing, story views, encrypted chats and notification-read paths still need their own recipient-side checks.
