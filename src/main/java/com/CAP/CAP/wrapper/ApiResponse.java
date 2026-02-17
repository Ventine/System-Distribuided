package com.CAP.CAP.wrapper;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        String strategy,      // CP o AP
        boolean partitioned,
        T data,
        Instant timestamp
) {}
