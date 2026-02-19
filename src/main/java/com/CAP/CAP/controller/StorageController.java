package com.CAP.CAP.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CAP.CAP.service.*;
import com.CAP.CAP.exception.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final DistributedStorageService storageService;

    public StorageController(DistributedStorageService storageService) {
        this.storageService = storageService;
    }

    // DTOs embebidos para simplicidad visual
    public record WriteRequest(@NotBlank String key, @NotBlank String value, boolean strongConsistency) {}
    public record DataResponse(String key, String value, String consistencyModel) {}

    @PostMapping
    public ResponseEntity<String> writeData(@Valid @RequestBody WriteRequest request) {
        if (request.strongConsistency()) {
            storageService.writeStrong(request.key(), request.value());
            return ResponseEntity.ok("Write SUCCESS (Strong Consistency). Quorum reached.");
        } else {
            storageService.writeEventual(request.key(), request.value());
            return ResponseEntity.accepted().body("Write ACCEPTED (Eventual Consistency). Replicating in background.");
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<DataResponse> readData(
            @PathVariable String key,
            @RequestParam(defaultValue = "false") boolean strongConsistency) {
        
        String value;
        String model;
        if (strongConsistency) {
            value = storageService.readStrong(key);
            model = "STRONG";
        } else {
            value = storageService.readEventual(key);
            model = "EVENTUAL";
        }
        
        return ResponseEntity.ok(new DataResponse(key, value, model));
    }

    // --- ENDPOINTS DE CAOS (SIMULACIÓN DE FALLOS) ---
    
    @PostMapping("/fault/partition/{status}")
    public ResponseEntity<String> togglePartition(@PathVariable boolean status) {
        storageService.toggleNetworkPartition(status);
        return ResponseEntity.ok("Network partition simulated: " + status);
    }
}