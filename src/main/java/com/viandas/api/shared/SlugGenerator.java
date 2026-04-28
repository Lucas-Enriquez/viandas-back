package com.viandas.api.shared;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class SlugGenerator {
	public String slugify(String input) {
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "empresa" : normalized;
	}
}
