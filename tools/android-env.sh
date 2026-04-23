#!/usr/bin/env bash
set -euo pipefail

get_repo_root() {
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  cd "${script_dir}/.." && pwd
}

get_android_sdk_dir() {
  local repo_root local_properties sdk_line sdk_dir
  repo_root="$(get_repo_root)"
  local_properties="${repo_root}/local.properties"
  if [[ ! -f "${local_properties}" ]]; then
    echo "local.properties not found at ${local_properties}" >&2
    exit 1
  fi

  sdk_line="$(grep '^sdk\.dir=' "${local_properties}" | head -n 1 || true)"
  if [[ -z "${sdk_line}" ]]; then
    echo "sdk.dir not found in local.properties" >&2
    exit 1
  fi

  sdk_dir="${sdk_line#sdk.dir=}"
  sdk_dir="${sdk_dir//\\:/:}"
  sdk_dir="${sdk_dir//\\\\/\\}"
  printf '%s\n' "${sdk_dir}"
}

get_adb_path() {
  local sdk_dir adb_path
  sdk_dir="$(get_android_sdk_dir)"
  adb_path="${sdk_dir}/platform-tools/adb.exe"
  if [[ ! -f "${adb_path}" ]]; then
    echo "adb.exe not found at ${adb_path}" >&2
    exit 1
  fi
  printf '%s\n' "${adb_path}"
}

get_gradle_wrapper_path() {
  local repo_root wrapper_path
  repo_root="$(get_repo_root)"
  wrapper_path="${repo_root}/gradlew.bat"
  if [[ ! -f "${wrapper_path}" ]]; then
    echo "gradlew.bat not found at ${wrapper_path}" >&2
    exit 1
  fi
  printf '%s\n' "${wrapper_path}"
}
