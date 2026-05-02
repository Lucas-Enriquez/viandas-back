package com.viandas.api.company.application;

import java.util.List;

import com.viandas.api.company.dto.request.GeocodePreviewRequest;
import com.viandas.api.company.dto.response.GeocodePreviewResponse;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {
	public GeocodePreviewResponse preview(GeocodePreviewRequest request) {
		return new GeocodePreviewResponse("NOT_CONFIGURED", List.of());
	}

}
