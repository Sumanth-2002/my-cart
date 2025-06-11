package com.ust.my_cart.utils;

import com.ust.my_cart.Dto.CategoryItemsResponse;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Item;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseHelper {
    public void insertCategoryResponse(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setBody(Map.of(
                "status", "success",
                "message", "Category inserted successfully"
        ));
    }

    public void findCategoryByIdResponse(Exchange exchange) {
        Document category = exchange.getIn().getBody(Document.class);
        if (category == null || category.isEmpty()) {
            throw new ProcessException("Category not found for the given ID.", 404);
        }
        exchange.getIn().setBody(category);
    }

    public void itemInsertionResponse(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setBody(Map.of(
                "status", "success",
                "message", "Item inserted successfully"
        ));
    }

    public void findItemsByCategoryIdResponse(Exchange exchange) {
        List<Item> items = (List<Item>) exchange.getIn().getBody();
        CategoryItemsResponse response = new CategoryItemsResponse();
        response.setCategoryName(exchange.getProperty("categoryName").toString());
        response.setCategoryDepartment(exchange.getProperty("categoryDep").toString());
        response.setItems(items);
        if (items.isEmpty()) {
            response.setMessage("No items found for categoryId: " + exchange.getProperty("categoryId").toString());
        }
        exchange.getIn().setBody(response);
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
    public void findItemsByCategoryIdResponses(Exchange exchange) {
        Document item = exchange.getIn().getBody(Document.class); // Single item Document
        Map<String, Object> responseItem = new HashMap<>();
        responseItem.put("itemId", item.getString("_id"));
        responseItem.put("itemName", item.getString("itemName"));
//        responseItem.put("categoryId", item.getString("categoryId"));
        responseItem.put("categoryName", item.getString("categoryName")); // Set by mapCategoryDetails
//        responseItem.put("categoryDepartment", item.getString("categoryDep")); // Set by mapCategoryDetails
//        responseItem.put("lastUpdateDate", item.getString("lastUpdateDate"));
        responseItem.put("specialProduct", item.getBoolean("specialProduct"));

        // Extract nested itemPrice
        Document priceDoc = item.get("itemPrice", Document.class);
        if (priceDoc != null) {
            responseItem.put("basePrice", priceDoc.get("basePrice"));
            responseItem.put("sellingPrice",priceDoc.get("sellingPrice"));
        }

        // Extract nested stockDetails
        Document stockDoc = item.get("stockDetails", Document.class);
        if (stockDoc != null) {
            responseItem.put("availableStock", stockDoc.getInteger("availableStock"));
            responseItem.put("unitOfMeasure", stockDoc.getString("unitOfMeasure"));
//            if (stockDoc.containsKey("damaged")) {
//                responseItem.put("damaged", stockDoc.getInteger("damaged"));
//            }
//            if (stockDoc.containsKey("soldOut")) {
//                responseItem.put("soldOut", stockDoc.getInteger("soldOut"));
//            }
        }

        // Extract reviews
//        List<Document> reviews = item.getList("review", Document.class);
//        List<Map<String, Object>> reviewList = new ArrayList<>();
//        if (reviews != null) {
//            for (Document review : reviews) {
//                Map<String, Object> reviewMap = new HashMap<>();
//                reviewMap.put("rating", review.getString("rating"));
//                reviewMap.put("comment", review.getString("comment"));
//                reviewList.add(reviewMap);
//            }
//        }
//        responseItem.put("reviews", reviewList);

        exchange.getIn().setBody(responseItem);
    }

    public List<Map<String, Object>> buildItemsWithCategoryNameResponse(Exchange exchange) {
        List<?> itemResponses = exchange.getIn().getBody(List.class); // Aggregated list of response maps
        List<Map<String, Object>> response = new ArrayList<>();

        for (Object itemObj : itemResponses) {
            if (itemObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                response.add(itemMap); // Already formatted by findItemsByCategoryIdResponses
            }
        }

        return response;
    }
}

