package com.jobweaver.worker.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decodes and executes simulation instructions from the job payload.
 *
 * Expected payload format:
 * {
 *   "steps": [
 *     { "action": "SLEEP",     "durationMs": 2000 },
 *     { "action": "LOG",       "message": "hello world" },
 *     { "action": "COMPUTE",   "iterations": 500000 },
 *     { "action": "HTTP_CALL", "url": "https://example.com", "latencyMs": 1500 },
 *     { "action": "FAIL",      "message": "simulated crash" }
 *   ]
 * }
 */
@Component
public class InstructionExecutor {

    private static final Logger log = LoggerFactory.getLogger(InstructionExecutor.class);

    /**
     * Runs every instruction step sequentially.
     * Throws on FAIL instruction or if the thread is interrupted.
     */
    @SuppressWarnings("unchecked")
    public void execute(Map<String, Object> payload, String jobLabel) throws Exception {
        Object raw = payload.get("steps");
        if (raw == null) {
            log.warn("[{}] No 'steps' found in payload — nothing to do", jobLabel);
            return;
        }

        List<Map<String, Object>> steps = (List<Map<String, Object>>) raw;
        log.info("[{}] Starting execution of {} step(s)", jobLabel, steps.size());

        for (int i = 0; i < steps.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Job was cancelled");
            }

            Map<String, Object> step = steps.get(i);
            String action = ((String) step.get("action")).toUpperCase();
            log.info("[{}] Step {}/{}: {}", jobLabel, i + 1, steps.size(), action);

            switch (action) {
                case "SLEEP"     -> executeSleep(step, jobLabel);
                case "LOG"       -> executeLog(step, jobLabel);
                case "COMPUTE"   -> executeCompute(step, jobLabel);
                case "HTTP_CALL" -> executeHttpCall(step, jobLabel);
                case "FAIL"      -> executeFail(step, jobLabel);
                default          -> log.warn("[{}] Unknown action '{}' — skipping", jobLabel, action);
            }
        }

        log.info("[{}] All steps completed", jobLabel);
    }

    private void executeSleep(Map<String, Object> step, String label) throws InterruptedException {
        long ms = toLong(step.getOrDefault("durationMs", 1000));
        log.info("[{}] SLEEP for {} ms", label, ms);
        Thread.sleep(ms);
    }

    private void executeLog(Map<String, Object> step, String label) {
        String message = (String) step.getOrDefault("message", "<no message>");
        log.info("[{}] LOG: {}", label, message);
    }

    private void executeCompute(Map<String, Object> step, String label) {
        long iterations = toLong(step.getOrDefault("iterations", 100_000));
        log.info("[{}] COMPUTE: {} iterations of busy-work", label, iterations);

        // Simulate CPU-bound work
        double accumulator = 0;
        for (long i = 0; i < iterations; i++) {
            accumulator += Math.sin(i) * Math.cos(i);
            // Periodically check for interruption on long computations
            if (i % 50_000 == 0 && Thread.currentThread().isInterrupted()) {
                log.warn("[{}] COMPUTE interrupted at iteration {}", label, i);
                return;
            }
        }
        log.info("[{}] COMPUTE done (result={})", label, accumulator);
    }

    private void executeHttpCall(Map<String, Object> step, String label) throws InterruptedException {
        String url = (String) step.getOrDefault("url", "https://simulated.local");
        long latencyMs = toLong(step.getOrDefault("latencyMs", 1000));

        // Add ±20% jitter to make it realistic
        long jitter = (long) (latencyMs * 0.2 * (ThreadLocalRandom.current().nextDouble() - 0.5));
        long actualLatency = Math.max(50, latencyMs + jitter);

        log.info("[{}] HTTP_CALL to {} (simulated latency {} ms)", label, url, actualLatency);
        Thread.sleep(actualLatency);
        log.info("[{}] HTTP_CALL to {} complete", label, url);
    }

    private void executeFail(Map<String, Object> step, String label) {
        String message = (String) step.getOrDefault("message", "Simulated failure");
        log.error("[{}] FAIL instruction triggered: {}", label, message);
        throw new RuntimeException("FAIL instruction: " + message);
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }
}
