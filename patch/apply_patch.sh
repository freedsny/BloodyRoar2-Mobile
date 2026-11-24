#!/usr/bin/env bash
# Aplica el parche de Bloody Roar 2 Mobile a un APK ya descompilado con apktool.
#   uso: apply_patch.sh <carpeta_descompilada> <carpeta_svg_del_icono> <carpeta_extras>
# Cambia: nombre visible, icono (adaptativo + clásico) y nombre de paquete
# (para que se instale junto al original sin conflicto de firmas).
# Agrega: controles estilo PlayStation, botones GUARDAR / CARGAR / MENÚ y
# menú "Trucos y mejoras".
# <carpeta_extras> es la salida de build_extras.sh (libbr2cheats.so).
# El classes.dex del menú se mete al APK después de "apktool b".
set -euo pipefail

DEC="$1"
SVG="$2"
EXTRAS="$3"
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
# 5) Interfaz del juego (Br2GameUi, en el classes.dex de build_extras.sh):
#    controles táctiles estilo PlayStation + botones GUARDAR / CARGAR / MENÚ.
#    - br2AddTouchOverlay() pasa a llamar a Br2GameUi.attach().
#    - Se borra el TouchControlsView original: el nuevo (Java) lo reemplaza con
#      la misma API, así el editor de controles sigue funcionando.
#    El APK debe descompilarse CON código (sin -s).
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_SMALI=$(find "$DEC" -path '*/com/br2recomp/android/MainActivity.smali' | head -n1)
if [ -z "$MAIN_SMALI" ]; then
  echo "ERROR: no encontré MainActivity.smali (¿se descompiló con -s?)" >&2
  exit 1
fi
APP_SMALI_DIR="$(dirname "$MAIN_SMALI")"
rm -f "$APP_SMALI_DIR/TouchControlsView.smali" "$APP_SMALI_DIR/TouchControlsView\$Listener.smali"
if ls "$APP_SMALI_DIR"/TouchControlsView*.smali >/dev/null 2>&1; then
  echo "ERROR: quedó un TouchControlsView viejo en $APP_SMALI_DIR" >&2
  exit 1
fi
python3 - "$MAIN_SMALI" <<'PY2'
import sys
path = sys.argv[1]
s = open(path, encoding="utf-8").read()
header = ".method private br2AddTouchOverlay()V"
start = s.index(header)
end = s.index(".end method", start) + len(".end method")
s = s[:start] + """.method private br2AddTouchOverlay()V
    .locals 2

    iget-boolean v0, p0, Lcom/br2recomp/android/MainActivity;->br2UseOverlay:Z

    iget v1, p0, Lcom/br2recomp/android/MainActivity;->br2TouchAlpha:I

    invoke-static {p0, v0, v1}, Lcom/br2recomp/android/Br2GameUi;->attach(Landroid/app/Activity;ZI)V

    return-void
.end method""" + s[end:]
open(path, "w", encoding="utf-8").write(s)
PY2

# 6) Trucos: plugin nativo, paquete de mods y menú.
#    - libbr2cheats.so va antes de psx_android en getLibraries(): SDL busca
#      SDL_main en la ÚLTIMA librería de la lista, y el plugin tiene que
#      registrarse antes de que el motor resuelva los mods.
#    - Br2Cheats.install() escribe files/mods antes de que arranque el motor.
#    - El botón "Mods" del launcher (que decía "próximamente") abre el menú.
cp "$EXTRAS/libbr2cheats.so" "$DEC/lib/arm64-v8a/"
mkdir -p "$DEC/assets/br2mobile_cheats"
cp "$HERE/cheats/manifest.toml" "$DEC/assets/br2mobile_cheats/manifest.toml"
SELECT_SMALI="$(dirname "$MAIN_SMALI")/SelectGameActivity.smali"
python3 - "$MAIN_SMALI" "$SELECT_SMALI" "$DEC/res/layout/activity_select_game.xml" <<'PY'
import re, sys
main_path, select_path, layout_path = sys.argv[1:4]

def method(src, header):
    start = src.index(header)
    end = src.index(".end method", start) + len(".end method")
    return start, end

# getLibraries -> { "SDL2", "br2cheats", "psx_android" }
s = open(main_path, encoding="utf-8").read()
start, end = method(s, ".method protected getLibraries()[Ljava/lang/String;")
s = s[:start] + """.method protected getLibraries()[Ljava/lang/String;
    .locals 3

    const/4 v0, 0x3

    new-array v0, v0, [Ljava/lang/String;

    const/4 v1, 0x0

    const-string v2, "SDL2"

    aput-object v2, v0, v1

    const/4 v1, 0x1

    const-string v2, "br2cheats"

    aput-object v2, v0, v1

    const/4 v1, 0x2

    const-string v2, "psx_android"

    aput-object v2, v0, v1

    return-object v0
.end method""" + s[end:]

# onCreate: preparar trucos justo antes de br2PrepareInput()
install = "    invoke-static {p0}, Lcom/br2recomp/android/Br2Cheats;->install(Landroid/content/Context;)V\n\n"
anchor = "    invoke-direct {p0}, Lcom/br2recomp/android/MainActivity;->br2PrepareInput()V\n"
if install not in s:
    assert s.count(anchor) == 1, "br2PrepareInput() no aparece una sola vez"
    s = s.replace(anchor, install + anchor)
open(main_path, "w", encoding="utf-8").write(s)

# Botón "Mods" del launcher -> menú de trucos
s = open(select_path, encoding="utf-8").read()
start, end = method(s, ".method private synthetic lambda$onCreate$4(Landroid/view/View;)V")
assert "Mods: pr" in s[start:end], "el botón Mods ya no es el esperado"
s = s[:start] + """.method private synthetic lambda$onCreate$4(Landroid/view/View;)V
    .locals 0
    .param p1, "v"    # Landroid/view/View;

    invoke-static {p0}, Lcom/br2recomp/android/Br2Cheats;->openMenu(Landroid/app/Activity;)V

    return-void
.end method""" + s[end:]
open(select_path, "w", encoding="utf-8").write(s)

s = open(layout_path, encoding="utf-8").read()
s2 = re.sub(r'(android:id="@id/button_mods"[^>]*?android:text=")Mods(")', r"\1Trucos\2", s)
assert s2 != s, "no encontré el texto del botón Mods"
open(layout_path, "w", encoding="utf-8").write(s2)
PY

sed -i 's/versionCode: 1$/versionCode: 6/' "$DEC/apktool.yml"
sed -i 's/versionName: 0.1-scaffold/versionName: 1.0-mobile.6/' "$DEC/apktool.yml"

echo "Parche aplicado: '$APP_NAME' ($NEW_PACKAGE) con controles estilo PlayStation, GUARDAR / CARGAR / MENÚ y Trucos"
