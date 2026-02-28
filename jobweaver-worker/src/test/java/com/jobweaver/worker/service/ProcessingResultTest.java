package com.jobweaver.worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingResultTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("ofSuccess creates non-duplicate success result")
        void ofSuccess() {
            ProcessingResult result = ProcessingResult.ofSuccess();
            assertThat(result.success()).isTrue();
            assertThat(result.failure()).isFalse();
            assertThat(result.duplicate()).isFalse();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("ofFailure creates non-duplicate failure result")
        void ofFailure() {
            ProcessingResult result = ProcessingResult.ofFailure("error msg");
            assertThat(result.success()).isFalse();
            assertThat(result.failure()).isTrue();
            assertThat(result.duplicate()).isFalse();
            assertThat(result.errorMessage()).isEqualTo("error msg");
        }

        @Test
        @DisplayName("ofDuplicateSuccess creates duplicate success result")
        void ofDuplicateSuccess() {
            ProcessingResult result = ProcessingResult.ofDuplicateSuccess();
            assertThat(result.success()).isTrue();
            assertThat(result.failure()).isFalse();
            assertThat(result.duplicate()).isTrue();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("ofDuplicateFailure creates duplicate failure result")
        void ofDuplicateFailure() {
            ProcessingResult result = ProcessingResult.ofDuplicateFailure("prev error");
            assertThat(result.success()).isFalse();
            assertThat(result.failure()).isTrue();
            assertThat(result.duplicate()).isTrue();
            assertThat(result.errorMessage()).isEqualTo("prev error");
        }

        @Test
        @DisplayName("ofDuplicateRunning creates duplicate running result")
        void ofDuplicateRunning() {
            ProcessingResult result = ProcessingResult.ofDuplicateRunning();
            assertThat(result.success()).isFalse();
            assertThat(result.failure()).isFalse();
            assertThat(result.duplicate()).isTrue();
            assertThat(result.errorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Record equality")
    class RecordEquality {

        @Test
        @DisplayName("identical results are equal")
        void identicalAreEqual() {
            assertThat(ProcessingResult.ofSuccess()).isEqualTo(ProcessingResult.ofSuccess());
        }

        @Test
        @DisplayName("different results are not equal")
        void differentAreNotEqual() {
            assertThat(ProcessingResult.ofSuccess()).isNotEqualTo(ProcessingResult.ofFailure("err"));
        }
    }
}
