package com.CAP.CAP.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CAP.CAP.service.*;
import com.CAP.CAP.exception.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final DistributedStorageService storageService;

    public StorageController(DistributedStorageService storageService) {
        this.storageService = storageService;
    }

    // DTOs embebidos
    public record WriteRequest(@NotBlank String key, @NotBlank String value, boolean strongConsistency) {}
    
    // Mejoramos el DTO de respuesta de lectura para incluir metadatos de la operación
    public record DataResponse(
        String key, 
        String value, 
        String consistencyModel, 
        String timestamp,
        String architecturalDetails
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> writeData(@Valid @RequestBody WriteRequest request) {
        if (request.strongConsistency()) {
            storageService.writeStrong(request.key(), request.value());
            // Respuesta detallada para Consistencia Fuerte
            return ResponseEntity.ok(Map.of(
                "status", "ÉXITO",
                "consistency", "FUERTE (Síncrona)",
                "message", "El dato fue escrito en el nodo local y replicado exitosamente a todos los nodos.",
                "details", "Se alcanzó el quórum requerido (W=3). Si un nodo cae ahora, no hay pérdida de datos.",
                "timestamp", Instant.now()
            ));
        } else {
            storageService.writeEventual(request.key(), request.value());
            // Respuesta detallada para Consistencia Eventual
            return ResponseEntity.accepted().body(Map.of(
                "status", "ACEPTADO",
                "consistency", "EVENTUAL (Asíncrona)",
                "message", "El dato fue guardado rápidamente en el nodo local.",
                "details", "La replicación hacia los nodos secundarios se está ejecutando en segundo plano para garantizar baja latencia en la respuesta.",
                "timestamp", Instant.now()
            ));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<DataResponse> readData(
            @PathVariable String key,
            @RequestParam(defaultValue = "false") boolean strongConsistency) {
        
        String value;
        String model;
        String details;

        if (strongConsistency) {
            value = storageService.readStrong(key);
            model = "CONSISTENCIA FUERTE";
            details = "Lectura verificada contra todos los nodos del clúster (R=3). Se garantiza que el valor devuelto es la última versión absoluta.";
        } else {
            value = storageService.readEventual(key);
            model = "CONSISTENCIA EVENTUAL";
            details = "Lectura rápida servida únicamente desde el nodo local (R=1). Advertencia: Podría devolver un valor desactualizado si existen replicaciones pendientes en la red.";
        }
        
        return ResponseEntity.ok(new DataResponse(
            key, 
            value, 
            model, 
            Instant.now().toString(),
            details
        ));
    }

    // --- ENDPOINTS DE CAOS (SIMULACIÓN DE FALLOS) ---
    
    @PostMapping("/fault/partition/{status}")
    public ResponseEntity<Map<String, String>> togglePartition(@PathVariable boolean status) {
        storageService.toggleNetworkPartition(status);
        
        String estadoActual = status 
            ? "ACTIVADA (Nodos aislados, clúster degradado)" 
            : "DESACTIVADA (Red normalizada, clúster saludable)";
            
        String impacto = status 
            ? "Las operaciones que exijan Consistencia Fuerte fallarán. Las operaciones con Consistencia Eventual seguirán funcionando (Alta Disponibilidad)." 
            : "Todas las operaciones de lectura y escritura operan con normalidad en todo el clúster.";
        
        return ResponseEntity.ok(Map.of(
            "action", "Simulación de Partición de Red (Teorema CAP)",
            "networkStatus", estadoActual,
            "architecturalImpact", impacto
        ));
    }
}