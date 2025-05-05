package com.ust.my_cart.utils;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    private Document convertToDocument(Item item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            return Document.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Error converting Item to Document", e);
        }
    }

    public Document findItemById(String id) {
        MongoCollection<Document> itemCollection = database.getCollection("item");
        return itemCollection.find(new Document("_id", id)).first();
    }
    public void applyInventoryUpdates(Exchange exchange) {
        List<Map<String, Object>> updates = exchange.getProperty("updates", List.class);
        List<Map<String, Object>> errors = exchange.getProperty("errors", List.class);
        List<Map<String, Object>> successfulUpdates = exchange.getProperty("successfulUpdates", List.class);

        MongoCollection<Document> itemCollection = database.getCollection("item");

        for (Map<String, Object> update : updates) {
            String itemId = (String) update.get("itemId");
            int totalReduction = (int) update.get("totalReduction");

            Document item = itemCollection.find(Filters.eq("_id", itemId)).first();
            if (item == null) {
                errors.add(Map.of(
                        "itemId", itemId,
                        "error", "Item not found in database"
                ));
                continue;
            }
            Document stockDetails = (Document) item.get("stockDetails");
            int availableStock = stockDetails.getInteger("availableStock", 0);
            if (availableStock < totalReduction) {
                errors.add(Map.of(
                        "itemId", itemId,
                        "error", String.format("Update Failed ,Insufficient stock")
                ));
                continue;
            }

            itemCollection.updateOne(
                    Filters.eq("_id", itemId),
                    Updates.combine(
                            Updates.inc("stockDetails.availableStock", -totalReduction),
                            Updates.set("lastUpdateDate", formatDate(LocalDateTime.now()))  // Automatically set lastUpdateDate
                    )
            );
            Document updatedItem = itemCollection.find(Filters.eq("_id", itemId)).first();
            Document updatedStockDetails = (Document) updatedItem.get("stockDetails");
            int updatedStock = updatedStockDetails.getInteger("availableStock", 0);

            successfulUpdates.add(Map.of(
                    "itemId", itemId,
                    "status", "updated",
                    "availableStock", updatedStock
            ));
        }
    }

}
