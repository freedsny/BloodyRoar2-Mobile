"""Genera el icono original de Bloody Roar 2 Mobile.

Diseño propio: tres zarpazos dorados sobre fondo rojo oscuro.
Salida: SVG de capas (fondo / frente) en el sistema de 108x108 de los
iconos adaptativos de Android, más una versión completa para vista previa.
"""
import math
import sys
from pathlib import Path

OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
OUT.mkdir(parents=True, exist_ok=True)


def slash(x0, y0, x1, y1, width, bend, n=40):
    """Polígono de un zarpazo: lomo curvo con grosor que se afila en las puntas."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    nx, ny = -dy / length, dx / length  # normal
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        # lomo curvo (parábola) desplazado por la normal
        off = bend * 4 * t * (1 - t)
        sx, sy = x0 + dx * t + nx * off, y0 + dy * t + ny * off
        # grosor: más ancho un poco antes de la mitad, puntas afiladas
        w = width * (math.sin(math.pi * t) ** 0.85) * (1.0 - 0.25 * t)
        left.append((sx + nx * w / 2, sy + ny * w / 2))
        right.append((sx - nx * w / 2, sy - ny * w / 2))
    pts = left + right[::-1]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z"


# Tres zarpazos paralelos, de arriba-derecha a abajo-izquierda, dentro de la
# zona segura (círculo de radio 33 centrado en 54,54).
SLASHES = [
    slash(63, 25, 30, 74, 7.0, 3.2),
    slash(72, 30, 39, 80, 8.2, 3.2),
    slash(80, 38, 49, 83, 6.6, 3.2),
]

DEFS = """
  <defs>
    <radialGradient id="bg" cx="50%" cy="42%" r="70%">
      <stop offset="0" stop-color="#7a1418"/>
      <stop offset="0.55" stop-color="#3a080b"/>
      <stop offset="1" stop-color="#120304"/>
    </radialGradient>
    <linearGradient id="gold" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#fff1b8"/>
      <stop offset="0.4" stop-color="#f6c94a"/>
      <stop offset="1" stop-color="#a8600f"/>
    </linearGradient>
    <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
      <feGaussianBlur stdDeviation="2.6"/>
    </filter>
  </defs>"""

slash_glow = "\n".join(
    f'    <path d="{d}" fill="#ff3b1f" opacity="0.75" filter="url(#glow)"/>' for d in SLASHES
)
slash_fill = "\n".join(f'    <path d="{d}" fill="url(#gold)" stroke="#7a3a08" stroke-width="0.5" stroke-linejoin="round"/>' for d in SLASHES)

background = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{DEFS}
  <rect width="108" height="108" fill="url(#bg)"/>
</svg>
"""

foreground = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{DEFS}
  <g>
{slash_glow}
{slash_fill}
  </g>
</svg>
"""


def full(clip):
    """Icono completo (fondo + frente) recortado en la forma dada, para iconos
    clásicos y vista previa."""
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">{DEFS}
  <clipPath id="c">{clip}</clipPath>
  <g clip-path="url(#c)">
    <rect width="108" height="108" fill="url(#bg)"/>
  {slash_glow}
{slash_fill}
  </g>
</svg>
"""


(OUT / "ic_background.svg").write_text(background)
(OUT / "ic_foreground.svg").write_text(foreground)
(OUT / "ic_launcher.svg").write_text(full('<rect x="18" y="18" width="72" height="72" rx="16"/>'))
(OUT / "ic_launcher_round.svg").write_text(full('<circle cx="54" cy="54" r="36"/>'))
print("ok", OUT)
