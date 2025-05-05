package com.ust.my_cart.Processor;

import com.mongodb.client.model.Filters;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;
import com.ust.my_cart.utils.MongoService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    @Autowired
    private MongoTemplate mongoTemplate;

    public Processor findCategoryByIdProcessor() {
        return exchange -> {
            String categoryId = exchange.getIn().getHeader("id", String.class);
            if (categoryId == null || categoryId.trim().isEmpty()) {
                throw new ProcessException("Missing required header: id", 400);
            }
            Bson filter = Filters.eq("_id", categoryId);
            exchange.getIn().setBody(filter);
        };
    }

    public void validateCategory(Exchange exchange) {
        Category category = exchange.getIn().getBody(Category.class);

        if (category.get_id() == null || category.get_id().trim().isEmpty()) {
            throw new ProcessException("Category _id is required", 400);
        }


        if (mongoTemplate.findById(category.get_id(), Category.class, "category") != null) {
            throw new ProcessException("Category with this ID already exists", 409);
        }


    }


}
