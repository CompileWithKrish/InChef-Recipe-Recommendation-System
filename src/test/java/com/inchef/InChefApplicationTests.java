package com.inchef;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
class InChefApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context, entities, repositories, and AOP beans bootstrap cleanly
    }
}
