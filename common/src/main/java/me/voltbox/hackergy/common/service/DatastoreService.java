package me.voltbox.hackergy.common.service;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantDto;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.mongodb.client.model.Filters.eq;

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

    public void insertEnrichedGrant(EnrichedGrantDto grant) {
        enrichedGrantsCollection.insertOne(grant);
    }

    public Iterable<GrantDto> findAll() {
        return grantsCollection.find();
    }

    public Iterable<GrantDto> findByScrapeId(String scrapeId) {
        return grantsCollection.find(eq("scrapeId", scrapeId));
    }
}
