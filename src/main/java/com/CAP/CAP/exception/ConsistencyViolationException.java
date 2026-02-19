package com.CAP.CAP.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Simula cuando se exige consistencia fuerte pero los nodos difieren
@ResponseStatus(HttpStatus.CONFLICT)
public class ConsistencyViolationException extends RuntimeException {
    public ConsistencyViolationException(String message) {
        super(message);
    }
}