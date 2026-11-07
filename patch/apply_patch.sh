#!/usr/bin/env bash
# Aplica el parche de Bloody Roar 2 Mobile a un APK ya descompilado con apktool.
#   uso: apply_patch.sh <carpeta_descompilada> <carpeta_svg_del_icono>
# Cambia: nombre visible, icono (adaptativo + clásico) y nombre de paquete
# (para que se instale junto al original sin conflicto de firmas).
set -euo pipefail

DEC="$1"
SVG="$2"
APP_NAME="Bloody Roar 2 Mobile"
NEW_PACKAGE="io.github.freedsny.br2mobile"

# 1) Iconos PNG en todas las densidades
declare -A SCALE=( [mdpi]=1 [hdpi]=1.5 [xhdpi]=2 [xxhdpi]=3 [xxxhdpi]=4 )
for d in "${!SCALE[@]}"; do
  s="${SCALE[$d]}"
  out="$DEC/res/mipmap-$d"
  mkdir -p "$out"
  legacy=$(python3 -c "print(round(48*$s))")
  layer=$(python3 -c "print(round(108*$s))")
  rsvg-convert -w "$legacy" -h "$legacy" "$SVG/ic_launcher.svg"       -o "$out/ic_launcher.png"
  rsvg-convert -w "$legacy" -h "$legacy" "$SVG/ic_launcher_round.svg" -o "$out/ic_launcher_round.png"
  rsvg-convert -w "$layer"  -h "$layer"  "$SVG/ic_foreground.svg"     -o "$out/ic_launcher_foreground.png"
  rsvg-convert -w "$layer"  -h "$layer"  "$SVG/ic_background.svg"     -o "$out/ic_launcher_background.png"
done

# 2) Icono adaptativo (Android 8+)
mkdir -p "$DEC/res/mipmap-anydpi-v26"
for name in ic_launcher ic_launcher_round; do
  cat > "$DEC/res/mipmap-anydpi-v26/$name.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
    <monochrome android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
XML
done

# 3) Manifiesto: nombre e icono
python3 - "$DEC/AndroidManifest.xml" "$APP_NAME" <<'PY'
import sys, re
path, name = sys.argv[1], sys.argv[2]
s = open(path, encoding="utf-8").read()
s = s.replace('android:label="BR2 Recompiled"', f'android:label="{name}"', 1)
if 'android:icon=' not in s:
    s = s.replace('<application ', '<application android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" ', 1)
open(path, "w", encoding="utf-8").write(s)
PY

# 4) Paquete propio (las clases Java no cambian, solo el id de la app; las
#    actividades ya están declaradas con su nombre completo)
python3 - "$DEC/AndroidManifest.xml" "$NEW_PACKAGE" <<'PY'
import sys
path, pkg = sys.argv[1], sys.argv[2]
s = open(path, encoding="utf-8").read()
s = s.replace('package="com.br2recomp.android"', f'package="{pkg}"', 1)
open(path, "w", encoding="utf-8").write(s)
PY
sed -i 's/versionName: 0.1-scaffold/versionName: 1.0-mobile.1/' "$DEC/apktool.yml"

echo "Parche aplicado: '$APP_NAME' ($NEW_PACKAGE)"
