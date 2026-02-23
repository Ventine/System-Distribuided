package com.CAP.CAP.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OrderSagaOrchestrator {

    // Simula una base de datos de estados de la Saga (Saga Execution Log)
    private final Map<String, String> sagaStates = new ConcurrentHashMap<>();

    public record SagaExecutionResult(String orderId, String finalState, List<String> executionLog) {}

    /**
     * Ejecuta la Saga completa de forma síncrona para propósitos de demostración.
     * En la vida real, cada paso sería asíncrono y reaccionaría a eventos (Kafka/RabbitMQ).
     */
    public SagaExecutionResult executeOrderSaga(String orderId, String simulatedFailurePoint) {
        List<String> logDeEjecucion = new ArrayList<>();
        sagaStates.put(orderId, "PENDING");
        logDeEjecucion.add("[SAGA INICIADA] Orden " + orderId + " en estado PENDING.");

        try {
            // PASO 1: Reservar Inventario
            logDeEjecucion.add(">> Comando enviado: RESERVE_INVENTORY");
            if ("INVENTORY".equalsIgnoreCase(simulatedFailurePoint)) {
                throw new RuntimeException("Fallo catastrófico en el microservicio de Inventario.");
            }
            sagaStates.put(orderId, "INVENTORY_RESERVED");
            logDeEjecucion.add("<< Evento recibido: INVENTORY_RESERVED_SUCCESS");

            // PASO 2: Procesar Pago
            logDeEjecucion.add(">> Comando enviado: PROCESS_PAYMENT");
            if ("PAYMENT".equalsIgnoreCase(simulatedFailurePoint)) {
                throw new RuntimeException("Tarjeta declinada por el microservicio de Pagos.");
            }
            sagaStates.put(orderId, "PAYMENT_PROCESSED");
            logDeEjecucion.add("<< Evento recibido: PAYMENT_PROCESSED_SUCCESS");

            // PASO 3: Finalizar Orden
            sagaStates.put(orderId, "COMPLETED");
            logDeEjecucion.add("[SAGA COMPLETADA] Transacción distribuida finalizada con éxito.");

        } catch (Exception ex) {
            logDeEjecucion.add("[ERROR DETECTADO] " + ex.getMessage());
            logDeEjecucion.add("[INICIANDO COMPENSACIÓN] Ejecutando Rollback Distribuido...");
            
            // Inicia la máquina de estados de compensación
            executeCompensationLogic(orderId, logDeEjecucion);
        }

        return new SagaExecutionResult(orderId, sagaStates.get(orderId), logDeEjecucion);
    }

    /**
     * Lógica de Compensación (El famoso "Rollback" distribuido).
     * Se ejecuta en orden inverso dependiendo del estado en el que se quedó la Saga.
     */
    private void executeCompensationLogic(String orderId, List<String> logDeEjecucion) {
        String currentState = sagaStates.get(orderId);

        switch (currentState) {
            case "PAYMENT_PROCESSED":
                // Si el pago pasó pero falló el paso posterior (imaginemos un paso de envío)
                logDeEjecucion.add(">> Comando de Compensación: REFUND_PAYMENT");
                logDeEjecucion.add("<< Compensación exitosa: Dinero devuelto al cliente.");
                // No hacemos 'break' porque el switch "cae" (fall-through) hacia los pasos anteriores
            case "INVENTORY_RESERVED":
                logDeEjecucion.add(">> Comando de Compensación: RELEASE_INVENTORY");
                logDeEjecucion.add("<< Compensación exitosa: Stock liberado y devuelto a la tienda.");
                break;
            case "PENDING":
                logDeEjecucion.add(">> No hay acciones que compensar, la saga falló en el primer paso.");
                break;
        }

        sagaStates.put(orderId, "COMPENSATED_ABORTED");
        logDeEjecucion.add("[SAGA ABORTADA] Estado final: COMPENSATED_ABORTED. Consistencia restaurada.");
    }
}