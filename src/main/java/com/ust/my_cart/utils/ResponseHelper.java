package com.ust.my_cart.utils;

import com.ust.my_cart.Dto.CategoryItemsResponse;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Item;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.springframework.stereotype.Component;

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
}

