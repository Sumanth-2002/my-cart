package com.ust.my_cart.Dummy;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class ResponseHelper {
    public Processor categorySuccessResponse() {
        return exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getIn().setBody(Map.of(
                    "status", "success",
                    "message", "Category inserted successfully"
            ));
        };
    }

}
