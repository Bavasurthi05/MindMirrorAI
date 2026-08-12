package com.project.mentalhealth.infrastructure.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal circuit breaker for calls to the ML service.
 *
 * <p>Deliberately hand-rolled rather than pulling in Resilience4j: the backend has no
 * resilience dependency today and this needs exactly one policy — stop hammering a service
 * that is already failing, and probe it periodically.
 *
 * <p>States: CLOSED (calls pass) → OPEN after {@code failureThreshold} consecutive failures →
 * HALF_OPEN after {@code openDuration}, where a single probe decides whether to close again.
 */
public class MlCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(MlCircuitBreaker.class);

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicReference<Instant> openedAt = new AtomicReference<>(Instant.EPOCH);

    public MlCircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    /** @return true when a call may proceed. */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN && Instant.now().isAfter(openedAt.get().plus(openDuration))) {
            // Let a single probe through to see whether the service recovered.
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                log.info("ML circuit breaker entering HALF_OPEN; probing the ML service");
                return true;
            }
        }
        return current == State.HALF_OPEN && state.get() == State.HALF_OPEN;
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        if (state.getAndSet(State.CLOSED) != State.CLOSED) {
            log.info("ML circuit breaker CLOSED; ML service is responding again");
        }
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold && state.get() != State.OPEN) {
            state.set(State.OPEN);
            openedAt.set(Instant.now());
            log.warn("ML circuit breaker OPEN after {} consecutive failures; "
                    + "skipping ML calls for {}s", failures, openDuration.toSeconds());
        }
    }

    public boolean isOpen() {
        return state.get() == State.OPEN;
    }
}
