package com.CAP.CAP.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Simula que un nodo o la red no está disponible (Tolerancia a Particiones)
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class NetworkPartitionException extends RuntimeException {
    public NetworkPartitionException(String message) {
        super(message);
    }
}