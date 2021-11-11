package me.voltbox.hackergy.scraping;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
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
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BmwiScraper {

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(12);

    private final DatastoreService datastoreService;
    private final EnrichmentNotificationService enrichmentNotificationService;

    @PostConstruct
    public void startScraping() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    scrapeBmwi();
                } catch (IOException | TimeoutException | ExecutionException e) {
                    log.error("Could not scrape bmwi.", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, 3000L);
    }

    @Scheduled(cron = "* 13 * * * *")
    public void scrapeBmwi() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        var scrapeId = UUID.randomUUID();
        var startTime = LocalDateTime.now();
        log.info("Scrape {} ({}) started at {}.", scrapeId, "BMWI", startTime);

        try (var webClient = createWebClient()) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Get the first page
            String pageUrl = "https://www.foerderdatenbank.de/SiteGlobals/FDB/Forms/Suche/Expertensuche_Formular" +
                    ".html?resourceId=c4b4dbf3-4c29-4e70-9465-1f1783a8f117&input_=bd101467-e52a-4850-931d-5e2a691629e5&pageLocale=de&filterCategories" +
                    "=FundingProgram&filterCategories.GROUP=1&templateQueryString=&submit=Suchen";
            HtmlPage page = webClient.getPage(pageUrl);

            List<Future<?>> futures = new ArrayList<>();

            while (true) {
                page.getByXPath("//div[@class='card card--horizontal card--fundingprogram']//a[@class='']")
                        .stream()
                        .map(HtmlAnchor.class::cast)
                        .map(anchor -> anchor.getAttribute("href"))
                        .map(link -> threadPool.submit(() -> fetchAndStoreGrant(link, scrapeId.toString())))
                        .forEach(futures::add);
                var forwardButton = page.getFirstByXPath("//a[@class='forward button']");
                if (forwardButton != null) {
                    page = ((HtmlAnchor) forwardButton).click();
                    webClient.waitForBackgroundJavaScript(5000);
                    log.info("Collected {} results to be scraped in total.", futures.size());
                } else {
                    log.info("Last page fetched.");
                    break;
                }
            }

            log.info("Finished fetching all pages.");
            for (var future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
            log.info("Fetched {} grants.", futures.size());
            threadPool.shutdown();
        }
        log.info("Scrape {} finished at {}. Took {}s", scrapeId, LocalDateTime.now(), ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()));
        enrichmentNotificationService.notifyEnrichment(scrapeId);
    }

    private void fetchAndStoreGrant(String link, String scrapeId) {
        try (var webClient = createWebClient()) {
            HtmlPage programPage = webClient.getPage("https://www.foerderdatenbank.de/" + link);

            var list = programPage.getByXPath("//dd");
            if (list.size() != 2 && list.size() != 5 && list.size() != 6 && list.size() != 7) {
                log.error("Could not parse https://www.foerderdatenbank.de/{}.", link);
            }

            GrantDto grant = extractGrant(scrapeId, programPage, list);
            datastoreService.insertGrant(grant);
            log.debug("Stored grant {}}.", grant.getTitle());
        } catch (RuntimeException | IOException e) {
            log.warn("Could not fetch https://www.foerderdatenbank.de/{}: {}", link, e.getMessage());
        }
    }

    private GrantDto extractGrant(String scrapeId, HtmlPage programPage, List<Object> list) {
        if (list.size() == 2) {
            return GrantDto.builder()
                    .title(extractTextIfPresent(programPage.getFirstByXPath("//h1[@class='title']")))
                    .eligibleRegion(extractTextIfPresent(list.get(0)))
                    .contact(extractNormalizedTextIfPresent(list.get(1)))
                    .text(extractNormalizedTextIfPresent(programPage.getFirstByXPath("//div[@class='content']//div[@class='rich--text']")))
                    .scrapeTime(LocalDateTime.now())
                    .scrapeId(scrapeId)
                    .source("BMWI")
                    .linkOut(List.of(programPage.getUrl().toString()))
                    .build();
        }

        var grantBuilder = GrantDto.builder()
                .title(extractTextIfPresent(programPage.getFirstByXPath("//h1[@class='title']")))
                .type(Arrays.asList(extractTextIfPresent(list.get(0)).split(",")))
                .category(Arrays.asList(((HtmlElement) list.get(1)).getTextContent().split(",")))
                .eligibleRegion(extractTextIfPresent(list.get(2)))
                .eligibleEntities(Arrays.asList(((HtmlElement) list.get(3)).getTextContent().split(",")))
                .text(extractNormalizedTextIfPresent(programPage.getFirstByXPath("//div[@class='content']//div[@class='rich--text']")))
                .scrapeTime(LocalDateTime.now())
                .scrapeId(scrapeId)
                .source("BMWI");

        if (list.size() == 5) {
            grantBuilder = grantBuilder.contact(extractNormalizedTextIfPresent(list.get(4)));

        } else if (list.size() == 6) {
            grantBuilder = grantBuilder.sponsor(extractTextIfPresent(list.get(4)))
                    .contact(extractNormalizedTextIfPresent(list.get(5)));
        } else if (list.size() == 7) {
            grantBuilder = grantBuilder.sponsor(extractTextIfPresent(list.get(4)))
                    .contact(extractNormalizedTextIfPresent(list.get(5)))
                    .linkOut(List.of(programPage.getUrl().toString(), extractNormalizedTextIfPresent(list.get(6))));
        }
        return grantBuilder.build();
    }

    private String extractTextIfPresent(Object obj) {
        return Optional.ofNullable(obj).map(HtmlElement.class::cast).map(HtmlElement::getTextContent).map(String::trim).orElse("<empty>");
    }

    private String extractNormalizedTextIfPresent(Object obj) {
        return Optional.ofNullable(obj).map(HtmlElement.class::cast).map(HtmlElement::asNormalizedText).map(String::trim).orElse("<empty>");
    }

    private WebClient createWebClient() {
        var webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        return webClient;
    }
}
