# Sony KD-55X8500D: experimental Android 7 launcher build

This local checkout adapts the launcher to Android 7.0 / API 24. The TV reports
`BRAVIA 4K 2015` / `SVP-DTV15`. This is not a firmware upgrade.

## Compatibility changes

- minSdk 24, compile/target SDK 34; package remains `com.gathin.tvhome`.
- Core-library desugaring supplies `java.time` on Android 7.
- Static Outfit weights plus an API-24/25 TextView inflater preserve fonts in
  main/settings/clock views, popup menus and Hebrew layouts. Android 8+ retains
  its existing resource-font path. Static files are generated from the bundled
  OFL font with `uv run --with fonttools==4.60.1 python tools/build-legacy-fonts.py`.
- Instrumentation tests exercise desugared dates/zones, static font loading and
  English/Hebrew inflation on the actual device.

## Build and verify (macOS)

Use JDK **17**, not the system JDK 27. Tools installed for this trial:
Homebrew `openjdk@17`, `android-commandlinetools`, platform 34, build-tools 34.0.0.

```sh
cd launcher
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
cd ..
python3 -m unittest discover -s tests -v
JAVA_HOME=/opt/homebrew/opt/openjdk@17 apkanalyzer manifest print launcher/app/build/outputs/apk/debug/app-debug.apk
```

Check package `com.gathin.tvhome`, minSdk **24**, targetSdk **34** in the built manifest.
Lint originally failed with 43 API-level errors before desugaring and font changes;
the compatibility build passed lint and 134 JVM tests.

## Trial install WITHOUT replacing Home

Do **not** run `launcher/build-and-install.sh` for a side-by-side trial: it performs
a HOME takeover. Use the following standalone commands instead. Connect/authorize
ADB first and check `pm path com.gathin.tvhome` before overwriting an existing build.

```sh
adb connect TV_IP:5555
adb -s TV_IP:5555 shell pm list packages -d
adb -s TV_IP:5555 shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s TV_IP:5555 push launcher/app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/tvhome-api24.apk
md5 -q launcher/app/build/outputs/apk/debug/app-debug.apk
adb -s TV_IP:5555 shell md5sum /data/local/tmp/tvhome-api24.apk
# Compare both checksums before installing:
adb -s TV_IP:5555 shell pm install -r /data/local/tmp/tvhome-api24.apk
adb -s TV_IP:5555 shell am start -W -n com.gathin.tvhome/.MainActivity
adb -s TV_IP:5555 shell dumpsys activity activities
adb -s TV_IP:5555 shell logcat -d -b crash
adb -s TV_IP:5555 shell screencap -p /data/local/tmp/tvhome-check.png
adb -s TV_IP:5555 pull /data/local/tmp/tvhome-check.png
```

Stop on installer verification/signature refusal. A timeout from `am start -W`
is not proof of failure or success: inspect activity focus, crash logs and the screen.
For a hanging push, use `tools/b64push.py` with a configured adb-shell venv, then
compare MD5 before `pm install`.

The initial trial installed successfully (APK MD5 `1565dce9b8bba1397d01dbd2a90a14fe`).
`am start -W` reported timeout, but dumpsys confirmed MainActivity resumed/focused,
the crash buffer was empty, and inspected screenshots showed the dock, scrolled
grid and an Inputs popup with all four HDMI inputs and two analog/component inputs.
Screenshots 21–23 are in `~/sony-tv-backups/20261022-011046/` (not published).
The three instrumentation tests passed on the actual API-24 TV. To repeat them:

```sh
adb -s TV_IP:5555 push launcher/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk /data/local/tmp/tvhome-tests.apk
# Compare local MD5 and the device's md5sum before installing, as above.
adb -s TV_IP:5555 shell pm install -r /data/local/tmp/tvhome-tests.apk
adb -s TV_IP:5555 shell am instrument -w com.gathin.tvhome.test/androidx.test.runner.AndroidJUnitRunner
```

After the trial, injected Home returned to `com.google.android.leanbacklauncher`;
HOME resolution and screenshot 24 confirmed Sony Home remained available. The
disabled-package list matched the pre-install baseline exactly.

## Preferences and remaining checks

The user selected this dock, left to right: YouTube, Netflix, Spotify, AirPlay Server.
It is saved in `themes/sony-kd-55x8500d/config.json` and applied to the TV's external
launcher files directory. Screenshots 25–26 verify the order and AirPlay Server label.
A theme's
`config.json` can set `dock_apps`; a theme may include `wallpaper.jpg` (see
`themes/README.md`). The Sony Live TV package still needs explicit configuration;
the trial's Inputs popup verified HDMI/AV enumeration, not actual signal playback.

Keep stock Home enabled. Press real Home to return to Sony; test real Input/Settings,
HDMI playback, streaming and retained casting before considering permanent takeover.
No accessibility helper or HOME preference was changed by this trial.

## Restore and security

Existing debloat backup/restore script: `~/sony-tv-backups/20261022-011046/restore.sh`.
It does not uninstall this launcher. For a trial, leaving stock Home enabled is the
recovery path; the launcher can be removed later from TV Settings if requested.
Re-enable one disabled app using `pm enable --user 0 PACKAGE`, then check
`pm list packages -d`. Do not enable previously disabled hotel/factory packages.

At the end of device work, disable USB/network debugging on the TV, revoke USB
debugging authorizations, reboot, and verify `tools/adb-closed.py TV_IP` reports
connection refusal. A timeout or macOS “No route to host” does not prove closure.
This final closure and physical-remote/playback checks are still pending.
