package me.voltbox.hackergy.scraping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class ScrapingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScrapingApplication.class, args);
	}

	@Scheduled(cron = "0/5 * * * * *")
	public void scrape(){
		log.info("Scraping.");
	}

}
