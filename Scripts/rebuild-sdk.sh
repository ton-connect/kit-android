#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIT_ANDROID_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
KIT_DIR="${KIT_DIR:-$(cd "$KIT_ANDROID_DIR/../kit" 2>/dev/null && pwd)}"

if [ -z "$KIT_DIR" ] || [ ! -d "$KIT_DIR" ]; then
    echo -e "${RED}Error: kit repo not found. Set KIT_DIR or place kit/ next to kit-android/.${NC}"
    exit 1
fi

echo -e "${YELLOW}=== Rebuild SDK ===${NC}"
echo "kit:         $KIT_DIR"
echo "kit-android: $KIT_ANDROID_DIR"

# 1. Build TypeScript bridge bundle
echo -e "\n${GREEN}[1/5] Building TypeScript bridge...${NC}"

cd "$KIT_DIR"
pnpm build --force --filter walletkit-android-bridge

# 2. Copy bundle
echo -e "\n${GREEN}[2/5] Copying bundle to dist-android...${NC}"
mkdir -p "$KIT_ANDROID_DIR/dist-android/"
cp packages/walletkit-android-bridge/dist/* "$KIT_ANDROID_DIR/dist-android/"

# 3. Regenerate OpenAPI models (opt-in)
if [[ "$*" == *"--regen-models"* ]]; then
    echo -e "\n${GREEN}[3/5] Regenerating OpenAPI models...${NC}"
    "$KIT_ANDROID_DIR/Scripts/generate-api/generate-api-models.sh" "$KIT_DIR/packages/walletkit"
    cd "$KIT_ANDROID_DIR/TONWalletKit-Android"
    ./gradlew spotlessApply
else
    echo -e "\n${YELLOW}[3/5] Skipping model regeneration (pass --regen-models to regenerate)${NC}"
fi

# 4. Build SDK AAR
echo -e "\n${GREEN}[4/5] Building SDK and copying to demo libs...${NC}"
cd "$KIT_ANDROID_DIR/TONWalletKit-Android"
./gradlew syncWalletKitWebViewAssets buildAndCopyToDemo --rerun-tasks

# 5. Install demo app (opt-in)
if [[ "$*" == *"--install-demo"* ]]; then
    echo -e "\n${GREEN}[5/5] Installing demo app...${NC}"
    cd "$KIT_ANDROID_DIR/AndroidDemo"
    ./gradlew :app:clean :app:installDebug --rerun-tasks
else
    echo -e "\n${YELLOW}[5/5] Skipping demo install (pass --install-demo to install)${NC}"
fi

echo -e "\n${GREEN}=== Done ===${NC}"
