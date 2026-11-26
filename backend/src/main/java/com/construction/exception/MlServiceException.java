package com.construction.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the YOLO11 FastAPI ML microservice encounters communication,
 * authentication, timeout, or model inference errors.
 */
public class MlServiceException extends RuntimeException {

    private final HttpStatus status;

    public MlServiceException(String message) {
        super(message);
        this.status = HttpStatus.BAD_GATEWAY;
    }

    public MlServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
    }

    public MlServiceException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_GATEWAY;
    }

    public MlServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
