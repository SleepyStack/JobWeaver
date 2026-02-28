package com.jobweaver.common.messaging.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationInstructionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("SimulationStep polymorphic deserialization")
    class PolymorphicDeserialization {

        @Test
        @DisplayName("deserializes SleepStep correctly")
        void deserializesSleepStep() throws Exception {
            String json = """
                    {"steps": [{"action": "SLEEP", "durationMs": 2000}]}
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(1);
            assertThat(instruction.steps().getFirst()).isInstanceOf(SleepStep.class);
            SleepStep step = (SleepStep) instruction.steps().getFirst();
            assertThat(step.durationMs()).isEqualTo(2000);
        }

        @Test
        @DisplayName("deserializes LogStep correctly")
        void deserializesLogStep() throws Exception {
            String json = """
                    {"steps": [{"action": "LOG", "message": "hello world"}]}
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(1);
            assertThat(instruction.steps().getFirst()).isInstanceOf(LogStep.class);
            assertThat(((LogStep) instruction.steps().getFirst()).message()).isEqualTo("hello world");
        }

        @Test
        @DisplayName("deserializes ComputeStep correctly")
        void deserializesComputeStep() throws Exception {
            String json = """
                    {"steps": [{"action": "COMPUTE", "iterations": 500000}]}
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(1);
            assertThat(instruction.steps().getFirst()).isInstanceOf(ComputeStep.class);
            assertThat(((ComputeStep) instruction.steps().getFirst()).iterations()).isEqualTo(500000);
        }

        @Test
        @DisplayName("deserializes HttpCallStep correctly")
        void deserializesHttpCallStep() throws Exception {
            String json = """
                    {"steps": [{"action": "HTTP_CALL", "url": "https://example.com", "latencyMs": 1500}]}
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(1);
            assertThat(instruction.steps().getFirst()).isInstanceOf(HttpCallStep.class);
            HttpCallStep step = (HttpCallStep) instruction.steps().getFirst();
            assertThat(step.url()).isEqualTo("https://example.com");
            assertThat(step.latencyMs()).isEqualTo(1500);
        }

        @Test
        @DisplayName("deserializes FailStep correctly")
        void deserializesFailStep() throws Exception {
            String json = """
                    {"steps": [{"action": "FAIL", "message": "simulated crash"}]}
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(1);
            assertThat(instruction.steps().getFirst()).isInstanceOf(FailStep.class);
            assertThat(((FailStep) instruction.steps().getFirst()).message()).isEqualTo("simulated crash");
        }

        @Test
        @DisplayName("deserializes full mixed payload with all step types")
        void deserializesFullPayload() throws Exception {
            String json = """
                    {
                      "steps": [
                        { "action": "SLEEP", "durationMs": 2000 },
                        { "action": "LOG", "message": "hello world" },
                        { "action": "COMPUTE", "iterations": 500000 },
                        { "action": "HTTP_CALL", "url": "https://example.com", "latencyMs": 1500 },
                        { "action": "FAIL", "message": "simulated crash" }
                      ]
                    }
                    """;

            SimulationInstruction instruction = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(instruction.steps()).hasSize(5);
            assertThat(instruction.steps().get(0)).isInstanceOf(SleepStep.class);
            assertThat(instruction.steps().get(1)).isInstanceOf(LogStep.class);
            assertThat(instruction.steps().get(2)).isInstanceOf(ComputeStep.class);
            assertThat(instruction.steps().get(3)).isInstanceOf(HttpCallStep.class);
            assertThat(instruction.steps().get(4)).isInstanceOf(FailStep.class);
        }
    }

    @Nested
    @DisplayName("SimulationStep serialization")
    class Serialization {

        @Test
        @DisplayName("serializes and deserializes round-trip correctly")
        void roundTrip() throws Exception {
            SimulationInstruction original = new SimulationInstruction(List.of(
                    new SleepStep(1000),
                    new LogStep("test message"),
                    new ComputeStep(100),
                    new HttpCallStep("https://example.com", 500),
                    new FailStep("boom")
            ));

            String json = objectMapper.writeValueAsString(original);
            SimulationInstruction deserialized = objectMapper.readValue(json, SimulationInstruction.class);

            assertThat(deserialized.steps()).hasSize(5);
            assertThat(deserialized.steps().get(0)).isEqualTo(original.steps().get(0));
            assertThat(deserialized.steps().get(1)).isEqualTo(original.steps().get(1));
            assertThat(deserialized.steps().get(2)).isEqualTo(original.steps().get(2));
            assertThat(deserialized.steps().get(3)).isEqualTo(original.steps().get(3));
            assertThat(deserialized.steps().get(4)).isEqualTo(original.steps().get(4));
        }

        @Test
        @DisplayName("serialized JSON contains action discriminator")
        void serializedJsonContainsActionField() throws Exception {
            SimulationInstruction instruction = new SimulationInstruction(
                    List.of(new SleepStep(500))
            );

            String json = objectMapper.writeValueAsString(instruction);

            assertThat(json).contains("\"action\":\"SLEEP\"");
            assertThat(json).contains("\"durationMs\":500");
        }
    }

    @Nested
    @DisplayName("SimulationInstruction record")
    class RecordBehavior {

        @Test
        @DisplayName("empty steps list is valid")
        void emptyStepsList() {
            SimulationInstruction instruction = new SimulationInstruction(List.of());
            assertThat(instruction.steps()).isEmpty();
        }

        @Test
        @DisplayName("equals and hashCode work correctly")
        void equalsAndHashCode() {
            SimulationInstruction a = new SimulationInstruction(List.of(new SleepStep(100)));
            SimulationInstruction b = new SimulationInstruction(List.of(new SleepStep(100)));

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }
}
