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

    public void insertItem(Item item) {
        MongoCollection<Document> itemCollection = database.getCollection("item");
        Document itemDoc = convertToDocument(item);
        itemCollection.insertOne(itemDoc);
    }

    public List<Document> findItemsByQuery(String query) {
        MongoCollection<Document> itemCollection = database.getCollection("item");
        return itemCollection.find(Document.parse(query)).into(new ArrayList<>());
    }

    public Document findCategoryById(String categoryId) {
        MongoCollection<Document> categoryCollection = database.getCollection("category");
        return categoryCollection.find(new Document("_id", categoryId)).first();
    }

    private Document convertToDocument(Item item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            return Document.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Error converting Item to Document", e);
        }
    }
    public List<Document> findAllItems() {
        MongoCollection<Document> itemCollection = database.getCollection("item");
        return itemCollection.find().into(new ArrayList<>());
    }

    public Document findItemById(String id) {
        MongoCollection<Document> itemCollection = database.getCollection("item");
        return itemCollection.find(new Document("_id", id)).first();
    }
    public List<Document> findAllCategories() {
        MongoCollection<Document> categoryCollection = database.getCollection("category");
        return categoryCollection.find().into(new ArrayList<>());
    }
    public void insertCategory(Category category) {
        MongoCollection<Document> collection = database.getCollection("category");
        Document doc = new Document()
                .append("_id", category.get_id())
                .append("categoryName", category.getCategoryName())
                .append("categoryDep", category.getCategoryDep())
                .append("categoryTax",category.getCategoryTax());

        collection.insertOne(doc);
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
                        "error", String.format("Insufficient stock: available=%d, reduce=%d", availableStock, totalReduction)
                ));
                continue;
            }

            itemCollection.updateOne(
                    Filters.eq("_id", itemId),
                    Updates.inc("stockDetails.availableStock", -totalReduction)
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

    public void applySingleInventoryUpdate(Exchange exchange) {
        Map<String, Object> updatePayload = exchange.getIn().getBody(Map.class);
        String itemId = (String) updatePayload.get("_id");
        int totalReduction = (Integer) updatePayload.get("totalReduction");

        Bson criteria = Filters.eq("_id", itemId);
        Bson updateQuery = Updates.inc("stockDetails.availableStock", -totalReduction);

        MongoCollection<Document> itemCollection = database.getCollection("item");
        itemCollection.updateOne(criteria, updateQuery);
    }



}
