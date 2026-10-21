# Now Playing panel — approved functional design

User approved: show title, artist and cover art on Home; offer Play/Pause and Open App;
hide when there is no current media; use event-driven Android media sessions, no video
or network polling. Ask BEFORE enabling cross-app notification access.

## Implementation calls (not user preferences)
- A compact dark rounded panel below the clock/header, above the dock area. Keep the
  existing four-app dock/order and input/Wi-Fi/Settings buttons unchanged. Adapt both
  default and Hebrew layouts; ellipsize long metadata, never overlap dock or header.
- Show an active playing/buffering/connecting session first; an active paused session
  may remain visible so Resume is useful. Hide stopped, destroyed, null-state sessions.
  Ties retain the selected eligible session to prevent flicker; otherwise use session order.
- Missing title falls back to app name; missing artist uses app name; absent artwork
  uses app icon. Use supplied metadata bitmaps only, bounded to 256px; do not download
  artwork URLs or decode untrusted URIs. No progress timer, seek bar or track skipping.
- Disable Play/Pause when the session doesn't advertise the corresponding action.
  Open App uses its session PendingIntent, then a leanback launch-intent fallback.
  Never dispatch global media keys to a different app.
- Deterministic D-pad: Up from dock enters the two media controls when visible, Up
  from those goes to the existing top bar; Down reverses that path. Without the panel,
  preserve existing navigation. Focused control disappearing returns focus to a dock
  tile without stealing focus from elsewhere. Clamp row ends.
- Use an opt-in NotificationListenerService solely as authorization for
  MediaSessionManager.getActiveSessions. Do not inspect, store, transmit or log
  notification contents. Service reconnect/disconnect refreshes the visible UI;
  no-access/revoked access hides the panel safely. No automatic access grants.
- Register active-session and selected-controller callbacks only while Home is
  visible; unregister and release artwork references when stopped. Catch stale-session,
  canceled PendingIntent and SecurityException paths without crashing Home.

## Constraints
Java, Android minSdk24/target34; preserve existing desugaring/font compatibility.
No blur, video, network polling, telemetry, additional debloat or router changes.
Work in current checkout, preserve all prior uncommitted changes; no commits requested.
Wi-Fi investigation and final HOME-takeover verification remain unresolved. Do not
claim stability or completion based on a single successful ADB connection.

## Verification and privacy gate
Actual TV read-only dumpsys confirmed Spotify and io.github.jqssun.airplay sessions,
both inactive at inspection. It does not establish live metadata/transport behavior.
Use JVM tests for selection and device tests for view/focus/callback behavior.
Before notification access is enabled, explain that Android grants broad notification
visibility even though this implementation only uses session access, and get explicit
user consent. Preserve all existing authorized notification listeners.
After consent, ask user to start Spotify/AirPlay themselves, inspect screenshots,
exercise Play/Pause and Open App, and verify idle/no-permission behavior. No purchases,
sign-ins or personal playback choices without the user. If ADB drops, save pending state.
