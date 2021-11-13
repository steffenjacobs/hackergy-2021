package me.voltbox.hackergy.enriching.enrichments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantDto;
import me.voltbox.hackergy.common.service.DatastoreService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
            if (StringUtils.isEmpty(grantDto.getContact())) {
                grantDto = grantDto.withContact("");
            }
            if (StringUtils.isEmpty(grantDto.getSponsor())) {
                grantDto = grantDto.withSponsor("");
            }
            if (grantDto.getType() == null || grantDto.getType().isEmpty()) {
                grantDto = grantDto.withType(List.of("Zuschuss"));
            }
            if (StringUtils.isEmpty(grantDto.getEligibleRegion())) {
                grantDto = grantDto.withEligibleRegion("bundesweit");
            }
            var enrichedGrant = categorize(EnrichedGrantDto.builder().grantDto(grantDto).summary(grantDto.getText()).build());
            enrichEntitiesIfNecessary(enrichedGrant, "Unternehmen");
            datastoreService.insertEnrichedGrant(enrichedGrant);
        } else if ("EnergieagenturRLP".equals(grantDto.getSource())) {
            if (StringUtils.isEmpty(grantDto.getEligibleRegion())) {
                grantDto = grantDto.withEligibleRegion("Rheinland-Pfalz");
            }
            if (StringUtils.isEmpty(grantDto.getContact())) {
                grantDto = grantDto.withContact("");
            }
            if (StringUtils.isEmpty(grantDto.getSponsor())) {
                grantDto = grantDto.withSponsor("");
            }
            if (grantDto.getType() == null || grantDto.getType().isEmpty()) {
                grantDto = grantDto.withType(List.of("Zuschuss"));
            }
            if (StringUtils.isEmpty(grantDto.getEligibleRegion())) {
                grantDto = grantDto.withEligibleRegion("bundesweit");
            }
        }
        var dto = grantDto;
        executorService.submit(() -> {
            var enrichedGrant = categorize(summarizeText(dto));
            datastoreService.insertEnrichedGrant(enrichedGrant);
        });
    }

    private void enrichEntitiesIfNecessary(EnrichedGrantDto enrichedGrant, String entity) {
        var eligibleEntities = enrichedGrant.getGrantDto().getEligibleEntities();
        if (eligibleEntities == null) {
            eligibleEntities = new ArrayList<>();
        }
        if (eligibleEntities.isEmpty()) {
            eligibleEntities.add(entity);
        }
        enrichedGrant.getGrantDto().setEligibleEntities(eligibleEntities);
    }

    public EnrichedGrantDto summarizeText(GrantDto grantDto) {
        var shortText = textSummarizationService.summarize(grantDto.get_id(), grantDto.getText());
        log.info("Enriched grant {} with a short summary.", grantDto.get_id());
        return EnrichedGrantDto.builder().grantDto(grantDto).summary(shortText.orElse("-")).build();
    }

    public EnrichedGrantDto categorize(EnrichedGrantDto grantDto) {
        var category = categoryMappingService.mapCategory(grantDto.getGrantDto());
        log.info("Enriched grant {} with a category.", grantDto.getGrantDto().get_id());
        return EnrichedGrantDto.builder().grantDto(grantDto.getGrantDto()).summary(grantDto.getSummary()).enrichedCategories(category).build();
    }
}
