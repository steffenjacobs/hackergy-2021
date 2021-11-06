package me.voltbox.hackergy.enriching.enrichments;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class TextSummarizationService {
    public Optional<String> summarize(String id, String text) {
        try (var webClient = createWebClient()) {
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            String pageUrl = "https://summarizing-tool.org/de/summary";
            HtmlPage page = webClient.getPage(pageUrl);

            var textArea = (HtmlTextArea) page.getFirstByXPath("//textarea");
            textArea.setText(text);

            var resultPage = (HtmlPage) ((HtmlButton) page.getFirstByXPath("//button")).click();
            return Optional.of(((HtmlTextArea) resultPage.getByXPath("//textarea").get(1)).getText());
        } catch (Exception e) {
            log.error("Could not fetch summary for id {}", id, e);
        }
        return Optional.empty();
    }

    private WebClient createWebClient() {
        var webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        return webClient;
    }
}
