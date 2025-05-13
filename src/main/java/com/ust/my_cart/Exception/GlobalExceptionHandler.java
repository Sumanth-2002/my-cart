package com.ust.my_cart.Exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GlobalExceptionHandler implements Processor {
    @Override
    public void process(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        int statusCode = 500;
        String message = "Internal Server Error";
        if (exception instanceof ProcessException) {
            statusCode = ((ProcessException) exception).getStatusCode();
            message = exception.getMessage();
        }
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getMessage().setBody(error);
    }
}
