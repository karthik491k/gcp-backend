package com.logancee.globalcart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getStatusCode().toString());
        body.put("message", ex.getReason());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        String msg = ex.getMessage() != null ? ex.getMessage() : "An error occurred";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (msg.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (msg.toLowerCase().contains("unauthorized") || msg.toLowerCase().contains("credentials")) {
            status = HttpStatus.UNAUTHORIZED;
        }
        body.put("error", status.getReasonPhrase());
        body.put("message", msg);
        return new ResponseEntity<>(body, status);
    }
}
