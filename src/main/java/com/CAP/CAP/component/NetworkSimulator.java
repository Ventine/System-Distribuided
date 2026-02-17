package com.CAP.CAP.component;

import org.springframework.stereotype.Component;

@Component
public class NetworkSimulator {

    private volatile boolean partitioned = false;

    public void enablePartition() {
        partitioned = true;
    }

    public void disablePartition() {
        partitioned = false;
    }

    public boolean isPartitioned() {
        return partitioned;
    }
}
