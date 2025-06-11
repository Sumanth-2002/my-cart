package com.ust.my_cart.utils;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class ArrayListAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            List<Object> list = new ArrayList<>();
            list.add(newExchange.getIn().getBody());
            newExchange.getIn().setBody(list);
            return newExchange;
        }
        List<Object> list = oldExchange.getIn().getBody(List.class);
        list.add(newExchange.getIn().getBody());
        return oldExchange;
    }

}