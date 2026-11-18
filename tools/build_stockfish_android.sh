#!/usr/bin/env bash
# Build an official Stockfish Android arm64 executable and package it for this app.
# Requires Linux/macOS shell, git, make, and Android NDK r27c or newer.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_NDK_HOME:=${ANDROID_NDK_ROOT:-}}"
if [[ -z "$ANDROID_NDK_HOME" || ! -d "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" ]]; then
  echo "Set ANDROID_NDK_HOME to an installed Android NDK r27c+ directory." >&2
  exit 2
fi
HOST_TAG=""
case "$(uname -s):$(uname -m)" in
  Linux:x86_64) HOST_TAG=linux-x86_64 ;;
  Darwin:arm64|Darwin:x86_64) HOST_TAG=darwin-x86_64 ;;
  *) echo "Unsupported build host: $(uname -s) $(uname -m)" >&2; exit 2 ;;
esac
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG/bin"
[[ -x "$TOOLCHAIN/aarch64-linux-android29-clang++" ]] || { echo "NDK toolchain not found: $TOOLCHAIN" >&2; exit 2; }
export PATH="$TOOLCHAIN:$PATH"
WORK="${STOCKFISH_WORK_DIR:-$ROOT/.stockfish-build}"
if [[ ! -d "$WORK/Stockfish/.git" ]]; then
  mkdir -p "$WORK"
  git clone --depth 1 https://github.com/official-stockfish/Stockfish.git "$WORK/Stockfish"
fi
cd "$WORK/Stockfish/src"
make net
make -j"$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)" build ARCH=armv8 COMP=ndk
make strip ARCH=armv8 COMP=ndk
mkdir -p "$ROOT/app/src/main/jniLibs/arm64-v8a"
install -m 0755 stockfish "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so"
git -C "$WORK/Stockfish" rev-parse HEAD > "$ROOT/STOCKFISH_COMMIT.txt"
cp "$WORK/Stockfish/Copying.txt" "$ROOT/STOCKFISH_LICENSE.txt" 2>/dev/null || true
file "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so"
sha256sum "$ROOT/app/src/main/jniLibs/arm64-v8a/libstockfish.so" | tee "$ROOT/STOCKFISH_BINARY_SHA256.txt"
echo "Stockfish packaged. Rebuild the Android app with ./gradlew assembleDebug."
