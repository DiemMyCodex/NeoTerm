#!/usr/bin/env bash
# Capture the four theme screenshots for docs/screenshots/ from a connected device.
#
# Usage:
#   1. Install the debug APK and open NeoTerm.
#   2. Run this script, switch to the requested theme in the in-app palette popup,
#      then press Enter when prompted. The script pulls each capture into place.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/docs/screenshots"
mkdir -p "$OUT"

adb wait-for-device

for theme in obsidian catppuccin_mocha tokyo_night dracula; do
    case "$theme" in
        obsidian)        file="theme_obsidian.png" ;;
        catppuccin_mocha) file="theme_catppuccin.png" ;;
        tokyo_night)      file="theme_tokyo_night.png" ;;
        dracula)          file="theme_dracula.png" ;;
    esac

    read -r -p "Switch the app to '$theme', then press Enter to capture... " _
    remote="/sdcard/neoterm_showcase_${theme}.png"
    adb shell screencap -p "$remote"
    adb pull "$remote" "$OUT/$file"
    adb shell rm "$remote"
    echo "  saved -> $OUT/$file"
done

echo "Done. Commit docs/screenshots/*.png with the updated README."
