package com.jobweaver.common.messaging.simulation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "action"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SleepStep.class, name = "SLEEP"),
        @JsonSubTypes.Type(value = LogStep.class, name = "LOG"),
        @JsonSubTypes.Type(value = ComputeStep.class, name = "COMPUTE"),
        @JsonSubTypes.Type(value = HttpCallStep.class, name = "HTTP_CALL"),
        @JsonSubTypes.Type(value = FailStep.class, name = "FAIL")
})

/**
{
 EXAMPLE PAYLOAD -
  "steps": [
    { "action": "SLEEP", "durationMs": 2000 },
    { "action": "LOG", "message": "hello world" },
    { "action": "COMPUTE", "iterations": 500000 },
    { "action": "HTTP_CALL", "url": "https://example.com", "latencyMs": 1500 },
    { "action": "FAIL", "message": "simulated crash" }
  ]
}
 */

public sealed interface SimulationStep
        permits SleepStep, LogStep, ComputeStep, HttpCallStep, FailStep {
}
