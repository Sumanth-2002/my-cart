package com.ust.my_cart.Processor;

import com.mongodb.client.model.Filters;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.stereotype.Component;


@Component
public class ValidateCategoryProcessor implements Processor {

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public void process(Exchange exchange) throws Exception {
        Category category = exchange.getIn().getBody(Category.class);
        String categoryId = category.get_id();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new ProcessException("Category _id is required", 400);
        }
        Bson filter = Filters.eq("_id", categoryId);
        exchange.getIn().setHeader("category", category); // Store Category POJO in header
        exchange.getIn().setBody(filter); // Set body to Bson filter
    }


}

