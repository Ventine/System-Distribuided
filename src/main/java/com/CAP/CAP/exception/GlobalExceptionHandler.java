package com.CAP.CAP.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.CAP.CAP.wrapper.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {

        ApiResponse<Void> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                null,
                false,
                null,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(NetworkPartitionException.class)
    public ProblemDetail handlePartition(NetworkPartitionException ex, WebRequest request) {
        // CORRECCIÓN: Usamos HttpStatus.SERVICE_UNAVAILABLE en lugar de 503
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Network Partition Detected");
        problem.setProperty("timestamp", Instant.now());
        
        // El header 'traceparent' viene del estándar W3C para Tracing Distribuido (OpenTelemetry)
        String traceId = request.getHeader("traceparent");
        if (traceId != null) {
            problem.setProperty("traceId", traceId); 
        }
        
        return problem;
    }

    @ExceptionHandler(ConsistencyViolationException.class)
    public ProblemDetail handleConsistency(ConsistencyViolationException ex, WebRequest request) {
        // CORRECCIÓN: Usamos HttpStatus.CONFLICT en lugar de 409
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Consistency Violation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
