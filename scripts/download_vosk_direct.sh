#!/data/data/com.termux/files/usr/bin/bash
# Descarga directa del modelo Vosk Español para PixelBot
# Modelo: vosk-model-es-0.42 (~128 MB)

set -e

DEST_DIR="/data/data/com.termux/files/home/PixelBot/ears/src/main/assets/vosk-model"
MODEL_NAME="vosk-model-es-0.42"
ZIP_URL="https://alphacephei.com/vosk/models/${MODEL_NAME}.zip"

echo "=== Descargando Vosk Spanish Model ==="
echo "Destino: $DEST_DIR"
echo "URL: $ZIP_URL"

mkdir -p "$DEST_DIR"
cd "$(dirname "$DEST_DIR")"

if [ -d "vosk-model/am" ] && [ -f "vosk-model/conf/mfcc.conf" ]; then
    echo "✓ Modelo ya existe y es válido"
    exit 0
fi

echo "Descargando..."
if command -v wget &> /dev/null; then
    wget -q --show-progress "$ZIP_URL" -O vosk-model.zip
elif command -v curl &> /dev/null; then
    curl -L --progress-bar "$ZIP_URL" -o vosk-model.zip
else
    echo "✗ Necesitas wget o curl instalado"
    echo "  Termux: pkg install wget"
    exit 1
fi

echo "Extrayendo..."
unzip -q vosk-model.zip
mv "$MODEL_NAME"/* vosk-model/
rm -rf "$MODEL_NAME" vosk-model.zip

echo ""
echo "✓ Modelo Vosk listo:"
ls -la vosk-model/
echo ""
echo "Tamaño: $(du -sh vosk-model | cut -f1)"
echo ""
echo "Para usar en la app, compilar con: ./gradlew :ears:assembleDebug"