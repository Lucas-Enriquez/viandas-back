package com.viandas.api.order.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrderItemCommentRequest(
		@Size(max = 500) String comment
) {}
