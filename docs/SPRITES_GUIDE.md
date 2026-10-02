# Guía de Sprites para PixelBot

## Especificaciones Técnicas

| Parámetro | Valor |
|-----------|-------|
| **Tamaño de frame** | 32×32 píxeles |
| **Grid** | 8 columnas × 8 filas = 64 frames totales |
| **Tamaño spritesheet** | 256×256 píxeles |
| **Formato** | PNG con transparencia (alpha) |
| **Filtro** | Nearest-neighbor (sin suavizado) |
| **Paleta** | 4 colores exactos (ver abajo) |
| **Estilo** | Pixel art, vista frontal, contorno 1px, sin degradados ni antialiasing |

## Paleta de 4 Colores (OBLIGATORIA)

```json
[
  "#000000",  // 0 - Negro (contorno, ojos, detalles)
  "#FFFFFF",  // 1 - Blanco (cara, ojos, dientes)
  "#6C5CE7",  // 2 - Primario (cuerpo, orejas - morado Pixel)
  "#00CEC9"   // 3 - Secundario (acentos, ondas, burbujas - cyan)
]
```

> **Nota**: Para skin "Neón" usar: `#000000`, `#FF10F0`, `#00F0FF`, `#FFE600`

## Estados y Filas (7 filas × 8 frames)

| Fila | Estado | Frames | FPS | Loop | Descripción |
|------|--------|--------|-----|------|-------------|
| 0 | `dormido` | 2 | 4 | ✅ | Ojos cerrados, burbujas Zzz |
| 1 | `escuchando` | 6 | 12 | ✅ | Orejas animadas, ondas de sonido |
| 2 | `pensando` | 6 | 8 | ✅ | Burbuja de pensamiento, ojos arriba |
| 3 | `hablando` | 4 | 10 | ✅ | Boca abriéndose/cerrando |
| 4 | `feliz` | 4 | 8 | ✅ | Sonrisa, brillo en ojos |
| 5 | `error` | 4 | 6 | ❌ | Ojos en X, sacudida, cuerpo rojo |
| 6 | `confundido` | 4 | 8 | ✅ | Ojos desiguales, signo ? |
| 7 | *reservada* | - | - | - | Para futuros estados |

## Estructura skin.json

```json
{
  "id": "mi_skin",
  "name": "Mi Skin Personalizada",
  "author": "Tu Nombre",
  "version": 1,
  "frameSize": 32,
  "palette": ["#000000", "#FFFFFF", "#6C5CE7", "#00CEC9"],
  "rows": {
    "dormido": { "frames": 2, "rowIndex": 0, "loop": true },
    "escuchando": { "frames": 6, "rowIndex": 1, "loop": true },
    "pensando": { "frames": 6, "rowIndex": 2, "loop": true },
    "hablando": { "frames": 4, "rowIndex": 3, "loop": true },
    "feliz": { "frames": 4, "rowIndex": 4, "loop": true },
    "error": { "frames": 4, "rowIndex": 5, "loop": false },
    "confundido": { "frames": 4, "rowIndex": 6, "loop": true }
  },
  "fps": {
    "dormido": 4,
    "escuchando": 12,
    "pensando": 8,
    "hablando": 10,
    "feliz": 8,
    "error": 6,
    "confundido": 8
  }
}
```

## Distribución en Grid (8×8)

```
Col:  0  1  2  3  4  5  6  7
Row ┌─────────────────────────┐
  0 │ 😴😴                    │  dormido (2 frames)
  1 │ 👂👂👂👂👂👂              │  escuchando (6 frames)
  2 │ 🤔🤔🤔🤔🤔🤔              │  pensando (6 frames)
  3 │ 💬💬💬💬                  │  hablando (4 frames)
  4 │ 😊😊😊😊                  │  feliz (4 frames)
  5 │ ❌❌❌❌                  │  error (4 frames)
  6 │ 😕😕😕😕                  │  confundido (4 frames)
  7 │ 🔒🔒🔒🔒🔒🔒🔒🔒          │  reservada
└─────────────────────────┘
```

## Guía de Dibujo por Estado

### Fila 0 - DORMIDO (2 frames)
- **Frame 0**: Ojos cerrados (línea horizontal), cuerpo relajado, burbuja "z" pequeña
- **Frame 1**: Igual pero burbuja "z" más grande/alta (animación subiendo)

### Fila 1 - ESCUCHANDO (6 frames)
- **Frames 0,2,4**: Orejas posición normal
- **Frames 1,3,5**: Orejas movidas (arriba/abajo alternando)
- Ojos abiertos fijos
- Ondas de sonido a la derecha (3 líneas creciendo)

### Fila 2 - PENSANDO (6 frames)
- Ojos mirando arriba/esquina
- Burbuja de pensamiento creciendo (frame 0: pequeña → frame 5: grande)
- 2-3 burbujas pequeñas adicionales flotando

### Fila 3 - HABLANDO (4 frames)
- **Frame 0**: Boca cerrada (línea)
- **Frame 1**: Boca abierta grande (óvalo vertical)
- **Frame 2**: Boca media
- **Frame 3**: Boca muy abierta

### Fila 4 - FELIZ (4 frames)
- **Frames 0,2**: Ojos abiertos con brillo (pixel blanco en esquina)
- **Frames 1,3**: Ojos cerrados en arco (sonrisa con ojos)
- Boca: arco sonriendo
- Brillo en cuerpo (pixel secundario)

### Fila 5 - ERROR (4 frames, NO loop)
- **Frame 0**: Ojos en X (dos líneas cruzadas), cuerpo normal
- **Frame 1**: Cuerpo rojo, ojos en X
- **Frame 2**: Cuerpo rojo, offset X -2px (sacudida izquierda)
- **Frame 3**: Cuerpo rojo, offset X +2px (sacudida derecha)

### Fila 6 - CONFUNDIDO (4 frames)
- **Frame 0**: Ojo izq grande (3×3), ojo der pequeño (1×1), signo ? arriba
- **Frame 1**: Ojos intercambiados
- **Frame 2**: Ambos medianos, ? girando
- **Frame 3**: Igual frame 0

## Cómo Crear (Workflow Recomendado)

1. **Aseprite / Piskel / Pixelorama** (no IA generativa - fallan en grids exactos)
2. Canvas 256×256, grid 32×32 visible
3. Dibujar silueta base en frame (0,0)
4. Copiar a todos los frames de la fila
5. Animar frame por frame
6. Verificar: contorno 1px negro en todo el personaje
7. Exportar PNG
8. Crear `skin.json` correspondiente
9. Zip: `skin.json` + `sprites.png` → `mi_skin.zip`
10. Importar en app

## Testing Checklist

- [ ] Spritesheet exactamente 256×256
- [ ] 8×8 grid perfecto (sin offset)
- [ ] Solo 4 colores de la paleta usados
- [ ] Fondo transparente (alpha=0)
- [ ] Contorno 1px negro consistente
- [ ] Sin antialiasing (bordes duros)
- [ ] Frames por estado según tabla
- [ ] `skin.json` válido y coincidente
- [ ] Zip importa sin errores en app

## Skins Incluidas (Referencia)

1. **pixel_classic** - Morado/cyan clásico
2. **pixel_neon** - Rosa neón/cyan/amarillo

Ambas generadas por código en `SkinGenerator.kt` como referencia.