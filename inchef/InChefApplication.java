package com.inchef;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class InChefApplication {

    public static void main(String[] args) {
        SpringApplication.run(InChefApplication.class, args);
    }
}
