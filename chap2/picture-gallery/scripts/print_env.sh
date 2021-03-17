#!/usr/bin/env bash
set -euo pipefail

lein with-profile dev run -m picture-gallery.cmd.print-env.core/-main
