# frame-kiosk (Home HQ)

Android kiosk app (`com.wesbos.homehq`) for the photo frames. It opens the dashboard from [bomshteyn/frame-dashboard](https://github.com/bomshteyn/frame-dashboard) full screen and updates itself silently (it is device owner) when `/kiosk/version.json` on the dashboard has a higher `versionCode`.

Based on the kiosk in [wesbos/photo-frame-dashboard](https://github.com/wesbos/photo-frame-dashboard), plus device-owner self-update (`Updater`, `AdminReceiver`, `InstallResultReceiver`, `UpdatedReceiver`), an About screen and a "Turn off automatic updates" option.

The source in `src/` was recovered by decompiling the installed 1.3 (versionCode 4) APK, so some names are generated (e.g. `AnonymousClass2`, `m3lambda$...`). Rebuilding it gives the same manifest and resources.

## Build and release

```sh
sdkmanager --sdk_root=$HOME/android-sdk "build-tools;34.0.0" "platforms;android-34"
# bump VERSION_CODE / VERSION_NAME in build.sh, then:
KEYSTORE=/path/to/signing.keystore PUBLISH_DIR=../frame-dashboard/public/kiosk ./build.sh
```

Then commit `public/kiosk/` in frame-dashboard and deploy it. Each frame installs the update on its next check.

**Signing:** frames only accept an update signed with the same key as the installed app (debug key, SHA-256 `8F:4E:82:A0:C4:9E:FA:0F:85:6F:57:AA:2F:A2:D3:4C:97:A4:14:75:C1:01:23:28:6F:FD:45:18:01:76:67:BB`). Never commit the keystore.
