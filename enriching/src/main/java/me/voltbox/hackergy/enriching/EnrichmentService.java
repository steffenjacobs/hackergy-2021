package me.voltbox.hackergy.enriching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.service.DatastoreService;
import me.voltbox.hackergy.enriching.enrichments.EnrichmentPipeline;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentService {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(8);
    private static final Set<UUID> ENRICHMENTS_FOR_SCRAPES_CURRENTLY_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    private final DatastoreService datastoreService;
    private final EnrichmentPipeline enrichmentPipeline;

    public boolean triggerEnrichment(UUID scrapeId) {
        if (!ENRICHMENTS_FOR_SCRAPES_CURRENTLY_IN_PROGRESS.add(scrapeId)) {
            return false;
        }
        EXECUTOR_SERVICE.submit(() -> {
            datastoreService.findByScrapeId(scrapeId.toString()).forEach(enrichmentPipeline::enrichGrant);
            log.info("Finished enrichment of scrape {}.", scrapeId);
            ENRICHMENTS_FOR_SCRAPES_CURRENTLY_IN_PROGRESS.remove(scrapeId);
        });
        return true;
    }
}
