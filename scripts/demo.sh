#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
library_root=${WORKER_PACKAGES_DIR:-"$project_root/../worker-packages-java"}
sdk_dir=${TEMPORAL_SDK_DIR:-"$project_root/../sdk-java"}
TEMPORAL_SDK_DIR="$sdk_dir" "$library_root/scripts/bootstrap-sdk.sh"
exec ./gradlew demo "$@"
