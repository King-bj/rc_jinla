package com.notifyhub.delivery.governance.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 每个目的地独立计数的内存在途上限，以及内存中的熔断开关。 */
public final class InMemoryAdmissionGuard implements AdmissionGuard {

    private final Map<String, Integer> limits = new HashMap<>();
    private final Map<String, Boolean> criticalFlags = new HashMap<>();
    private final Map<String, Integer> inFlight = new HashMap<>();
    private final Set<String> openCircuits = new HashSet<>();

    public void limit(String destinationId, int maxInFlight) {
        limits.put(destinationId, maxInFlight);
    }

    public void critical(String destinationId, boolean critical) {
        criticalFlags.put(destinationId, critical);
    }

    public void openCircuit(String destinationId) {
        openCircuits.add(destinationId);
    }

    @Override
    public synchronized Admission evaluate(String destinationId) {
        if (openCircuits.contains(destinationId)) {
            boolean critical = criticalFlags.getOrDefault(destinationId, true);
            return critical ? Admission.DEGRADE_RETRY : Admission.DEGRADE_SKIP;
        }
        int limit = limits.getOrDefault(destinationId, Integer.MAX_VALUE);
        int current = inFlight.getOrDefault(destinationId, 0);
        if (current >= limit) {
            return Admission.BULKHEAD_FULL;
        }
        inFlight.put(destinationId, current + 1);
        return Admission.ALLOW;
    }

    @Override
    public synchronized void release(String destinationId) {
        int current = inFlight.getOrDefault(destinationId, 0);
        if (current <= 1) {
            inFlight.remove(destinationId);
            return;
        }
        inFlight.put(destinationId, current - 1);
    }
}
