package com.ust.my_cart.CamelRoutes;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.ust.my_cart.Document.Category;
import com.ust.my_cart.Document.Item;
import com.ust.my_cart.Document.ItemPrice;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MongoRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        onException(IllegalArgumentException.class)
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
        rest("/category")
                .post()
                .type(Category.class)
                .consumes("application/json")
                .produces("application/json")
                .to("direct:insertCategory")
                .get()
                .to("direct:findAllCategories")
                .get("/{id}")
                .to("direct:findCategoryById");

        from("direct:insertCategory")
                .routeId("insertCategoryRoute")
                .log("Received category to insert: ${body}")
                .setProperty("categoryBody", body())

                .process(exchange -> {
                    Category category = exchange.getIn().getBody(Category.class);
                    if (category.get_id() == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("status", "error");
                        error.put("message", "Category _id is required.");
                        exchange.getIn().setHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE, 400);
                        exchange.getIn().setBody(error);
                        exchange.setProperty("errorOccurred", true);
                    } else {
                        String query = String.format("{ \"_id\": \"%s\" }", category.get_id());
                        exchange.getIn().setHeader("CamelMongoDbCriteria", query);
                    }
                })
                .choice()
                .when(exchangeProperty("errorOccurred").isEqualTo(true))
                .stop()
                .end()
                .to("mongodb:myDb?database=cart&collection=category&operation=findOneByQuery")

                .choice()
                .when(body().isNull())
                .log("Category not found. Inserting.")
                .setBody(exchangeProperty("categoryBody"))
                .to("mongodb:myDb?database=cart&collection=category&operation=insert")
                .setBody(exchangeProperty("categoryBody"))
                .otherwise()
                .setHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE, constant(409))
                .setBody().constant(Map.of(
                        "status", "error",
                        "message", "Category with this ID already exists."
                ))
                .end();

        from("direct:findAllCategories")
                .log("Fetching all categories")
                .to("mongodb:myDb?database=cart&collection=category&operation=findAll");

        from("direct:findCategoryById")
                .log("Fetching category by ID: ${header.id}")
                .process(exchange -> {
                    String categoryId = exchange.getIn().getHeader("id", String.class);
                    String query = "{ \"_id\": \"" + categoryId + "\" }";

                    exchange.getIn().setHeader("CamelMongoDbCriteria", query);
                })
                .to("mongodb:myDb?database=cart&collection=category&operation=findOneByQuery")
                .log("Category fetched: ${body}");

        rest("/item")
                .post()
                .type(Item.class)
                .to("direct:insertItem")
                .get()
                .to("direct:findAllItems")
                .get("/{id}")
                .to("direct:findItemById")
                .get("/category/{categoryid}")
                .to("direct:findItemsByCategoryId");

        from("direct:insertItem")
                .routeId("createItemRoute")
                .log("Received item to insert: ${body}")
                .process(exchange -> {
                    Item item = exchange.getIn().getBody(Item.class);
                    Map<String, String> errors = new HashMap<>();

                    // Validate item structure
                    if (item == null || item.get_id() == null || item.get_id().trim().isEmpty()) {
                        errors.put("itemId", "Item ID is required and must not be empty");
                    }

                    // Validate categoryId
                    if (item.getCategoryId() == null || item.getCategoryId().trim().isEmpty()) {
                        errors.put("categoryId", "Category ID is required and must not be empty");
                    }

                    // Validate prices
                    ItemPrice itemPrice = item.getItemPrice();
                    if (itemPrice == null) {
                        errors.put("itemPrice", "Item price is required");
                    } else {
                        if (itemPrice.getBasePrice() <= 0) {
                            errors.put("basePrice", "Base price must be greater than zero");
                        }
                        if (itemPrice.getSellingPrice() <= 0) {
                            errors.put("sellingPrice", "Selling price must be greater than zero");
                        }
                    }

                    // Validate stock details
                    if (item.getStockDetails() != null) {
                        if (item.getStockDetails().getAvailableStock() < 0) {
                            errors.put("availableStock", "Available stock cannot be negative");
                        }
                    }

                    if (!errors.isEmpty()) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("status", "error");
                        errorResponse.put("message", "Validation failed");
                        errorResponse.put("errors", errors);
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                        exchange.getIn().setBody(errorResponse);
                        exchange.setProperty("validationFailed", true);
                    } else {
                        exchange.setProperty("item", item);
                    }
                })
                .choice()
                .when(exchangeProperty("validationFailed").isEqualTo(true))
                .stop()
                .otherwise()
                // Log Item ID before query
                .log("Item ID: ${exchangeProperty.item.get_id()}")
                // Check if item exists
                .setHeader("CamelMongoDbCriteria", simple("{'_id': '${exchangeProperty.item.get_id()}'}"))
                .to("mongodb:myDb?database=cart&collection=item&operation=findOneByQuery")
                .choice()
                .when(simple("${body} != null"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(409))
                .setBody(simple("{\"status\": \"error\", \"message\": \"Item with ID ${exchangeProperty.item.get_id()} already exists\"}"))
                .stop()
                .otherwise()
                // Validate category
                .setHeader("CamelMongoDbCriteria", simple("{'_id': '${exchangeProperty.item.categoryId}'}"))
                .to("mongodb:myDb?database=cart&collection=category&operation=findOneByQuery")
                .choice()
                .when(simple("${body} == null"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setBody(simple("{\"status\": \"error\", \"message\": \"Category ${exchangeProperty.item.categoryId} does not exist\"}"))
                .stop()
                .otherwise()
                // Insert item and pass the same data as response
                .setBody(simple("${exchangeProperty.item}"))
                .to("mongodb:myDb?database=cart&collection=item&operation=insert")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
                .log("Item inserted successfully with ID: ${exchangeProperty.item.get_id()}")
                .endChoice()
                .endChoice()
                .endChoice();


        from("direct:findItemsByCategoryId")
                .routeId("findItemsByCategoryIdRoute")
                .log("Fetching items for category ID: ${header.categoryid}, includeSpecial: ${header.includeSpecial}")
                .process(exchange -> {
                    String categoryId = exchange.getIn().getHeader("categoryid", String.class);
                    String includeSpecial = exchange.getIn().getHeader("includeSpecial", String.class);

                    String query = "{ \"categoryId\": \"" + categoryId + "\" }";

                    if (includeSpecial != null) {
                        if ("true".equalsIgnoreCase(includeSpecial)) {
                            query = "{ \"categoryId\": \"" + categoryId + "\", \"specialProduct\": true }";
                        } else if ("false".equalsIgnoreCase(includeSpecial)) {
                            query = "{ \"categoryId\": \"" + categoryId + "\", \"specialProduct\": { \"$ne\": true } }";
                        }
                    }


                    exchange.getIn().setHeader("CamelMongoDbCriteria", query);
                })
                .log("Mongo Query: ${header.CamelMongoDbCriteria}")
                .to("mongodb:myDb?database=cart&collection=item&operation=findAll")
                .log("Items fetched: ${body}");


        from("direct:findAllItems")
                .log("Fetching all items")
                .to("mongodb:myDb?database=cart&collection=item&operation=findAll");

        from("direct:findItemById")
                .log("Fetching item by ID: ${header.id}")
                .setBody(simple("{ '_id': '${header.id}' }"))
                .to("mongodb:myDb?database=cart&collection=item&operation=findOneByQuery");
        rest("/inventory")
                .post()
                .consumes("application/json")
                .to("direct:updateInventory");

        from("direct:updateInventory")
                .routeId("updateInventoryRoute")
                .log("Received inventory payload: ${body}")
                .process(exchange -> {

                    Map<String, Object> payload = exchange.getIn().getBody(Map.class);
                    List<List<Map<String, Object>>> itemsWrapper = (List<List<Map<String, Object>>>) payload.get("items");
                    List<Map<String, Object>> items = itemsWrapper.get(0);

                    List<String> errors = new ArrayList<>();
                    List<Map<String, Object>> updates = new ArrayList<>();
                    exchange.setProperty("errors", errors);
                    exchange.setProperty("updates", updates);

                    exchange.getIn().setBody(items);
                })
                .split(body())
                .process(exchange -> {

                    Map<String, Object> item = exchange.getIn().getBody(Map.class);
                    List<String> errors = exchange.getProperty("errors", List.class);
                    List<Map<String, Object>> updates = exchange.getProperty("updates", List.class);

                    if (item != null) {

                        String itemId = (String) item.get("_id");
                        Map<String, Object> stockDetails = (Map<String, Object>) item.get("stockDetails");


                        int soldOut = Integer.parseInt((String) stockDetails.getOrDefault("soldOut", "0"));
                        int damaged = Integer.parseInt((String) stockDetails.getOrDefault("damaged", "0"));
                        int totalReduction = soldOut + damaged;

                        Bson criteria = Filters.eq("_id", itemId);
                        exchange.getIn().setHeader("CamelMongoDbCriteria", criteria);
                        exchange.getIn().setHeader("CamelMongoDbOperation", "findOneByQuery");

                        exchange.setProperty("itemId", itemId);
                        exchange.setProperty("totalReduction", totalReduction);
                    } else {
                        errors.add("Item is null in the input payload");
                    }
                })
                .to("mongodb:myDb?database=cart&collection=item&operation=findOneByQuery")
                .process(exchange -> {

                    Map<String, Object> document = exchange.getIn().getBody(Map.class);
                    String itemId = exchange.getProperty("itemId", String.class);
                    int totalReduction = exchange.getProperty("totalReduction", Integer.class);
                    List<String> errors = exchange.getProperty("errors", List.class);
                    List<Map<String, Object>> updates = exchange.getProperty("updates", List.class);

                    if (document != null) {
                        Map<String, Object> stockDetails = (Map<String, Object>) document.get("stockDetails");
                        int availableStock = ((Number) stockDetails.getOrDefault("availableStock", 0)).intValue();

                        if (availableStock < totalReduction) {
                            errors.add(String.format("Insufficient stock for item %s: availableStock=%d, requested reduction=%d",
                                    itemId, availableStock, totalReduction));
                        } else {

                            Bson updateQuery = Updates.inc("stockDetails.availableStock", -totalReduction);
                            Bson criteria = Filters.eq("_id", itemId);

                            // Store the update operation
                            Map<String, Object> updateOp = new HashMap<>();
                            updateOp.put("criteria", criteria);
                            updateOp.put("updateQuery", updateQuery);
                            updates.add(updateOp);
                        }
                    } else {
                        errors.add("Item not found in database: " + itemId);
                    }
                })
                .end()
                .process(exchange -> {
                    List<String> errors = exchange.getProperty("errors", List.class);
                    if (!errors.isEmpty()) {
                        String errorMessage = "Validation errors: " + String.join("; ", errors);
                        throw new IllegalArgumentException(errorMessage);
                    }
                })
                .split(simple("${exchangeProperty.updates}"))
                .process(exchange -> {
                    Map<String, Object> updateOp = exchange.getIn().getBody(Map.class);
                    exchange.getIn().setHeader("CamelMongoDbCriteria", updateOp.get("criteria"));
                    exchange.getIn().setHeader("CamelMongoDbOperation", "update");
                    exchange.getIn().setBody(updateOp.get("updateQuery"));
                })
                .to("mongodb:myDb?database=cart&collection=item&operation=update")
                .end();
    }
}
