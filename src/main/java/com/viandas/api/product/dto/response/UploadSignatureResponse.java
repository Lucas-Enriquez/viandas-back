package com.viandas.api.product.dto.response;

public record UploadSignatureResponse(
		String cloudName,
		String apiKey,
		long timestamp,
		String folder,
		String signature,
		String uploadUrl) {
}
