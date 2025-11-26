#!/bin/bash

set -e  # Exit on error

# Get the script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Get the project root (parent of Scripts folder)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Resolve walletkit path using the resolver script
echo "Resolving walletkit path..."
WALLETKIT_PATH=$("$PROJECT_ROOT/Scripts/resolve-walletkit-path.sh" "$1")

if [ -z "$WALLETKIT_PATH" ]; then
    echo "Error: Failed to resolve walletkit path"
    exit 1
fi

echo "Walletkit path: $WALLETKIT_PATH"

if ! command -v pnpm &> /dev/null; then
    echo "Error: pnpm is not installed. Please install it first."
    echo "Run: npm install -g pnpm"
    exit 1
fi

if ! command -v openapi-generator &> /dev/null; then
    echo "Error: openapi-generator is not installed. Install it via Homebrew or npm."
    exit 1
fi

# Allow overriding the OpenAPI spec path (useful for local testing)
MANUAL_SPEC="${OPENAPI_SPEC:-${WALLETKIT_OPENAPI_SPEC:-$2}}"
if [ -n "$MANUAL_SPEC" ]; then
    OPENAPI_SPEC="$(cd "$(dirname "$MANUAL_SPEC")" && pwd)/$(basename "$MANUAL_SPEC")"
    echo "Using provided OpenAPI spec: $OPENAPI_SPEC"
else
    cd "$WALLETKIT_PATH"

    echo "Installing WalletKit dependencies..."
    pnpm install

    # Try to run generate-openapi-spec from walletkit-android-bridge package
    if [ -f "packages/walletkit-android-bridge/package.json" ] && grep -q "generate-openapi-spec" "packages/walletkit-android-bridge/package.json"; then
        echo "Running pnpm generate-openapi-spec in walletkit-android-bridge..."
        cd packages/walletkit-android-bridge
        OPENAPI_SPEC=$(pnpm generate-openapi-spec 2>&1 | grep -oE '/.*walletkit-openapi\.json' | tail -1)
        cd "$WALLETKIT_PATH"
    elif pnpm -s run | grep -q "generate-openapi-spec"; then
        echo "Running pnpm generate-openapi-spec..."
        OPENAPI_SPEC=$(pnpm generate-openapi-spec 2>&1 | grep -oE '/.*walletkit-openapi\.json' | tail -1)
    else
        echo "Error: No generate-openapi-spec script found in walletkit. Provide an OpenAPI spec via OPENAPI_SPEC=<path> or WALLETKIT_OPENAPI_SPEC=<path>."
        exit 1
    fi
    echo "OpenAPI spec path: $OPENAPI_SPEC"
fi

OUTPUT_DIR="${SCRIPT_DIR}/generated/openapi"
CONFIG_FILE="${SCRIPT_DIR}/generate-api-models-config.json"
TEMPLATES_DIR="${SCRIPT_DIR}/templates"
DEST_DIR="${PROJECT_ROOT}/TONWalletKit-Android/api/src/main/java/io/ton/walletkit/api/generated"

if [ -z "$OPENAPI_SPEC" ]; then
    echo "❌ Error: OpenAPI specification file is required"
    exit 1
fi

if [ ! -f "$OPENAPI_SPEC" ]; then
    echo "❌ Error: OpenAPI specification not found at '$OPENAPI_SPEC'"
    exit 1
fi

echo "🧹 Cleaning output directory..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "🔨 Generating Kotlin models..."
openapi-generator generate \
    -i "$OPENAPI_SPEC" \
    -g kotlin \
    -o "$OUTPUT_DIR" \
    -c "$CONFIG_FILE" \
    -t "$TEMPLATES_DIR" \
    --skip-validate-spec \
    --global-property models,modelDocs=false,modelTests=false,apis=false,apiDocs=false,apiTests=false,supportingFiles=false

MODELS_DIR="$OUTPUT_DIR/src/main/kotlin/io/ton/walletkit/api/generated"

# Check if models directory exists
if [ ! -d "$MODELS_DIR" ]; then
    echo "❌ Error: Generated models directory not found at '$MODELS_DIR'"
    exit 1
fi

# Copy generated models to destination directory
echo "📁 Copying generated models to destination directory: $DEST_DIR"

# Clean destination before copying new models
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"
cp -R "$MODELS_DIR/"* "$DEST_DIR/"

# Clean up generated directory
echo "🧹 Cleaning up generated directory..."
rm -rf "$OUTPUT_DIR"
