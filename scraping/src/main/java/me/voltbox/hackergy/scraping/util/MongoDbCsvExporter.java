package me.voltbox.hackergy.scraping.util;

import me.voltbox.hackergy.scraping.DatastoreService;
import me.voltbox.hackergy.scraping.GrantDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MongoDbCsvExporter {
    private static final char DELIMITER = ';';

    public static void main(String[] args) throws IOException {
        var datastoreService = new DatastoreService();
        datastoreService.setupConnection();

        Files.writeString(Path.of("export.csv"),
                "title;type;category;eligible_region;eligible_entities;sponsor;contact;link_out;source;scrape_id;scrape_time\n" .replace(';', DELIMITER),
                StandardOpenOption.TRUNCATE_EXISTING);
        datastoreService.findAll().forEach((Consumer<GrantDto>) grantDto -> {
            try {
                Files.writeString(Path.of("export.csv"), toCsv(grantDto), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        datastoreService.closeConnection();
    }

    private static String toCsv(GrantDto grantDto) {
        var builder = new StringBuilder();
        builder.append(clean(grantDto.getTitle()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getType()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getCategory()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getEligibleRegion()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getEligibleEntities()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getSponsor()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getContact()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getLinkOut()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getSource()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getScrapeId()));
        builder.append(DELIMITER);
        builder.append(clean(grantDto.getScrapeTime().toString()));
        builder.append('\n');
        return builder.toString();
    }

    private static String clean(List<String> input) {
        return input == null ? "" : input.stream().map(MongoDbCsvExporter::clean).collect(Collectors.joining(" "));
    }

    private static String clean(String input) {
        return input == null ? input : input.replace(DELIMITER, ' ').replace('\n', ' ');
    }
}
