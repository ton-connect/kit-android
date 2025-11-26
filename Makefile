.PHONY: models

# Generate Kotlin API models from the WalletKit OpenAPI spec
models:
	@bash Scripts/generate-api/generate-api-models.sh "$(WALLETKIT_PATH)"
