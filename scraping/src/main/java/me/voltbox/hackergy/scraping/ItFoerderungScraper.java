package me.voltbox.hackergy.scraping;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItFoerderungScraper {

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
                    log.error("Could not scrape IT Foerderung.", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, 3000L);
    }

    @Scheduled(cron = "* 25 * * * *")
    public void scrape() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var scrapeId = UUID.randomUUID();
        var startTime = LocalDateTime.now();
        log.info("Scrape {} ({}) started at {}.", scrapeId, "IT Foerderung", startTime);

        try (var webClient = createWebClient()) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Get the first page
            String pageUrl = "https://www.it-foerderung.de/foerderprogramme";
            HtmlPage page = webClient.getPage(pageUrl);

            var list = page.getByXPath("//div[@class='container' and @id]");

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
        var text = div.getNextElementSibling().asNormalizedText();
        DomElement overviewBox = div.getNextElementSibling().getNextElementSibling();
        var sponsor = ((HtmlStrong) overviewBox.getByXPath(".//strong").get(6)).getParentNode().getChildNodes().get(1).asNormalizedText();
        var links = overviewBox.getByXPath(".//a").stream()
                .map(HtmlAnchor.class::cast)
                .map(link -> link.getAttribute("href"))
                .toList();
        return GrantDto.builder()
                .title(div.getTextContent().trim())
                .text(text)
                .sponsor(sponsor)
                .scrapeTime(LocalDateTime.now())
                .scrapeId(scrapeId)
                .linkOut(links)
                .source("ItFoerderungen")
                .build();
    }

    private WebClient createWebClient() {
        var webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setUseInsecureSSL(true);
        return webClient;
    }
}
