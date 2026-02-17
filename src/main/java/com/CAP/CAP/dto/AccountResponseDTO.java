package com.CAP.CAP.dto;

import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        Double balance,
        Long version
) {}
