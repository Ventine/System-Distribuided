package com.CAP.CAP.controller;

import com.CAP.CAP.service.OrderSagaOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sagas/orders")
public class SagaController {

    private final OrderSagaOrchestrator orchestrator;

    public SagaController(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public record OrderRequest(
        @NotBlank String productId, 
        int quantity, 
        // Parámetro para ingeniería del caos: "NONE", "INVENTORY", "PAYMENT"
        String simulateFailureAt 
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@Valid @RequestBody OrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        log.info("Iniciando Orquestación de Saga para la orden: {}", orderId);
        
        String failurePoint = request.simulateFailureAt() != null ? request.simulateFailureAt().toUpperCase() : "NONE";
        
        // Ejecutamos la Saga
        var sagaResult = orchestrator.executeOrderSaga(orderId, failurePoint);

        boolean isSuccess = "COMPLETED".equals(sagaResult.finalState());
        
        // Construimos una respuesta hiper-detallada
        return ResponseEntity.status(isSuccess ? 200 : 409).body(Map.of(
            "orderId", sagaResult.orderId(),
            "finalSagaState", sagaResult.finalState(),
            "transactionStatus", isSuccess ? "COMMITTED" : "ROLLED_BACK (Compensated)",
            "timestamp", Instant.now(),
            "architecturalNote", isSuccess 
                ? "Todos los microservicios confirmaron sus transacciones locales." 
                : "La orquestación detectó un fallo y ejecutó comandos de compensación para mantener la consistencia eventual.",
            "sagaExecutionLog", sagaResult.executionLog() // Aquí verás la magia paso a paso
        ));
    }
}