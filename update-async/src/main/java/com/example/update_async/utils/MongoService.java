package com.example.update_async.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class MongoService {
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ObjectMapper objectMapper;

    public MongoService() {
        this.mongoClient = MongoClients.create();
        this.database = mongoClient.getDatabase("cart");
        this.objectMapper = new ObjectMapper();
    }

    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    public void applySingleInventoryUpdate(Exchange exchange) {
        Map<String, Object> updatePayload = exchange.getIn().getBody(Map.class);
        String itemId = (String) updatePayload.get("_id");

        int soldout = (Integer) updatePayload.get("soldout");
        int damaged = (Integer) updatePayload.get("damaged");
        Bson criteria = Filters.eq("_id", itemId);
        Bson updateQuery = Updates.combine(
                Updates.inc("stockDetails.availableStock", -(soldout + damaged)),
                        Updates.set("lastUpdateDate", formatDate(LocalDateTime.now())),
                        Updates.inc("stockDetails.soldout", +soldout),
                        Updates.inc("stockDetails.damaged", +damaged)
        );

        MongoCollection<Document> itemCollection = database.getCollection("item");
        itemCollection.updateOne(criteria, updateQuery);
    }
}
