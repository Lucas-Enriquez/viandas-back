package com.viandas.api.company.dto.response;

import java.math.BigDecimal;

public record GeocodeCandidate(String formattedAddress, BigDecimal latitude, BigDecimal longitude, String provider) {
}
