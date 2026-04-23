#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

repo_root="$(get_repo_root)"
adb_path="$(get_adb_path)"
activity_name="com.example.overlaybar/.MainActivity"

cd "${repo_root}"
./gradlew.bat :app:installDebug
"${adb_path}" wait-for-device
"${adb_path}" shell am start -n "${activity_name}"
