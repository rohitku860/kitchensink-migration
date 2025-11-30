package com.kitchensink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KitchensinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(KitchensinkApplication.class, args);
    }
}

