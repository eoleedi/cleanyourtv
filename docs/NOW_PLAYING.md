# Now Playing on Home

A compact panel under the header shows the selected media session's title, artist
and cover art. It offers Play/Pause and Open App, and disappears when no eligible
session exists. An active paused session stays visible so playback can be resumed.

## Permission and privacy

**Notification access is opt-in and has not been enabled by installation.** Android
describes this permission broadly: a notification-listener app can potentially read
notifications. This implementation does not inspect notifications; it uses the grant
only to obtain Android media-session access. It does not store/transmit metadata,
read notification contents or fetch artwork URLs. Displayed art comes from a session
bitmap (scaled to at most256px); missing art falls back to the application's icon.

After the owner explicitly agrees, open Android's Notification Access settings and
enable **Home — Now Playing access** (Traditional Chinese: **主畫面 — 播放資訊存取權**).
Where supported, the settings action is:

```sh
adb -s TV_IP:5555 shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
```

Some TV firmware lacks this page. Any ADB-based grant must be separately approved,
must preserve the existing colon-separated `enabled_notification_listeners` value,
and must add only `com.gathin.tvhome/.MediaAccessService`. Never overwrite other
listeners or grant signature-only MEDIA_CONTENT_CONTROL as a shortcut.

To undo, disable ONLY this listener in Notification Access settings. The panel hides;
the launcher, dock and other listeners continue working. Revoked/missing access is
not an error dialog or a repeated permission prompt.

## Supported playback

Only sessions exposed by Android apps are available. Spotify and AirPlay Server
(`io.github.jqssun.airplay`) both registered sessions on this Sony during inspection;
that alone does not prove they publish metadata/actions during actual playback.
An app may omit a title, artist, artwork or transport actions. HDMI input pictures
and remote-device playback cannot be synthesized into a media session by Home.

Playing/buffering/connecting sessions take precedence over paused ones. Equal-priority
sessions keep the current choice rather than flickering between apps. Stopped/error/
destroyed sessions hide or switch to another eligible session. Play/Pause is disabled
when the app doesn't advertise that action. Open App uses the session's activity or
its TV app launch intent. No global media key is sent to another player.

## Remote navigation

When visible: dock Up → media controls → Up → top bar. Down reverses the path.
Left/Right move within the two controls and stop at row ends. If Play/Pause is not
supported, Open App is the only focus target. A disappearing focused panel returns
to a dock tile; updates never steal focus from another control. When hidden, existing
launcher navigation is unchanged.

## Performance / build

Observation uses active-session/controller callbacks only while Home is visible.
Callbacks and artwork references are released on stop. No polling, progress timer,
blur, video or network requests have been added. The implementation retains API24
compatibility and the user's YouTube/Netflix/Spotify/AirPlay dock configuration.

```sh
cd launcher
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Use the side-by-side/update instructions in
[the Sony device note](devices/sony-kd-55x8500d-android7.md), not the takeover script,
to update the already-installed launcher. Capture and inspect actual screenshots of
idle, playing and paused states after explicit permission approval and user-started
playback. Empty captures or a lost ADB connection leave verification pending.

## Current verification status

Installed on the actual API24 Sony after verified APK transfer checksums. Local lint
and builds pass; 139 JVM tests and 208 Python tests pass (one skipped). All8 device
instrumentation tests passed, including real-session transport dispatch/destruction,
metadata/art downsampling, denied-access handling and default/Hebrew panel geometry.
Screenshot28 confirms Home and the chosen dock remain intact with access disabled.

The owner subsequently approved notification access. Only Home's listener was added
(the prior enabled-listener value was empty), and dumpsys confirmed it live/bound for
user0 with one media-session listener. Screenshot29 verifies idle hiding after the
grant. OS-level revocation and real Spotify/AirPlay metadata/control behavior and
active-panel screenshots still await user-started playback/testing.
