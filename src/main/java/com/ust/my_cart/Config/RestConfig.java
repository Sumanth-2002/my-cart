package com.ust.my_cart.Config;

import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestConfig extends RouteBuilder {
    @Override
    public void configure() {
        onException(Throwable.class)
                .handled(true)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Validation Failed");
                    errorResponse.put("message", exception.getMessage());
                    exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    exchange.getIn().setBody(errorResponse);
                });
        restConfiguration()
                .component("netty-http")
                .host("0.0.0.0")
                .port(9091)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true");

        rest("/myCart")
                // Endpoint to add a new category
                .post("/category/addCategory")
                .type(Category.class)
                .consumes("application/json")
                .produces("application/json")
                .to("direct:insertCategory")

                // Endpoint to get all categories
                .get("/category")
                .to("direct:findAllCategories")

                // Endpoint to get category by ID
                .get("/category/{id}")
                .to("direct:findCategoryById")

                // Endpoint to add a new item
                .post("/item/addItem")
                .type(Item.class)
                .to("direct:insertItem")

                // Endpoint to get all items
                .get("/item")
                .to("direct:findAllItems")

                // Endpoint to get item by ID
                .get("/item/{id}")
                .to("direct:findItemById")

                // Endpoint to get items by category ID
                .get("/item/category/{categoryid}")
                .to("direct:findItemsByCategoryId")

                .get("/test/{categoryId}")
                .to("direct:getItemTrendAnalyser")
                .get("/testItem")
                .to("direct:getItemReviews")
                .get("/testItemCategory")
                .to("direct:findAllItemsAndSaveToFile")
                .post("/item/update")
                .consumes("application/json")
                .to("direct:updateInventory")

                .post("/item/update/async")
                .consumes("application/json")
                .to("direct:updateInventoryAsync");
    }
}

