package com.dungeondelvers.common.exception;

import com.dungeondelvers.common.dto.ApiResponse;
import com.dungeondelvers.common.dto.ErrorDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameException.class)
    public ResponseEntity<ApiResponse<Void>> handleGameException(GameException ex) {
        ErrorDetail error = new ErrorDetail(ex.getCode().name(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        ErrorDetail error = new ErrorDetail(ErrorCode.INVALID_INPUT.name(), message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        ErrorDetail error = new ErrorDetail(ErrorCode.INTERNAL_ERROR.name(), "Internal server error");
        return ResponseEntity.internalServerError().body(ApiResponse.fail(error));
    }
}
