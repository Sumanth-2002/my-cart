package com.ust.my_cart.Processor;

import com.mongodb.client.model.Filters;
import com.ust.my_cart.Exception.ProcessException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;



@Component
public class FindCategoryByIdProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String categoryId = exchange.getIn().getHeader("id", String.class);
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new ProcessException("Missing required header: id", 400);
        }
        Bson filter = Filters.eq("_id", categoryId);
        exchange.getIn().setBody(filter);
    }
}
