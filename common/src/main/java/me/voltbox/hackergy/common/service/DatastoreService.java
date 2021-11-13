package me.voltbox.hackergy.common.service;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantDto;
import me.voltbox.hackergy.common.domain.GrantFilterDto;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.*;

@Component
public class DatastoreService {

    private MongoClient mongoClient;
    private MongoCollection<GrantDto> grantsCollection;
    private MongoCollection<EnrichedGrantDto> enrichedGrantsCollection;

    @PostConstruct
    public void setupConnection() {
        mongoClient = new MongoClient(new MongoClientURI("mongodb://root:rootpassword@mongodb:27017"));
        var pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        var db = mongoClient.getDatabase("voltbox").withCodecRegistry(pojoCodecRegistry);
        grantsCollection = db.getCollection("grants", GrantDto.class);
        enrichedGrantsCollection = db.getCollection("enriched_grants", EnrichedGrantDto.class);
    }

    @PreDestroy
    public void closeConnection() {
        mongoClient.close();
    }

    public void insertGrant(GrantDto grant) {
        grantsCollection.insertOne(grant);
    }

    public Iterable<EnrichedGrantDto> findByFilter(GrantFilterDto filter) {
        return enrichedGrantsCollection.find(and(
                in("enrichedCategories", filter.getCategory()),
                or(not(exists("grantDto.eligibleRegion")), in("grantDto.eligibleRegion", filter.getRegion(), "bundesweit")),
                or(not(exists("grantDto.type")), in("grantDto.type", filter.getType())),
                or(not(exists("grantDto.eligibleEntities")), in("grantDto.eligibleEntities", filter.getEntity()))
        ));
    }

    public void insertEnrichedGrant(EnrichedGrantDto grant) {
        trim(grant);
        enrichedGrantsCollection.insertOne(grant);
    }

    private String escapeBackslashes(String string){
        return string.replace("\\", "\\\\");
    }

    public void trim(EnrichedGrantDto grant) {
        trim(grant::getSummary, grant::setSummary);
        trimList(grant::getEnrichedCategories, grant::setEnrichedCategories);
        trim(grant.getGrantDto());
    }

    private void trim(GrantDto grant) {
        trim(grant::getTitle, grant::setTitle);
        trim(grant::getEligibleRegion, grant::setEligibleRegion);
        trim(grant::getSponsor, grant::setSponsor);
        trim(grant::getContact, grant::setContact);
        trim(grant::getText, grant::setText);
        trim(grant::getSource, grant::setText);
        trimList(grant::getType, grant::setType);
        trimList(grant::getCategory, grant::setCategory);
        trimList(grant::getEligibleEntities, grant::setEligibleEntities);
        trimList(grant::getLinkOut, grant::setLinkOut);
    }

    private void trim(Supplier<String> supplier, Consumer<String> consumer) {
        Optional.ofNullable(supplier.get())
                .map(String::trim)
                .map(this::escapeBackslashes)
                .ifPresent(consumer);
    }

    private void trimList(Supplier<List<String>> supplier, Consumer<List<String>> consumer) {
        Optional.ofNullable(supplier.get()).map(list -> list.stream()
                        .map(String::trim)
                        .map(this::escapeBackslashes)
                        .toList())
                .ifPresent(consumer);
    }

    public Iterable<GrantDto> findAll() {
        return grantsCollection.find();
    }

    public Iterable<GrantDto> findByScrapeId(String scrapeId) {
        return grantsCollection.find(eq("scrapeId", scrapeId));
    }
}
