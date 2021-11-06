package me.voltbox.hackergy.enriching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EnrichmentController {

    private final EnrichmentService enrichmentService;

    @PostMapping(value = "/enrich")
    public ResponseEntity<String> enrich(@RequestParam("scrapeId") UUID scrapeId) {
        if (enrichmentService.triggerEnrichment(scrapeId)) {
            log.info("Starting enrichment for scrapeId {}.", scrapeId);
            return ResponseEntity.accepted().build();

        } else {

            log.warn("Enrichment for scrapeId {} already in progress.", scrapeId);
            return ResponseEntity.badRequest().body("Enrichment already in progress");
        }
    }
}
