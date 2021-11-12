package me.voltbox.hackergy.common.domain;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantDto {
    @BsonId
    String _id;
    String title;
    @With
    List<String> type;
    List<String> category;
    @With
    String eligibleRegion;
    List<String> eligibleEntities;
    @With
    String sponsor;
    @With
    String contact;
    String text;
    List<String> linkOut;
    LocalDateTime scrapeTime;
    String scrapeId;
    String source;
}
