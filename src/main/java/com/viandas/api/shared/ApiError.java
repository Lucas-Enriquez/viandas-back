package com.viandas.api.shared;

public record ApiError(
        String field,
        String message
) {
    public ApiError(String message) {
        this(null, message);
    }
}
