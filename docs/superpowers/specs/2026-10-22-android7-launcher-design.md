# Android 7 launcher compatibility — approved scope

The user approved adapting the existing launcher for the Sony KD-55X8500D,
which reports BRAVIA 4K 2015, Android 7.0 / SDK 24, then test-installing it
alongside the enabled stock launcher. This is compatibility work, not a redesign.

Preserve the package identity com.gathin.tvhome, compile/target SDK 34, existing
appearance, resources, localization and AndroidX RecyclerView. Set minSdk to 24
only after adding support for newer Java/framework calls. Use core-library
desugaring for java.time; preserve the bundled open-license Outfit appearance
through a compatible font-loading path, including XML-inflated text on API 24.
Do not add blur, video, polling or enable optional animation/weather features.

The trial must not change HOME preferences, disable Sony's launcher or setup wizard,
enable accessibility services, alter existing disabled packages, or select a dock
order for the user. The repository's current default dock is not a user preference;
ask for four apps/order before a personalized or permanent handover.

Build and inspect the APK locally. Push it, verify the transfer, install with a
separate pm install shell call, start MainActivity explicitly, check foreground
activity/crash logs, inject navigation keys and inspect pulled screenshots.
Stock home must remain accessible with the real Home key. Stop on firmware
verification/HOME-signature refusal rather than retrying alternative install routes.
An installation refusal and a runtime crash are distinct outcomes; report them accurately.

Deliver compatibility source/tests and reproducible build/test-install instructions.
The existing backup and restore script live outside this repository under
~/sony-tv-backups/20261022-011046/. Do not overwrite those artifacts or user changes.
No commits or permanent launcher takeover without a separate user request.

Network ADB remains temporary: after real-device verification and any agreed next
steps, turn debugging off, revoke authorizations, reboot and verify refusal with
tools/adb-closed.py. This cleanup is not complete until checked.
