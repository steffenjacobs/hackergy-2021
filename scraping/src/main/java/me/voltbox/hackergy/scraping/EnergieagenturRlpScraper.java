package me.voltbox.hackergy.scraping;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.domain.GrantDto;
import me.voltbox.hackergy.common.service.DatastoreService;
import me.voltbox.hackergy.scraping.util.EnrichmentNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnergieagenturRlpScraper {

    private final DatastoreService datastoreService;
    private final EnrichmentNotificationService enrichmentNotificationService;

    @PostConstruct
    public void startScraping() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    scrape();
                } catch (IOException | TimeoutException | ExecutionException e) {
                    log.error("Could not scrape Energieagentur RLP.", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, 3000L);
    }

    @Scheduled(cron = "* 23 * * * *")
    public void scrape() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var scrapeId = UUID.randomUUID();
        var startTime = LocalDateTime.now();
        log.info("Scrape {} ({}) started at {}.", scrapeId, "Energieagentur RLP", startTime);

        try (var webClient = createWebClient()) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Get the first page
            String pageUrl = "https://www.energieagentur.rlp.de/themen/mobilitaetswende/foerdermoeglichkeiten-fuer-elektromobilitaet";
            HtmlPage page = webClient.getPage(pageUrl);

            var list = page.getByXPath("//div[@class='dropdown kompetenzfeld']//div[@class='d-header']");

            list.stream()
                    .map(HtmlDivision.class::cast)
                    .map(div -> extractGrant(div, scrapeId.toString()))
                    .forEach(datastoreService::insertGrant);

            log.info("Finished fetching {} grants.", list.size());
        }
        log.info("Scrape {} finished at {}. Took {}s", scrapeId, LocalDateTime.now(), ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()));
        enrichmentNotificationService.notifyEnrichment(scrapeId);
    }

    private GrantDto extractGrant(HtmlDivision div, String scrapeId) {
        var eligiblEntity = div.getParentNode().getParentNode().getParentNode().getChildNodes().get(1);
        var category = eligiblEntity.getParentNode().getParentNode().getParentNode().getChildNodes().get(1);
        var text = div.getParentNode().getChildNodes().get(3).asNormalizedText();
        return GrantDto.builder()
                .title(div.getTextContent().trim())
                .eligibleEntities(List.of(eligiblEntity.getTextContent().trim()))
                .category(List.of(category.getTextContent().trim()))
                .text(text)
                .scrapeTime(LocalDateTime.now())
                .scrapeId(scrapeId)
                .source("EnergieagenturRLP")
                .build();
    }

    private WebClient createWebClient() {
        var webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        return webClient;
    }
}
