# Supported Facebook build

Current hooks target Facebook for Android `com.facebook.katana`, version **582.0.0.50.54**, version code **475417129**. The module checks the installed package and version and leaves the Facebook hooks disabled when the host does not match. Its LSPosed scope targets Facebook only.

## Implemented features

- Filters sponsored items in the classic feed insertion path, feed model lists, CSR cache inputs, and News Feed rendering.
- Filters sponsored selections from the Reels, Watch and VideoHome ad pools. Organic video pools remain unchanged.
- Completes Reels banner/post-loop ad responses through Facebook's own no-ad handler and hides the dedicated banner component.
- Sends video ad-break responses through their native empty-result completion path for recognized Reels, News Feed and Watch player origins. Unknown origins and Story players remain unchanged.
- Hides Facebook suggestion and people-you-may-know categories. Ordinary posts and friends' shares are preserved.
- Adds independent switches for Threads recommendations, the top Stories tray, Stories between posts, and Reels cards in News Feed.
- Adds switches to suppress the in-app Messenger mark-thread-read call and supported screenshot-detection callbacks.

Unknown categories and unsupported model shapes remain visible. Counts in the settings screen are interception actions, not unique posts.

The optional content filters default to off. The Threads filter uses the `PROMOTION` category used by the reference mod; a Threads-only marker has not been identified, so other units in that category may also be hidden. The Reels filter removes `FB_SHORTS` and `FB_SHORTS_FALLBACK` feed cards without hiding the Reels tab. The middle Stories filter removes `MULTI_FB_STORIES_TRAY` feed units and the inline Stories viewer. The top Stories filter separately skips both top-tray adapter builders and requires a Facebook restart when changed.

Read-receipt suppression can leave a chat locally unread. It is scoped to Facebook's in-app Messenger pipeline, and its recipient-side effect has not been tested with a second account. Screenshot-detection suppression covers its registered callbacks and the identified observer; it does not suppress every screen-recording API, bypass DRM, or force protected content to allow screenshots.

Ads inside the Stories viewer, Marketplace and games, other suggested-content categories, the separate Messenger app, download and repost actions, account import/export, remote configuration, Shopee promotions, donations, and updates are outside this project.
