# frame-kiosk (Home HQ)

Android kiosk app (Java package `com.bomshteyn.homehq`) for the photo frames. It opens the dashboard from [bomshteyn/frame-dashboard](https://github.com/bomshteyn/frame-dashboard) full screen and updates itself silently (it is device owner) when `/kiosk/version.json` on the dashboard has a higher `versionCode`.

It includes device-owner self-update (`Updater`, `AdminReceiver`, `InstallResultReceiver`, `UpdatedReceiver`), an About screen and a "Turn off automatic updates" option.

The source in `src/` was recovered by decompiling the installed 1.3 (versionCode 4) APK, so some names are generated (e.g. `AnonymousClass2`, `m3lambda$...`). Rebuilding it gives the same manifest and resources.

## Build and release

```sh
sdkmanager --sdk_root=$HOME/android-sdk "build-tools;34.0.0" "platforms;android-34"
# bump VERSION_CODE / VERSION_NAME in build.sh, then:
KEYSTORE=/path/to/signing.keystore PUBLISH_DIR=../frame-dashboard/public/kiosk ./build.sh
```

Then commit `public/kiosk/` in frame-dashboard and deploy it. Each frame installs the update on its next check.

**Signing:** frames only accept an update signed with the same key as the installed app (debug key, SHA-256 `8F:4E:82:A0:C4:9E:FA:0F:85:6F:57:AA:2F:A2:D3:4C:97:A4:14:75:C1:01:23:28:6F:FD:45:18:01:76:67:BB`). Never commit the keystore.

## App ID

The installed app ID is still `com.wesbos.homehq` (`APP_ID` in `build.sh`, applied with `aapt2 --rename-manifest-package`), so the APK stays an update to the app already on the frames. Changing `APP_ID` turns it into a different app: install it by hand on each frame, remove the old one, and set up device owner, Home and the navigation service again.

The Java package changed in 2026-10, so component names are now `com.wesbos.homehq/com.bomshteyn.homehq.*`. If an APK built from this code is ever installed over the old one (only possible with the original signing key), device owner, the Home alias and the navigation accessibility service still point at the old `com.wesbos.homehq.*` classes. Expect to run the setup again with the new names, e.g. `dpm set-device-owner com.wesbos.homehq/com.bomshteyn.homehq.AdminReceiver` and `cmd package set-home-activity com.wesbos.homehq/com.bomshteyn.homehq.Home`.
