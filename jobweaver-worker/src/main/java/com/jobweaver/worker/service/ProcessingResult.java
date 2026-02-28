package com.jobweaver.worker.service;

public record ProcessingResult(
        boolean duplicate,
        boolean success,
        boolean failure,
        String errorMessage
) {

    public static ProcessingResult ofSuccess() {
        return new ProcessingResult(false, true, false, null);
    }

    public static ProcessingResult ofFailure(String msg) {
        return new ProcessingResult(false, false, true, msg);
    }

    public static ProcessingResult ofDuplicateSuccess() {
        return new ProcessingResult(true, true, false, null);
    }

    public static ProcessingResult ofDuplicateFailure(String msg) {
        return new ProcessingResult(true, false, true, msg);
    }

    public static ProcessingResult ofDuplicateRunning() {
        return new ProcessingResult(true, false, false, null);
    }
}