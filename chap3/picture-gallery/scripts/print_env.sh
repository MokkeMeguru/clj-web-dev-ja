#!/usr/bin/env bash
set -euo pipefail

lein with-profile dev exec -p src/picture_gallery/cmd/print_env/core.clj
