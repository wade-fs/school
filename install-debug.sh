#!/bin/bash
set -x
APK=$1
[ -z "$APK" ] && echo "Usage: $0 APK" && exit 1

adb uninstall com.wade.school.debug
adb install $APK
adb logcat -c
