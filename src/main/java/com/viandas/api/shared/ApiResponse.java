package com.viandas.api.shared;

import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<ApiError> errors,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, data, message, null, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, message, null, meta);
    }

    public static ApiResponse<Void> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, null, message, errors, null);
    }

    public static ApiResponse<Void> error(String message, List<ApiError> errors, Map<String, Object> meta) {
        return new ApiResponse<>(false, null, message, errors, meta);
    }
}
