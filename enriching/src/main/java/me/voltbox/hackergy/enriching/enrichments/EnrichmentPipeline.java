package me.voltbox.hackergy.enriching.enrichments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantDto;
import me.voltbox.hackergy.common.service.DatastoreService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentPipeline {
    private final TextSummarizationService textSummarizationService;
    private final CategoryMappingService categoryMappingService;
    private final DatastoreService datastoreService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(24);

    public void enrichGrant(GrantDto grantDto) {
        if ("ItFoerderungen".equals(grantDto.getSource())) {
            var enrichedGrant = categorize(EnrichedGrantDto.builder().grantDto(grantDto).summary(grantDto.getText()).build());
            enrichEntitiesIfNecessary(enrichedGrant, "Unternehmen");
            datastoreService.insertEnrichedGrant(enrichedGrant);

        } else {
            executorService.submit(() -> {
                var enrichedGrant = categorize(summarizeText(grantDto));
                datastoreService.insertEnrichedGrant(enrichedGrant);
            });
        }
    }

    private void enrichEntitiesIfNecessary(EnrichedGrantDto enrichedGrant, String entity) {
        var eligibleEntities = enrichedGrant.getGrantDto().getEligibleEntities();
        if (eligibleEntities.isEmpty()) {
            eligibleEntities.add(entity);
        }
    }

    public EnrichedGrantDto summarizeText(GrantDto grantDto) {
        var shortText = textSummarizationService.summarize(grantDto.get_id().toString(), grantDto.getText());
        log.info("Enriched grant {} with a short summary.", grantDto.get_id());
        return EnrichedGrantDto.builder().grantDto(grantDto).summary(shortText.orElse("-")).build();
    }

    public EnrichedGrantDto categorize(EnrichedGrantDto grantDto) {
        var category = categoryMappingService.mapCategory(grantDto.getGrantDto());
        log.info("Enriched grant {} with a category.", grantDto.getGrantDto().get_id());
        return EnrichedGrantDto.builder().grantDto(grantDto.getGrantDto()).summary(grantDto.getSummary()).enrichedCategories(category).build();
    }
}
