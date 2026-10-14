package com.dungeondelvers.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, long serverTime, T data, ErrorDetail error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, System.currentTimeMillis(), data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(false, System.currentTimeMillis(), null, error);
    }
}
