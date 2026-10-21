# Fork branches

Maintained development lines in <https://github.com/eoleedi/cleanyourtv>:

```text
upstream baseline
└── android-tv-7
    └── feat/now-playing
```

- **android-tv-7:** Android 7.0 / API24 compatibility, desugared Java APIs,
  static Outfit font support, compatibility tests and build/device notes. No Now
  Playing service or panel. Compile/target SDK remain34; builds require JDK17.
- **feat/now-playing:** builds on android-tv-7 and adds the optional media panel,
  session selection/transport, its tests and separate permission/privacy documentation.
  Android notification access is never automatically enabled by installation.
- **main:** not changed by this split. Upstream remains the Gathin23 repository.

## Update policy

1. Land compatibility fixes on android-tv-7 first.
2. Merge android-tv-7 into feat/now-playing so the feature inherits those fixes.
3. Keep feature-only changes on feat/now-playing; do not merge it into the
   compatibility branch unless intentionally making Now Playing part of the baseline.
4. Inspect upstream updates before merging them into android-tv-7; preserve API24
   support rather than blindly accepting a newer minimum SDK or framework API.
5. Do not force-push these published branches. No automatic synchronization or
   unattended device management is configured.

## Verification before pushing either branch

```sh
python3 -m unittest discover -s tests -q
cd launcher
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Use locally appropriate JDK17 and SDK paths on other machines. Inspect the built
manifest for minSdk24 and targetSdk34. Device tests require an authorized connected
TV; install/start explicitly and verify screenshots before changing its default Home.
Do not run the takeover installer merely to test an update.

The existing install-script tests deliberately use historical SDK26 fixtures to
verify the minimum-SDK guard; those fixtures are not this fork's actual build floor.
Device backups, media metadata and raw network logs stay outside the repository.
