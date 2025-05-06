package com.ust.my_cart.CamelRoutes;

import com.ust.my_cart.Processor.ItemProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequirementTwoRoute extends RouteBuilder {

    @Autowired
    private ItemProcessor itemProcessor;
    @Override
    public void configure() throws Exception {
        from("direct:updateInventoryAsync")
                .routeId("updateInventoryRouteAsync")
                .process(itemProcessor::validateAndPrepareAsyncUpdate)
                .split(exchangeProperty("updates")).parallelProcessing()
                .marshal().json(JsonLibrary.Jackson)
                .to("activemq:queue:inventory.update.queue?exchangePattern=InOnly&deliveryMode=2")
                .end()
                .setBody(constant("Item details are queued for update successfully"));
    }
}
