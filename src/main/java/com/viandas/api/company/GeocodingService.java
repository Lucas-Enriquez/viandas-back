package com.viandas.api.company;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class GeocodingService {
	public GeocodePreviewResponse preview(GeocodePreviewRequest request) {
		return new GeocodePreviewResponse("NOT_CONFIGURED", List.of());
	}

	public record GeocodePreviewRequest(String address) {
	}

	public record GeocodeCandidate(String formattedAddress, BigDecimal latitude, BigDecimal longitude, String provider) {
	}

	public record GeocodePreviewResponse(String status, List<GeocodeCandidate> candidates) {
	}
}
