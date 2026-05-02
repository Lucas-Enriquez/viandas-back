package com.viandas.api.company.dto.response;

import java.util.List;

public record GeocodePreviewResponse(String status, List<GeocodeCandidate> candidates) {
}
