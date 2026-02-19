package com.CAP.CAP.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.CAP.CAP.exception.*;

@Slf4j
@Service
public class DistributedStorageService {

    // Simulamos 3 nodos en un clúster
    private final Map<String, String> localNode = new ConcurrentHashMap<>();
    private final Map<String, String> replicaNodeA = new ConcurrentHashMap<>();
    private final Map<String, String> replicaNodeB = new ConcurrentHashMap<>();

    // Interruptor para simular un fallo en la red (Partición)
    private volatile boolean networkPartitionActive = false;

    public void toggleNetworkPartition(boolean status) {
        this.networkPartitionActive = status;
        log.warn("Network Partition is now: {}", status ? "ACTIVE (Nodes Isolated)" : "RESOLVED");
    }

    /**
     * ESCRITURA CON CONSISTENCIA EVENTUAL
     * Escribe en el nodo local rápido y delega la replicación en un hilo en background.
     */
    public void writeEventual(String key, String value) {
        log.info("Writing to local node: [{}={}]", key, value);
        localNode.put(key, value);
        
        // Dispara replicación asíncrona sin bloquear al cliente
        replicateAsync(key, value);
    }

    /**
     * ESCRITURA CON CONSISTENCIA FUERTE (Simulación de Quórum W=3)
     * Debe escribir en todos los nodos o falla.
     */
    public void writeStrong(String key, String value) {
        if (networkPartitionActive) {
            throw new NetworkPartitionException("Cannot achieve Strong Consistency: Network Partition active. Replicas unreachable.");
        }
        
        log.info("Performing Synchronous Strong Write to all nodes: [{}={}]", key, value);
        localNode.put(key, value);
        replicaNodeA.put(key, value);
        replicaNodeB.put(key, value);
    }

    /**
     * LECTURA CON CONSISTENCIA EVENTUAL
     * Devuelve lo que tenga el nodo local, sea o no la versión más reciente.
     */
    public String readEventual(String key) {
        log.info("Eventual Read requested for key: {}", key);
        return localNode.getOrDefault(key, "NOT_FOUND");
    }

    /**
     * LECTURA CON CONSISTENCIA FUERTE (Simulación de Quórum R=3)
     * Verifica que todos los nodos tengan el mismo dato.
     */
    public String readStrong(String key) {
        if (networkPartitionActive) {
            throw new NetworkPartitionException("Cannot verify Strong Consistency: Replicas are unreachable.");
        }

        String localVal = localNode.get(key);
        String repAVal = replicaNodeA.get(key);
        String repBVal = replicaNodeB.get(key);

        if (localVal != null && localVal.equals(repAVal) && localVal.equals(repBVal)) {
            return localVal;
        } else {
            throw new ConsistencyViolationException("Data divergence detected across nodes. Quorum failed.");
        }
    }

    @Async
    protected CompletableFuture<Void> replicateAsync(String key, String value) {
        if (networkPartitionActive) {
            log.error("Async Replication FAILED for key {}. Network is partitioned.", key);
            // En la vida real, aquí guardaríamos el evento en un Dead Letter Queue o Outbox Pattern
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Simulamos latencia de red
            Thread.sleep(2000); 
            replicaNodeA.put(key, value);
            replicaNodeB.put(key, value);
            log.info("Async Replication SUCCESSFUL for key {}. All nodes eventually consistent.", key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture(null);
    }
}