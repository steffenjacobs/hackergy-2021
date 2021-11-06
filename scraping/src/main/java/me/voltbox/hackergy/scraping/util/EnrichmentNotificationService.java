package me.voltbox.hackergy.scraping.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class EnrichmentNotificationService {
    public void notifyEnrichment(UUID scrapeId) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var map = new LinkedMultiValueMap<String, String>();
        map.add("scrapeId", scrapeId.toString());
        new RestTemplate().postForLocation("http://enriching:8080/enrich", new HttpEntity<>(map, headers));
    }
}
