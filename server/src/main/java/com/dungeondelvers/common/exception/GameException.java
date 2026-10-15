package com.dungeondelvers.common.exception;

public class GameException extends RuntimeException {

    private final ErrorCode code;
    private final int httpStatus;

    public GameException(ErrorCode code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public static GameException badRequest(ErrorCode code, String message) {
        return new GameException(code, message, 400);
    }

    public static GameException unauthorized(String message) {
        return new GameException(ErrorCode.UNAUTHORIZED, message, 401);
    }

    public static GameException forbidden(String message) {
        return new GameException(ErrorCode.FORBIDDEN, message, 403);
    }

    public static GameException notFound(String message) {
        return new GameException(ErrorCode.NOT_FOUND, message, 404);
    }

    public static GameException tooManyRequests(String message) {
        return new GameException(ErrorCode.RATE_LIMITED, message, 429);
    }

    public static GameException conflict(ErrorCode code, String message) {
        return new GameException(code, message, 409);
    }
}
