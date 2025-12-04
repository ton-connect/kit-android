#!/bin/bash

set -e  # Exit on error

# Get the script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Get the project root (parent of Scripts folder)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Resolve walletkit path using the resolver script
echo "Resolving walletkit path..."
WALLETKIT_PATH=${1}

if [ -z "$WALLETKIT_PATH" ]; then
    echo "Error: Failed to resolve walletkit path"
    echo "Usage: ./generate-api-models.sh <path-to-walletkit>"
    echo "Example: ./generate-api-models.sh /path/to/kit/packages/walletkit"
    exit 1
fi

echo "Walletkit path: $WALLETKIT_PATH"
cd "$WALLETKIT_PATH"

# Run pnpm build
echo "Running pnpm generate-openapi-spec..."
if ! command -v pnpm &> /dev/null; then
    echo "Error: pnpm is not installed. Please install it first."
    echo "Run: npm install -g pnpm"
    exit 1
fi

if ! command -v openapi-generator &> /dev/null; then
    echo "Error: openapi-generator is not installed. Install it via Homebrew or npm."
    exit 1
fi

pnpm install

OPENAPI_SPEC=$(pnpm generate-openapi-spec 2>&1 | grep -oE '/.*walletkit-openapi\.json' | tail -1)
echo "OpenAPI spec path: $OPENAPI_SPEC"

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

PATCHED_SPEC="$(mktemp -t walletkit-openapi.patched.XXXXXX)".json
echo "🧩 Applying OpenAPI patches for discriminated unions..."
python3 - "$OPENAPI_SPEC" "$PATCHED_SPEC" <<'PY'
import json
import sys

source, target = sys.argv[1:3]

with open(source, "r", encoding="utf-8") as f:
    data = json.load(f)

components_schemas = data.get("components", {}).get("schemas", {}) or {}
ref_prefix = "#/components/schemas"


def _rewrite_refs(node):
    if isinstance(node, dict):
        return {k: _rewrite_refs(v) for k, v in node.items()}
    if isinstance(node, list):
        return [_rewrite_refs(item) for item in node]
    if isinstance(node, str) and "#/definitions/" in node:
        return node.replace("#/definitions/", "#/components/schemas/")
    return node

# Normalize legacy refs to components/schemas
data = _rewrite_refs(data)
schemas = components_schemas

def add_discriminated_union(name, discriminator, cases, description=None):
    mapped_cases = []
    for case in cases:
        enum_name = case.get("enumName", case["value"])
        schema_name = case["schema"]
        mapped_cases.append({
            "name": case["name"],
            "value": case["value"],
            "schema": schema_name,
            "dataType": schema_name,
            "enumName": enum_name,
            "wrapperClass": case.get("wrapperClass", f"{schema_name}Variant")
        })

    schemas[name] = {
        "oneOf": [{"$ref": f"{ref_prefix}/{case['schema']}"} for case in cases],
        "discriminator": {
            "propertyName": discriminator,
            "mapping": {case["value"]: f"{ref_prefix}/{case['schema']}" for case in cases}
        },
        "type": "object",
        "required": [discriminator],
        "x-kotlin-discriminated-union": True,
        "x-kotlin-discriminator": discriminator,
        "x-kotlin-cases": mapped_cases,
    }
    if description:
        schemas[name]["description"] = description

# Ensure SignDataPayload uses a named union schema
payload_schema = schemas.get("SignDataPayload")
if payload_schema and "properties" in payload_schema:
    payload_schema["properties"]["value"] = {"$ref": f"{ref_prefix}/SignDataPayloadValue"}

add_discriminated_union(
    "SignDataPayloadValue",
    "type",
    [
        {"name": "text", "value": "text", "schema": "SignDataPayloadText"},
        {"name": "binary", "value": "binary", "schema": "SignDataPayloadBinary"},
        {"name": "cell", "value": "cell", "schema": "SignDataPayloadCell"},
    ],
    description="Payload variants for sign data requests"
)

# Replace SignDataPreview with a discriminated union on 'kind'
add_discriminated_union(
    "SignDataPreview",
    "kind",
    [
        {"name": "text", "value": "text", "schema": "SignDataPreviewText"},
        {"name": "binary", "value": "binary", "schema": "SignDataPreviewBinary"},
        {"name": "cell", "value": "cell", "schema": "SignDataPreviewCell"},
    ],
    description="Preview data for signing"
)

with open(target, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
PY
OPENAPI_SPEC="$PATCHED_SPEC"

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
