#!/bin/bash
# Script para descargar modelos necesarios para PixelBot
# Ejecutar desde la raíz del proyecto: ./scripts/download_models.sh

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS_EARS="$PROJECT_ROOT/ears/src/main/assets"
ASSETS_APP="$PROJECT_ROOT/app/src/main/assets"

echo "=== PixelBot Model Downloader ==="
echo "Project root: $PROJECT_ROOT"

# Crear directorios
mkdir -p "$ASSETS_EARS/vosk-model"
mkdir -p "$ASSETS_APP/models"

# ============================================
# 1. VOSK MODEL - Español (Argentina compatible)
# ============================================
VOSK_MODEL="vosk-model-es-0.42"
VOSK_URL="https://alphacephei.com/vosk/models/$VOSK_MODEL.zip"
VOSK_DEST="$ASSETS_EARS/vosk-model"

echo ""
echo "--- Descargando Vosk Spanish Model ---"
echo "URL: $VOSK_URL"
echo "Destino: $VOSK_DEST"

if [ -d "$VOSK_DEST/am" ] && [ -f "$VOSK_DEST/conf/mfcc.conf" ]; then
    echo "✓ Modelo Vosk ya existe, saltando..."
else
    echo "Descargando (~128 MB)..."
    cd "$ASSETS_EARS"
    
    # Usar wget o curl según disponibilidad
    if command -v wget &> /dev/null; then
        wget -q --show-progress "$VOSK_URL" -O vosk-model.zip
    elif command -v curl &> /dev/null; then
        curl -L --progress-bar "$VOSK_URL" -o vosk-model.zip
    else
        echo "✗ Error: wget o curl requerido"
        exit 1
    fi
    
    echo "Extrayendo..."
    unzip -q vosk-model.zip
    mv "$VOSK_MODEL"/* vosk-model/
    rm -rf "$VOSK_MODEL" vosk-model.zip
    
    echo "✓ Modelo Vosk listo en $VOSK_DEST"
fi

# Verificar estructura
if [ -d "$VOSK_DEST/am" ] && [ -f "$VOSK_DEST/conf/mfcc.conf" ]; then
    echo "  Estructura verificada:"
    ls -la "$VOSK_DEST/"
else
    echo "✗ Error: Estructura de modelo inválida"
    exit 1
fi

# ============================================
# 2. GEMINI NANO (Google AI Edge) - Opcional
# ============================================
echo ""
echo "--- Gemini Nano (Google AI Edge) ---"
echo "NOTA: Los modelos .task de MediaPipe LLM no se distribuyen públicamente."
echo "Opciones:"
echo "  1. Usar MediaPipe LLM Inference API (descarga automática en runtime)"
echo "  2. Usar llama.cpp con modelo GGUF cuantizado"
echo "  3. Compilar tu propio modelo con MediaPipe Model Maker"

GEMINI_MODEL_DIR="$ASSETS_APP/models"
mkdir -p "$GEMINI_MODEL_DIR"

# Crear placeholder con instrucciones
cat > "$GEMINI_MODEL_DIR/README.md" << 'EOF'
# Modelos Locales para PixelBot

## Gemini Nano / MediaPipe LLM

Los modelos `.task` de MediaPipe LLM Inference no están disponibles para descarga directa.
Opciones para obtener un modelo local:

### Opción A: MediaPipe LLM Inference (Recomendado)
El SDK descarga el modelo automáticamente en runtime.
Requiere Google Play Services y descarga ~1.5GB en primer uso.

```kotlin
// En LocalProvider.kt - usar LlmInference.createFromOptions()
// El modelo se descarga automáticamente
```

### Opción B: llama.cpp + GGUF (Totalmente offline)
1. Descargar modelo cuantizado (ej. Gemma 2B, Phi-3 mini, Qwen 1.5B):
   - https://huggingface.co/bartowski/Gemma-2-2B-it-GGUF
   - https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf

2. Convertir a formato MediaPipe (requiere MediaPipe Model Maker):
   ```bash
   pip install mediapipe-model-maker
   # Seguir docs de MediaPipe para conversión
   ```

3. Colocar `.task` en `app/src/main/assets/models/`

### Opción C: Modelos precompilados comunidad
- https://github.com/mbariola/llm-android (ejemplos con Gemma)
- Buscar en Hugging Face: `mediapipe task llm`

## Modelos recomendados para móviles (2024)

| Modelo | Tamaño | Calidad | Velocidad |
|--------|--------|---------|-----------|
| Gemma 2B IT Q4_K_M | ~1.3 GB | Buena | Rápida |
| Phi-3 Mini 4K Q4_K_M | ~1.5 GB | Muy buena | Media |
| Qwen 1.5B Chat Q4_K_M | ~1.0 GB | Buena | Muy rápida |
| SmolLM 1.7B Q4_K_M | ~1.1 GB | Aceptable | Rápida |

## Estructura esperada
```
app/src/main/assets/models/
├── gemma-2b-it-q4.task      # Modelo principal
└── tokenizer.model          # Tokenizer (si separado)
```

## Nota legal
Verificar licencias de cada modelo antes de distribuir en APK.
Gemma: Apache 2.0 | Phi-3: MIT | Qwen: Apache 2.0
EOF

echo "✓ README creado en $GEMINI_MODEL_DIR/README.md"

# ============================================
# 3. DESCARGAR MODELO GGUF DE EJEMPLO (Opcional)
# ============================================
echo ""
read -p "¿Descargar modelo GGUF de ejemplo (Gemma 2B Q4_K_M ~1.3GB)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    GGUF_URL="https://huggingface.co/bartowski/Gemma-2-2B-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"
    GGUF_DEST="$GEMINI_MODEL_DIR/gemma-2-2b-it-Q4_K_M.gguf"
    
    echo "Descargando Gemma 2B Q4_K_M (~1.3 GB)..."
    echo "URL: $GGUF_URL"
    
    if command -v wget &> /dev/null; then
        wget --show-progress "$GGUF_URL" -O "$GGUF_DEST"
    elif command -v curl &> /dev/null; then
        curl -L --progress-bar "$GGUF_URL" -o "$GGUF_DEST"
    else
        echo "✗ wget/curl requerido"
    fi
    
    if [ -f "$GGUF_DEST" ]; then
        echo "✓ Modelo GGUF descargado"
        echo "  Nota: Requiere conversión a .task para MediaPipe"
        echo "  Ver: https://github.com/google/mediapipe/blob/master/docs/solutions/llm_inference.md"
    fi
fi

# ============================================
# RESUMEN
# ============================================
echo ""
echo "=== RESUMEN ==="
echo "✓ Vosk Spanish: $VOSK_DEST"
echo "  Tamaño: $(du -sh "$VOSK_DEST" | cut -f1)"
echo ""
echo "📁 Modelos locales: $GEMINI_MODEL_DIR"
ls -la "$GEMINI_MODEL_DIR"/
echo ""
echo "Para usar Vosk en la app:"
echo "  1. El SkinLoader carga desde assets/skins/"
echo "  2. VoskRecognizer extrae el modelo a filesDir en primer uso"
echo ""
echo "Para IA local:"
echo "  - Opción A: MediaPipe LLM (auto-descarga, requiere Play Services)"
echo "  - Opción B: Convertir GGUF a .task con MediaPipe Model Maker"
echo "  - Opción C: Usar Ollama en red local (ya implementado en OllamaProvider)"
echo ""
echo "¡Listo! Ejecuta ./gradlew assembleDebug para compilar."