package com.ust.my_cart.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;

import org.apache.camel.Exchange;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class ItemProcessor {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ItemProcessor.class);
    public void createItemProcessor(Item item) {
        if (item == null || item.get_id() == null || item.get_id().trim().isEmpty()) {
            throw new ProcessException("Item ID is required and must not be empty", 400);
        }
        if (item.getCategoryId() == null || item.getCategoryId().trim().isEmpty()) {
            throw new ProcessException("Category ID is required and must not be empty", 400);
        }
        if (item.getItemPrice() == null) {
            throw new ProcessException("Item price is required", 400);
        }
        if (item.getItemPrice().getBasePrice() <= 0) {
            throw new ProcessException("Base price must be greater than zero", 400);
        }
        if (item.getItemPrice().getSellingPrice() <= 0) {
            throw new ProcessException("Selling price must be greater than zero", 400);
        }
        if (item.getStockDetails() != null && item.getStockDetails().getAvailableStock() < 0) {
            throw new ProcessException("Available stock cannot be negative", 400);
        }

        if (mongoTemplate.findById(item.get_id(), Item.class, "item") != null) {
            throw new ProcessException("Item with ID " + item.get_id() + " already exists", 409);
        }

        if (mongoTemplate.findById(item.getCategoryId(), Category.class, "category") == null) {
            throw new ProcessException("Category " + item.getCategoryId() + " does not exist", 404);
        }


    }

    public void findItemsByCategoryId(Exchange exchange) {

        String categoryId = exchange.getIn().getHeader("categoryid", String.class);
        String includeSpecial = exchange.getIn().getHeader("includeSpecial", String.class);
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new ProcessException("Missing required header: categoryid", 400);
        }
        String query;

        if ("true".equalsIgnoreCase(includeSpecial)) {
            query = "{ \"categoryId\": \"" + categoryId + "\" }";
        } else {
            query = "{ \"categoryId\": \"" + categoryId + "\", \"specialProduct\": { \"$ne\": true } }";  // exclude special products
        }
        exchange.getIn().setBody(Document.parse(query));


        Document categoryDoc = mongoTemplate.findById(categoryId, Document.class, "category");
        if (categoryDoc == null) {
            throw new ProcessException("CategoryId not found", 404);
        }
        String categoryName = categoryDoc.getString("categoryName");
        String categoryDep = categoryDoc.getString("categoryDep");
        exchange.setProperty("categoryName", categoryName);
        exchange.setProperty("categoryDep", categoryDep);
    }


    public void validateAndPrepareInventoryUpdates(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> updates = new ArrayList<>();
        List<Map<String, Object>> successfulUpdates = new ArrayList<>();

        for (Map<String, Object> item : items) {
            if (item == null) {
                errors.add(Map.of("itemId", null, "error", "Item is null in the input payload"));
                continue;
            }

            String itemId = (String) item.get("_id");
            Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");

            int soldOut = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("soldOut", "0")));
            int damaged = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("damaged", "0")));
            int totalReduction = soldOut + damaged;

            updates.add(Map.of(
                    "itemId", itemId,
                    "soldout", soldOut,
                    "damaged", damaged
            ));
        }
        exchange.setProperty("errors", errors);
        exchange.setProperty("updates", updates);
        exchange.setProperty("successfulUpdates", successfulUpdates);
    }

    public void updateStock(Exchange exchange) {
        List<Map<String, Object>> errors = exchange.getProperty("errors", List.class);
        List<Map<String, Object>> successfulUpdates = exchange.getProperty("successfulUpdates", List.class);

        try {
            Map<String, Object> item = exchange.getIn().getBody(Map.class);

            String itemId = exchange.getProperty("itemId", String.class);
            System.out.println(itemId);
            Integer newSoldout = (Integer) exchange.getProperty("soldout");
            Integer newDamaged = (Integer) exchange.getProperty("damaged");

            if (item == null) {
                errors.add(Map.of("itemId", itemId, "error", "Item not found in database"));
                exchange.setProperty("skipSave", true);
                return;
            }

            Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
            if (stockDetails == null) {
                errors.add(Map.of("itemId", itemId, "error", "Stock Details not found"));
                exchange.setProperty("skipSave", true);
                return;
            }

            int availableStock = Integer.parseInt(stockDetails.getOrDefault("availableStock", "0").toString());
            int soldOut = Integer.parseInt(stockDetails.getOrDefault("soldOut", "0").toString());
            int damaged = Integer.parseInt(stockDetails.getOrDefault("damaged", "0").toString());

            if ((newDamaged+newSoldout) > availableStock) {
                errors.add(Map.of("itemId", itemId, "error", "Total reduction exceeds available stock"));
                exchange.setProperty("skipSave", true);
                return;
            }

            stockDetails.put("availableStock", availableStock - (newDamaged+newSoldout));
            stockDetails.put("soldOut", soldOut + newSoldout);
            stockDetails.put("damaged", newDamaged);

            item.put("stockDetails", stockDetails);
            item.put("lastUpdateDate", LocalDate.now().toString());

            exchange.getIn().setBody(item);
            successfulUpdates.add(Map.of("itemId", itemId, "status", "updated"));
            exchange.setProperty("skipSave", false);

        } catch (Exception e) {
            e.printStackTrace();
            exchange.setProperty("skipSave", true);
            throw new ProcessException("Stock update failed: " + e.getMessage(), 400);
        }
    }


    public void prepareInventoryUpdateResponse(Exchange exchange) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> successfulUpdates = exchange.getProperty("successfulUpdates", List.class);
        List<Map<String, Object>> errors = exchange.getProperty("errors", List.class);
        response.put("successfulUpdates", successfulUpdates);
        if (errors.size() != 0) {
            response.put("errors", errors);
        }
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, errors.isEmpty() ? 200 : 400);
        exchange.getIn().setBody(response);
    }

    public void validateAndPrepareAsyncUpdate(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> updates = new ArrayList<>();

        for (Map<String, Object> item : items) {
            if (item != null) {
                String itemId = (String) item.get("_id");
                Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
                int soldOut = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("soldOut", "0")));
                int damaged = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("damaged", "0")));
                int totalReduction = soldOut + damaged;
                Document existing = mongoTemplate.findById(itemId, Document.class, "item");
                if (existing == null) {
                    errors.add(Map.of("itemId", itemId, "error", "Item not found in database"));
                    continue;
                }
                Document existingStock = (Document) existing.get("stockDetails");
                int availableStock = existingStock.getInteger("availableStock", 0);
                if (availableStock < totalReduction) {
                    errors.add(Map.of("itemId", itemId,
                            "error", String.format("Updated failed , insufficient stock")));
                    continue;
                }
                Map<String, Object> updatePayload = new HashMap<>();
                updatePayload.put("_id", itemId);
                updatePayload.put("totalReduction", totalReduction);
                updatePayload.put("soldout",soldOut);
                updatePayload.put("damaged",damaged);
                updatePayload.put("availableStock", availableStock - totalReduction);
                updates.add(updatePayload);

            } else {
                errors.add(Map.of("itemId", null, "error", "Item is null in the input payload"));

            }

        }
        exchange.setProperty("updates", updates);
        exchange.setProperty("errors", errors);
        if (!errors.isEmpty()) {
            logger.error("Inventory update validation completed with {} error(s):", errors.size());
            for (Map<String, Object> error : errors) {
                logger.error(" - {}", error);
            }
        } else {
            logger.info(" Inventory update validation completed with no errors.");
        }
    }


}
