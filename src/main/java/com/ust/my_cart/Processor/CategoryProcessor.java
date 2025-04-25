package com.ust.my_cart.Processor;

import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.utils.MongoService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CategoryProcessor {
    private final MongoService mongoService;
    @Autowired
     public CategoryProcessor(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    public Processor findCategoryByIdProcessor() {
        return exchange -> {
            String categoryId = exchange.getIn().getHeader("id", String.class);
            if (categoryId == null || categoryId.trim().isEmpty()) {
                throw new ProcessException("Missing required header: id", 400);
            }

            Document category = mongoService.findCategoryById(categoryId);
            if (category == null) {
                throw new ProcessException("Category with ID " + categoryId + " not found", 404);
            }

            exchange.getIn().setBody(category);
        };
    }
    public Processor insertCategoryProcessor() {
        return exchange -> {
            Category category = exchange.getIn().getBody(Category.class);

            if (category.get_id() == null || category.get_id().trim().isEmpty()) {
                throw new ProcessException("Category _id is required", 400);
            }

            Document existingCategory = mongoService.findCategoryById(category.get_id());
            if (existingCategory != null) {
                throw new ProcessException("Category with this ID already exists", 409);
            }

            mongoService.insertCategory(category);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            exchange.getIn().setBody(Map.of(
                    "status", "success",
                    "message", "Category inserted successfully"
            ));
        };
    }


}
