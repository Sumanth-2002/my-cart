package com.ust.my_cart.utils;

import com.ust.my_cart.Dto.CategoryItemsResponse;
import com.ust.my_cart.Dto.ItemDto;
import com.ust.my_cart.Exception.ProcessException;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.springframework.stereotype.Component;

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
        List<Document> items = (List<Document>) exchange.getIn().getBody();
        List<ItemDto> itemDtos = new ItemMapper().mapToItemDtoList(items);

        CategoryItemsResponse response = new CategoryItemsResponse();
        response.setCategoryName(exchange.getProperty("categoryName").toString());
        response.setCategoryDepartment(exchange.getProperty("categoryDep").toString());
        response.setItems(itemDtos);
        if (itemDtos.isEmpty()) {
            response.setMessage("No items found for categoryId: " + exchange.getProperty("categoryId").toString());
        }
        exchange.getIn().setBody(response);
    }
}

