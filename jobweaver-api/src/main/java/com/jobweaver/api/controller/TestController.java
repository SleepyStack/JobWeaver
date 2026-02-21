package com.jobweaver.api.controller;

import com.jobweaver.api.kafka.JobEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final JobEventProducer jobEventProducer;
    @PostMapping
    public String test(){
        jobEventProducer.sendMessage(UUID.randomUUID());
        return "sent";
    }
}
