package me.voltbox.hackergy.scraping;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class DatastoreService {

    private MongoClient mongoClient;
    private MongoCollection<GrantDto> grantsCollection;

    @PostConstruct
    public void setupConnection() {
        mongoClient = new MongoClient(new MongoClientURI("mongodb://root:rootpassword@mongodb:27017"));
        var pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        var db = mongoClient.getDatabase("voltbox").withCodecRegistry(pojoCodecRegistry);
        grantsCollection = db.getCollection("grants", GrantDto.class);
    }

    @PreDestroy
    public void closeConnection() {
        mongoClient.close();
    }

    public void insertGrant(GrantDto grant) {
        grantsCollection.insertOne(grant);
    }

    public FindIterable<GrantDto> findAll() {
        return grantsCollection.find();
    }
}
