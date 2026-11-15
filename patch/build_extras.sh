#!/usr/bin/env bash
# Compila lo que el parche agrega con código propio:
#   - libbr2cheats.so  (plugin de trucos, C, arm64, sin libc)
#   - classes.dex      (menú de trucos en Java -> dex)
#
#   uso: build_extras.sh <salida> <libpsx_android.so> <android.jar> <comando d8...>
#   ej.: build_extras.sh extras dec/lib/arm64-v8a/libpsx_android.so \
#          "$ANDROID_HOME/platforms/android-34/android.jar" "$ANDROID_HOME/build-tools/34.0.0/d8"
set -euo pipefail

OUT="$1"; PSX_SO="$2"; ANDROID_JAR="$3"; shift 3
D8=("$@")
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"

# 1) Plugin de trucos. Solo usa funciones de libpsx_android.so, así que no
#    necesita el NDK: basta clang + lld apuntando a Android arm64.
clang --target=aarch64-linux-android26 -ffreestanding -fPIC -O2 -Wall -Wextra -Werror \
  -fno-stack-protector -nostdlib -shared -fuse-ld=lld \
  -Wl,-soname,libbr2cheats.so -Wl,-z,max-page-size=16384 -Wl,--no-undefined \
  -o "$OUT/libbr2cheats.so" "$HERE/cheats/br2cheats.c" \
  -L"$(dirname "$PSX_SO")" -lpsx_android

# 2) Menú de trucos (Java -> .class -> dex)
rm -rf "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/classes" "$OUT/dex"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$HERE/java" -name '*.java')
"${D8[@]}" --release --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class')
mv "$OUT/dex/classes.dex" "$OUT/classes.dex"

echo "Extras listos en $OUT: libbr2cheats.so, classes.dex"
