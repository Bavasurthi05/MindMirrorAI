package com.project.mentalhealth.interfaces.api.v1.common;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final int status;
    private final Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .status(200)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(T data, String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }
}
