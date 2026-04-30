#!/bin/bash
# Generates Kotlin test-scoped models from the walletkit model-patterns fixture.
#
# Mirrors the real `generate-api-models.sh` pipeline but:
#  * feeds `__fixtures__/model-patterns.fixture.ts` directly into generate-json-schema.js
#    (the fixture is excluded from the normal TS build, so the usual OpenAPI extraction
#     path skips it)
#  * writes the generated Kotlin models into a test-only package
#    (`io.ton.walletkit.api.generatedtest`) under `api/src/test/java/`
#
# This output is consumed by GeneratedModelsTest.kt, which asserts the generated
# Kotlin shape matches each fixture pattern. Run this script whenever fixtures
# or the generator change.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

WALLETKIT_PATH=${1}
# Second arg (optional) overrides where the generated .kt files are copied.
# When unset, output goes to the committed snapshot under api/src/test/java/.
# GeneratedModelsSnapshotTest passes a build-dir path here so it can diff a
# fresh generation against the committed snapshot without overwriting it.
DEST_DIR_OVERRIDE=${2}
if [ -z "$WALLETKIT_PATH" ]; then
    echo "Error: walletkit path not provided"
    echo "Usage: ./generate-test-models.sh <path-to-walletkit> [output-dir]"
    echo "Example: ./generate-test-models.sh /path/to/kit/packages/walletkit"
    exit 1
fi
echo "Walletkit path: $WALLETKIT_PATH"

if ! command -v pnpm &> /dev/null; then
    echo "Error: pnpm is not installed. Run: npm install -g pnpm"
    exit 1
fi
if ! command -v openapi-generator &> /dev/null; then
    echo "Error: openapi-generator is not installed. Install via Homebrew or npm."
    exit 1
fi
if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Install via Homebrew (\`brew install jq\`) or apt (\`apt-get install jq\`)."
    exit 1
fi

FIXTURE_PATH="$WALLETKIT_PATH/src/api/scripts/__fixtures__/model-patterns.fixture.ts"
JSON_SCHEMA_SCRIPT="$WALLETKIT_PATH/src/api/scripts/generate-json-schema.js"
OPENAPI_SCRIPT="$WALLETKIT_PATH/src/api/scripts/json-schema-to-openapi-spec.js"

if [ ! -f "$FIXTURE_PATH" ]; then
    echo "❌ Error: fixture not found at '$FIXTURE_PATH'"
    exit 1
fi

cd "$WALLETKIT_PATH"
echo "📦 Installing walletkit deps (if needed)..."
pnpm install --silent

OUTPUT_DIR="${SCRIPT_DIR}/generated/test-openapi"
CONFIG_FILE="${SCRIPT_DIR}/generate-api-models-config.json"
TEMPLATES_DIR="${SCRIPT_DIR}/templates"
DEST_DIR="${DEST_DIR_OVERRIDE:-${PROJECT_ROOT}/TONWalletKit-Android/api/src/test/java/io/ton/walletkit/api/generatedtest}"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

TEMP_SCHEMA="$OUTPUT_DIR/fixture-schema.json"
OPENAPI_SPEC="$OUTPUT_DIR/fixture-openapi.json"

# Derive a config without `modelNamePrefix` / `modelNameMappings` for the fixture run.
# The real api config sets `"modelNamePrefix": "TON"` (correct for the published SDK), and
# the test fixture runs in a non-prefixed test package — relying on
# `--additional-properties=modelNamePrefix=` to override the config has proved version-
# fragile (some openapi-generator builds ignore an empty additional-property override).
# Removing the keys at the source eliminates the precedence question entirely.
TEST_CONFIG_FILE="$OUTPUT_DIR/generate-test-models-config.json"
jq 'del(.modelNamePrefix) | del(.modelNameMappings)' "$CONFIG_FILE" > "$TEST_CONFIG_FILE"

echo "📝 Step 1: JSON Schema from fixture..."
node "$JSON_SCHEMA_SCRIPT" "$FIXTURE_PATH" "$TEMP_SCHEMA"

echo "📝 Step 2: JSON Schema → OpenAPI..."
NODE_PATH=$(npm root -g) node "$OPENAPI_SCRIPT" "$TEMP_SCHEMA" "$OPENAPI_SPEC"

echo "🔨 Step 3: Kotlin model codegen..."
openapi-generator generate \
    -i "$OPENAPI_SPEC" \
    -g kotlin \
    -o "$OUTPUT_DIR" \
    -c "$TEST_CONFIG_FILE" \
    -t "$TEMPLATES_DIR" \
    --package-name "io.ton.walletkit.api.generatedtest" \
    --model-package "io.ton.walletkit.api.generatedtest" \
    --type-mappings number=kotlin.Int \
    --skip-validate-spec \
    --global-property models,modelDocs=false,modelTests=false,apis=false,apiDocs=false,apiTests=false,supportingFiles=false

MODELS_DIR="$OUTPUT_DIR/src/main/kotlin/io/ton/walletkit/api/generatedtest"
if [ ! -d "$MODELS_DIR" ]; then
    echo "❌ Error: generated models dir not found at '$MODELS_DIR'"
    exit 1
fi

echo "📁 Copying generated test models to: $DEST_DIR"
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"
cp -R "$MODELS_DIR/"* "$DEST_DIR/"

echo "🧹 Removing empty / boilerplate-only files..."
find "$DEST_DIR" -name '*.kt' -type f -empty -delete
find "$DEST_DIR" -name '*.kt' -type f | while read -r file; do
    if ! grep -qE '^\s*(class |data class |sealed class |object |interface |typealias |enum class |fun |val |var |abstract )' "$file"; then
        echo "  Removing boilerplate-only file: $(basename "$file")"
        rm "$file"
    fi
done
find "$DEST_DIR" -type d -empty -delete

echo "🧹 Cleaning up work dir..."
rm -rf "$OUTPUT_DIR"

echo ""
echo "✅ Test models generated at: $DEST_DIR"
echo "   Run the GeneratedModelsTest suite to validate them."
