package com.ust.my_cart.Processor;

import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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

        if (category.get_id() == null || category.get_id().trim().isEmpty()) {
            throw new ProcessException("Category _id is required", 400);
        }

        if (mongoTemplate.findById(category.get_id(), Category.class, "category") != null) {
            throw new ProcessException("Category with this ID already exists", 409);
        }
    }
}

