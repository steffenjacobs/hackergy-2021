package me.voltbox.hackergy.scraping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
@ComponentScan("me.voltbox.hackergy")
public class ScrapingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrapingApplication.class, args);
    }
}
