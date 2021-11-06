package me.voltbox.hackergy.enriching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan("me.voltbox.hackergy")
public class EnrichingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrichingApplication.class, args);
    }
}
