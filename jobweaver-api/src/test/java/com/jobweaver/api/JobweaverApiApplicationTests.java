package com.jobweaver.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test – requires running PostgreSQL and Kafka infrastructure")
class JobweaverApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
