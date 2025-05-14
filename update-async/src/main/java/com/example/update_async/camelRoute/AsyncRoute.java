package com.example.update_async.camelRoute;

import com.example.update_async.utils.MongoService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class AsyncRoute extends RouteBuilder {
    @Autowired
    private MongoService mongoService;
    @Override
    public void configure() throws Exception {
        from("activemq:queue:inventory.update.queue?concurrentConsumers=10")
                .routeId("inventoryAsyncUpdater")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .log("Received Body: ${body}")
                .process(mongoService::applySingleInventoryUpdate)
                .log("Stock updated asynchronously for item: ${body}");

    }
}
