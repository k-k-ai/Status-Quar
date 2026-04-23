#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

repo_root="$(get_repo_root)"

cd "${repo_root}"
./gradlew.bat :app:assembleDebug
