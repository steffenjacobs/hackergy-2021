package me.voltbox.hackergy.enriching.enrichments;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantDto;
import me.voltbox.hackergy.common.service.DatastoreService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentPipeline {
    private final TextSummarizationService textSummarizationService;
    private final DatastoreService datastoreService;

    public void enrichGrant(GrantDto grantDto) {
        final EnrichedGrantDto enrichedGrant;
        if ("ItFoerderungen".equals(grantDto.getSource())) {
            enrichedGrant = EnrichedGrantDto.builder().grantDto(grantDto).summary(grantDto.getText()).build();

        } else {
            enrichedGrant = summarizeText(grantDto);
        }
        datastoreService.insertEnrichedGrant(enrichedGrant);
    }

    public EnrichedGrantDto summarizeText(GrantDto grantDto) {
        var shortText = textSummarizationService.summarize(grantDto.get_id(), grantDto.getText());
        log.info("Enriched grant {} with a short summary.", grantDto.get_id());
        return EnrichedGrantDto.builder().grantDto(grantDto).summary(shortText.orElse("-")).build();
    }
}
