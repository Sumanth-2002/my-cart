package com.ust.my_cart.Processor;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.ust.my_cart.Dto.CategoryItemsResponse;
import com.ust.my_cart.Dto.ItemDto;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;
import com.ust.my_cart.utils.ItemMapper;
import com.ust.my_cart.utils.MongoService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ItemProcessor {
    private final MongoService mongoService;

    @Autowired
    public ItemProcessor(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    @Autowired
    private MongoTemplate mongoTemplate;


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

    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
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
                    "totalReduction", totalReduction
            ));
        }
        exchange.setProperty("errors", errors);
        exchange.setProperty("updates", updates);
        exchange.setProperty("successfulUpdates", successfulUpdates);
    }


    public void prepareInventoryUpdateResponse(Exchange exchange) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> successfulUpdates = exchange.getProperty("successfulUpdates", List.class);
        List<Map<String, Object>> errors = exchange.getProperty("errors", List.class);
        response.put("successfulUpdates", successfulUpdates);
        response.put("errors", errors);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, errors.isEmpty() ? 200 : 400);
        exchange.getIn().setBody(response);
    }

    public void validateAndPrepareAsyncUpdate(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> updates = new ArrayList<>();
        List<Map<String, Object>> successfulUpdates = new ArrayList<>();

        for (Map<String, Object> item : items) {
            if (item != null) {
                String itemId = (String) item.get("_id");
                Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");
                int soldOut = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("soldOut", "0")));
                int damaged = Integer.parseInt(String.valueOf(stockDetails.getOrDefault("damaged", "0")));
                int totalReduction = soldOut + damaged;
                Document existing = mongoService.findItemById(itemId);
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
                updatePayload.put("availableStock", availableStock - totalReduction);
                updates.add(updatePayload);
                successfulUpdates.add(Map.of(
                        "itemId", itemId,
                        "status", "updated",
                        "availableStock", availableStock - totalReduction
                ));
            } else {
                errors.add(Map.of("itemId", null, "error", "Item is null in the input payload"));
            }
        }
        exchange.setProperty("updates", updates);
        exchange.setProperty("successfulUpdates", successfulUpdates);
        exchange.setProperty("errors", errors);
        exchange.getIn().setBody(updates);
    }





}
