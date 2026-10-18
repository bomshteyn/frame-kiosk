#!/usr/bin/env bash
# Builds build/homehq.apk without Gradle. Needs JDK 17+, Android build-tools 34 and platform 34:
#   sdkmanager --sdk_root=$ANDROID_HOME "build-tools;34.0.0" "platforms;android-34"
# PUBLISH_DIR=../frame-dashboard/public/kiosk ./build.sh  also copies the APK there and writes version.json,
# which the frames use to update themselves (bump VERSION_CODE first).
set -euo pipefail
cd "$(dirname "$0")"

VERSION_CODE=4
VERSION_NAME=1.3
ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
BT="${BUILD_TOOLS:-$ANDROID_HOME/build-tools/34.0.0}"
JAR="$ANDROID_HOME/platforms/android-34/android.jar"
# Frames only accept updates signed with the same key as the installed app.
KS="${KEYSTORE:-$HOME/.android/debug.keystore}"

rm -rf build && mkdir -p build/gen build/classes

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml build/res.zip \
  --java build/gen --min-sdk-version 23 --target-sdk-version 23 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

javac -nowarn -source 8 -target 8 -classpath "$JAR" -d build/classes \
  src/com/wesbos/homehq/*.java build/gen/com/wesbos/homehq/R.java

"$BT/d8" --min-api 23 --lib "$JAR" --output build $(find build/classes -name '*.class')
(cd build && zip -qj base.apk classes.dex)
"$BT/zipalign" -f 4 build/base.apk build/aligned.apk

if [ ! -f "$KS" ]; then
  echo "No keystore at $KS. Use the key the frames' app was signed with (KEYSTORE=...)." >&2
  exit 1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out build/homehq.apk build/aligned.apk
echo "Built build/homehq.apk"

if [ -n "${PUBLISH_DIR:-}" ]; then
  cp build/homehq.apk "$PUBLISH_DIR/homehq.apk"
  SHA=$(shasum -a 256 build/homehq.apk | cut -d' ' -f1)
  printf '{\n  "versionCode": %s,\n  "versionName": "%s",\n  "sha256": "%s",\n  "apk": "/kiosk/homehq.apk"\n}\n' \
    "$VERSION_CODE" "$VERSION_NAME" "$SHA" > "$PUBLISH_DIR/version.json"
  echo "Published to $PUBLISH_DIR"
fi
