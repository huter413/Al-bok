#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")/.."
if [ -x ./gradlew ]; then ./gradlew :app:assembleDebug; else gradle :app:assembleDebug; fi
mkdir -p "$HOME/storage/downloads/Mojolauncher-Android"
cp app/build/outputs/apk/debug/app-debug.apk "$HOME/storage/downloads/Mojolauncher-Android/Mojolauncher-debug.apk"
echo "APK copied to Download/Mojolauncher-Android/Mojolauncher-debug.apk"
