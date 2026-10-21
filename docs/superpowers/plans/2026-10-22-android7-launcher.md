# Android 7 Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and trial the existing launcher on the Sony's Android 7 / SDK 24 without replacing stock Home.

**Architecture:** Keep the existing native Java/RecyclerView launcher. Add Java API desugaring and compatible font loading, audit API-26+ framework use with Android lint, then install/start the verified APK manually without the takeover script.

**Tech Stack:** Java 17, Android Gradle Plugin 8.5.2, Gradle wrapper, AndroidX, Python unittest, ADB.

**Spec:** `docs/superpowers/specs/2026-10-22-android7-launcher-design.md` (approved scope from chat).

## Global Constraints
- minSdk 24; compile/target SDK 34; package com.gathin.tvhome.
- Stock com.google.android.leanbacklauncher stays enabled; no HOME preference/accessibility/setup-wizard changes.
- No additional debloat, personalized dock order, video, blur or network polling.
- No commits without a user request; all verification results must be recorded accurately.
- Firmware verification/HOME-signature refusal ends the install attempt.

## Review Focus
- API-24 startup must not fail from java.time or font-resource inflation.
- XML and programmatically created text must both remain legible with compatible fonts.
- Newer Android versions must retain existing guarded receiver/network behavior.
- Install/start failure must leave stock Home enabled and usable.
- Screenshot zero bytes or lost ADB must stop visual-verification claims, not trigger more disables.

### Task 1: API-24 build and font compatibility

**Files:**
- Modify: `launcher/app/build.gradle`.
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java`.
- Audit: `launcher/app/src/main/java/com/gathin/tvhome/` and `launcher/app/src/main/res/` (including Hebrew layouts).
- Create: `tests/test_android7_compat.py`.
- Update if needed: bundled font resources and affected text-inflation paths, using the smallest compatible helper rather than a UI rewrite.

**Interfaces:** Existing activities/configuration/package identity remain unchanged. New build dependency: pinned `com.android.tools:desugar_jdk_libs:2.1.5` with coreLibraryDesugaring enabled.

- [ ] Add regression tests asserting minSdk 24, desugaring enabled and dependency pinned, and no direct `Resources.getFont` call in AppLibraryDialog.
- [ ] Run `python3 -m unittest discover -s tests -p test_android7_compat.py -v`; verify failure against the unchanged build/source.
- [ ] Check available JDK 17/Android SDK; record versions and baseline test results. Do not run the takeover installer.
- [ ] Enable core-library desugaring and set minSdk 24. Run `./gradlew :app:lintDebug` from launcher with JDK 17; inspect actual NewApi failures instead of suppressing them wholesale.
- [ ] Replace direct framework font loading with AndroidX-compatible loading; inspect bundled font-family XML and variable-font support. Ensure layouts inflated on API 24 receive compatible fonts, covering default and Hebrew resources. Keep Outfit licensing with any necessary static font assets.
- [ ] Fix only API-24 incompatibilities identified by lint/source audit; preserve version guards for API 28/33 behavior.
- [ ] Re-run the new regression tests, existing JVM tests (`./gradlew :app:testDebugUnitTest`) and lint. Record any pre-existing unrelated failures separately.

### Task 2: Reproducible trial build and instructions

**Files:**
- Create: `docs/devices/sony-kd-55x8500d-android7.md`.
- Modify: `docs/TROUBLESHOOTING.md` only to distinguish this local compatibility build from the original SDK-26 limitation.

**Interfaces:** Produce `launcher/app/build/outputs/apk/debug/app-debug.apk`; do not modify the stock build-and-install takeover behavior in this task.

- [ ] Build using `./gradlew :app:assembleDebug` and inspect merged APK manifest with SDK tooling; verify minSdk 24 and package com.gathin.tvhome.
- [ ] Run `python3 -m unittest discover -s tests -v`; preserve historical SDK-26 install-script fixtures rather than changing tests merely to turn them green.
- [ ] Document exact JDK/SDK/build commands, safe push + checksum + pm install + explicit activity start commands, stock-home recovery and debugging cleanup. Clearly label Android 7 support experimental until real-device tests pass.

### Task 3: Real Sony trial without takeover

**Files:**
- Update: `docs/devices/sony-kd-55x8500d-android7.md` with measured outcomes.
- Save: new screenshots and command logs in the existing external TV backup folder.

**Interfaces:** ADB serial 192.168.50.28:5555; activity com.gathin.tvhome/.MainActivity. Existing disabled-package list is the comparison baseline.

- [ ] Reconnect, verify SDK/model, read HOME claimants/current owner and disabled-package list. Check no existing com.gathin.tvhome install will be overwritten unexpectedly.
- [ ] Push locally built APK; compare local/device MD5. If push hangs, use the repository base64 transfer tool and verify checksum.
- [ ] Run one standalone `pm install -r` call. Stop on verification/signature refusal; leave stock home untouched.
- [ ] Start explicitly with `am start -W -n com.gathin.tvhome/.MainActivity`; check activity focus and crash logs before claiming startup.
- [ ] Inject navigation keys, pull/inspect screenshots of dock/grid/top bar and Inputs popup; correct compatibility regressions before proceeding. Empty screenshots or disconnected ADB mean verification is pending.
- [ ] Ask user to press real Home/Input/Settings and test streaming/casting. Verify Home still opens Sony and its package remains enabled; compare disabled list against baseline.
- [ ] Ask for four dock apps/order only if proceeding to personalization; permanent takeover requires separate approval after the trial is shown.
- [ ] End with accurate trial outcome and rollback instructions; perform debugging closure/revocation/reboot/refusal verification when device work is finished.
