#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

adb_path="$(get_adb_path)"
package_name="com.example.overlaybar"
pid="$("${adb_path}" shell pidof -s "${package_name}" | tr -d '\r')"

if [[ -z "${pid}" ]]; then
  echo "App is not running: ${package_name}" >&2
  exit 1
fi

"${adb_path}" logcat --pid="${pid}"
