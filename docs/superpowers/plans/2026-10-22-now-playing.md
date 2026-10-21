# Now Playing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show current media metadata and Play/Pause/Open App controls on Home on the real Android7 Sony TV.

**Architecture:** Separate pure session selection from Android session callbacks and view rendering. A minimal opt-in notification listener enables cross-app session access; Home observes media only while visible, with no polling.

**Tech Stack:** Java17, Android MediaSessionManager/MediaController, NotificationListenerService, JUnit, existing device instrumentation and ADB.

**Spec:** `docs/superpowers/specs/2026-10-22-now-playing-design.md`.

## Global Constraints
- minSdk24/target34; retain desugaring, LegacyFonts and all existing uncommitted work.
- Title/artist/cover art, Play/Pause, Open App; hidden when no eligible session.
- No video, blur, progress timer, network polling or artwork-URL fetching.
- Explicit user approval before notification access; preserve existing listener grants.
- Current checkout, no commits, additional debloat or router changes.

## Review Focus
- Stopped/destroyed sessions must not leave stale artwork or control another app.
- Permission denial/revocation must hide safely, not crash Home or prompt repeatedly.
- Missing metadata/artwork and canceled app intents must degrade gracefully.
- Panel appearance/disappearance must not trap D-pad focus or break Hebrew layouts.
- Background Home must release callbacks/artwork and never poll media or networks.

### Task 1: Session selection and event lifecycle

**Files:**
- Create `launcher/app/src/main/java/com/gathin/tvhome/NowPlayingSelection.java`.
- Create `launcher/app/src/main/java/com/gathin/tvhome/NowPlayingController.java`.
- Create `launcher/app/src/main/java/com/gathin/tvhome/MediaAccessService.java`.
- Modify `launcher/app/src/main/AndroidManifest.xml`.
- Create `launcher/app/src/test/java/com/gathin/tvhome/NowPlayingSelectionTest.java`.
- Create `launcher/app/src/androidTest/java/com/gathin/tvhome/NowPlayingControllerTest.java`.

**Interfaces:** Pure `NowPlayingSelection.choose(List<Candidate> sessions, String previousKey)`
returns candidate/null. Candidate has stable session key, package, active flag and playback
state; rank playing/buffering/connecting above paused, exclude stopped/null/destroyed.
Android controller exposes `start()`, `stop()`, `playPause()`, `openApp()` and a listener
`onChanged(Snapshot snapshot)` (null means hidden). Snapshot supplies selected identity,
title/artist/app label/artwork, playing flag and advertised Play/Pause availability.

- [ ] Write selection tests first: empty/null state/stopped→null; playing beats paused;
  active paused eligible; inactive excluded; equal rank retains previous key; destroyed
  selected session selects next or null. Run JVM tests and observe RED.
- [ ] Implement pure selection, then verify GREEN with `:app:testDebugUnitTest`.
- [ ] Add device tests for denied/revoked session access, callback cleanup/restart,
  selected-session destruction and matching-controller transport dispatch; run RED.
- [ ] Implement MediaSessionManager callbacks with minimal listener service declaration
  (`BIND_NOTIFICATION_LISTENER_SERVICE`), local-only metadata and lifecycle cleanup.
  Handle null metadata, app-label fallbacks, supplied-artwork downsampling to256px,
  unsupported actions, stale controls and canceled PendingIntent without global media keys.
- [ ] Run device tests/JVM suite/lint; confirm no access setting has been changed.

### Task 2: Panel rendering and deterministic focus

**Files:**
- Create `launcher/app/src/main/java/com/gathin/tvhome/NowPlayingView.java`.
- Create `launcher/app/src/main/res/layout/panel_now_playing.xml`.
- Modify `launcher/app/src/main/res/layout/activity_main.xml` and `layout-iw/activity_main.xml`.
- Modify `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java`.
- Modify `launcher/app/src/main/res/values/strings.xml` and `values-zh-rTW/strings.xml` if present.
- Create `launcher/app/src/androidTest/java/com/gathin/tvhome/NowPlayingViewTest.java`.

**Interfaces:** View binds a Snapshot/null and two callbacks; MainActivity supplies
focus return/entry via existing `focusTile`, `lastFocused`, `firstTopButton` facilities.
Controller starts in visible lifecycle and stops onStop; session callbacks never rebuild
the app grid or steal unrelated focus.

- [ ] Write device tests first: null snapshot hides; long/missing metadata remains legible;
  unsupported transport disabled; supplied/fallback art shown; app action uses callback;
  disappearing focused control returns to dock; no focus movement when focus is elsewhere.
  Inflate/test default and Hebrew layouts. Observe RED before view implementation.
- [ ] Implement compact panel, accessible labels, bounded artwork, and font-compatible
  text using existing glass/pill visual conventions; do not redesign dock/header.
- [ ] Integrate lifecycle/rendering and exact Up/Down/row-end navigation from the spec.
- [ ] Run all JVM/device tests plus lint; capture an idle Home screenshot before access.

### Task 3: Real media verification and instructions

**Files:**
- Create `docs/NOW_PLAYING.md`.
- Update `docs/devices/sony-kd-55x8500d-android7.md` with actual measured results only.
- Save screenshots/logs in existing `~/sony-tv-backups/20261022-011046/`.

**Interfaces:** Build local debug APK, verify MD5, standalone `pm install -r`, then
explicit start; never invoke the stock takeover installer during this update.

- [ ] Build/inspect API24 APK; run `python3 -m unittest discover -s tests -q`,
  `:app:testDebugUnitTest`, `:app:lintDebug` and all device instrumentation.
- [ ] Preserve installed app identity/signature/config/dock and disabled-package baseline;
  push/checksum/install update; verify focus/crash logs and pull/inspect idle screenshot.
- [ ] Explain notification-access scope and ask user consent. Stop before granting if
  absent; permission is a separate security-sensitive approval, not inferred from design.
- [ ] After consent, preserve existing grants, enable only this listener, verify enabled
  and bound, then ask user to start actual Spotify/AirPlay playback and return Home.
- [ ] Inspect active/paused/idle screenshots; verify title/artist/art, exact Play/Pause
  target, Open App, real remote focus, and background lifecycle. Note app limitations.
- [ ] Document opt-in/undo, metadata privacy, app limitations and reproducible build/tests.
  Run one fresh whole-change review and fix important findings with RED→GREEN tests.
- [ ] Report pending Wi-Fi/HOME/physical tests and ADB shutdown accurately; close debugging
  and verify refusal when all agreed device work is finished, not before recovery checks.
