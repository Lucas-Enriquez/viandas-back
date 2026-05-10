package com.viandas.api.shared.photo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.viandas.api.shared.ApiException;

/**
 * Wraps Cloudinary configuration so the API can:
 * <ul>
 *     <li>Issue short-lived <em>signed direct upload</em> payloads — clients upload the file
 *         straight to Cloudinary, the bytes never touch the backend.</li>
 *     <li>Validate that a {@code publicId} returned by the client actually lives under the
 *         folder this service is configured to allow (so a client can't claim an arbitrary
 *         Cloudinary asset belongs to the app).</li>
 *     <li>Build the canonical secure delivery URL for a {@code publicId}.</li>
 * </ul>
 *
 * <p>If Cloudinary credentials are not configured (e.g. local dev without env vars),
 * {@link #isConfigured()} returns false and signing/URL generation throw
 * {@link ApiException#conflict(String)}. Validation of an empty/null publicId still works.</p>
 */
@Service
public class PhotoUploadService {

	private final Cloudinary cloudinary;
	private final String cloudName;
	private final String apiKey;
	private final String apiSecret;
	private final String productsFolder;
	private final boolean configured;

	public PhotoUploadService(
			@Value("${viandas.cloudinary.cloud-name:}") String cloudName,
			@Value("${viandas.cloudinary.api-key:}") String apiKey,
			@Value("${viandas.cloudinary.api-secret:}") String apiSecret,
			@Value("${viandas.cloudinary.products-folder:viandas/products}") String productsFolder
	) {
		this.cloudName = cloudName;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.productsFolder = trimTrailingSlash(productsFolder);
		this.configured = !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
		this.cloudinary = configured
				? new Cloudinary(ObjectUtils.asMap(
						"cloud_name", cloudName,
						"api_key", apiKey,
						"api_secret", apiSecret,
						"secure", true))
				: null;
	}

	public boolean isConfigured() {
		return configured;
	}

	public String getProductsFolder() {
		return productsFolder;
	}

	/**
	 * Generates a short-lived signature the client uses to upload directly to Cloudinary.
	 * The folder is fixed server-side so clients can't upload outside the allowed area.
	 */
	public UploadSignature signProductUpload() {
		requireConfigured();
		long timestamp = System.currentTimeMillis() / 1000L;
		Map<String, Object> params = new HashMap<>();
		params.put("timestamp", timestamp);
		params.put("folder", productsFolder);
		// 2 = SHA-256 signing (default and recommended in Cloudinary SDK 2.x).
		// El cliente debe enviar el header signature_algorithm=sha256 (Cloudinary lo asume).
		String signature = cloudinary.apiSignRequest(params, apiSecret, 2);
		return new UploadSignature(
				cloudName,
				apiKey,
				timestamp,
				productsFolder,
				signature,
				"https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
	}

	/**
	 * Verifies that {@code publicId} points to an asset under the configured products folder.
	 * Null/blank input is treated as "no photo" and returns silently.
	 */
	public void validateProductPublicId(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		String prefix = productsFolder + "/";
		if (!publicId.startsWith(prefix)) {
			throw ApiException.badRequest("photoPublicId no pertenece a la carpeta de productos");
		}
		// avoid trivially traversing folders
		if (publicId.contains("..")) {
			throw ApiException.badRequest("photoPublicId invalido");
		}
	}

	/**
	 * Builds the canonical secure delivery URL for a Cloudinary publicId.
	 * Returns null for null/blank input (so callers can map empty → empty).
	 */
	public String buildDeliveryUrl(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return null;
		}
		requireConfigured();
		return cloudinary.url().secure(true).generate(publicId);
	}

	private void requireConfigured() {
		if (!configured) {
			throw ApiException.conflict("Subida de fotos no configurada en el servidor");
		}
	}

	private static String trimTrailingSlash(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	public record UploadSignature(
			String cloudName,
			String apiKey,
			long timestamp,
			String folder,
			String signature,
			String uploadUrl) {
	}
}
