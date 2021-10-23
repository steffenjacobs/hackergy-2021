package me.voltbox.hackergy.scraping;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantDto {
    String title;
    String type;
    List<String> category;
    String eligibleRegion;
    List<String> eligibleEntities;
    String sponsor;
    String contact;
    String text;
    List<String> linkOut;
    LocalDateTime scrapeTime;
    String scrapeId;
    String source;
}
