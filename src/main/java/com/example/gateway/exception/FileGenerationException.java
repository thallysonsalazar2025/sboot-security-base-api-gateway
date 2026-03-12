package com.example.gateway.exception;

public class FileGenerationException extends RuntimeException {
    public FileGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileGenerationException(String message) {
        super(message);
    }
}
