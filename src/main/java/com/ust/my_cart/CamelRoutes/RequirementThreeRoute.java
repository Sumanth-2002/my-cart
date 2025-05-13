package com.ust.my_cart.CamelRoutes;

import org.apache.camel.builder.RouteBuilder;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RequirementThreeRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        rest("/items")
                .get("/xml")
                .produces("application/xml")
                .to("direct:generateXml");
//        from("direct:fetchAllItemsXml")
//                .log("Received request to fetch all items")
//                .process(exchange -> {
//                    // Query to fetch all items (empty query document)
//                    Document query = new Document();
//                    exchange.getIn().setBody(query);
//                })
//                .log("Fetching all items from MongoDB with query: ${body}")
//                .to("mongodb:myMongoClient?database=cart&collection=item&operation=findAll")
//                .process(exchange -> {
//                    List<Document> items = exchange.getIn().getBody(List.class);
//
//                    if (items != null && !items.isEmpty()) {
//                        log.info("Fetched " + items.size() + " items from MongoDB");
//                    } else {
//                        log.warn("No items found in MongoDB");
//                    }
//
//                    List<Map<String, Object>> itemList = new ArrayList<>();
//                    for (Document item : items) {
//                        Map<String, Object> itemModel = new HashMap<>();
//                        itemModel.put("itemId", item.getString("_id"));
//                        List<Map<String, Object>> reviews = new ArrayList<>();
//                        List<Document> reviewDocs = (List<Document>) item.get("review");
//                        if (reviewDocs != null) {
//                            for (Document reviewDoc : reviewDocs) {
//                                Map<String, Object> reviewMap = new HashMap<>();
//                                reviewMap.put("rating", reviewDoc.get("rating"));
//                                reviewMap.put("comment", reviewDoc.get("comment"));
//                                reviews.add(reviewMap);
//                            }
//                        }
//                        itemModel.put("review", reviews);
//                        itemList.add(itemModel);
//                    }
//                    Map<String, Object> model = new HashMap<>();
//                    model.put("items", itemList);
//                    exchange.getIn().setBody(model.toString());
//
//                    log.info("Model prepared for FreeMarker: " + model);
//                })
//                .log("Exchange body before FreeMarker: ${body}")
//                .log("Exchange headers before FreeMarker: ${headers}")
//                .to("freemarker:templates/single-item-template.ftl")
//                .setHeader("Content-Type", constant("application/xml"))
//                .log("FreeMarker output: ${body}");

        from("direct:generateXml")
                .log("Starting XML generation")
                .process(exchange -> {
                    List<Map<String, Object>> itemsList = new ArrayList<>();
                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("itemId", "item1");
                    item1.put("review", Arrays.asList(
                            Map.of("rating", 4, "comment", "Good product"),
                            Map.of("rating", 3, "comment", "Average product")
                    ));
                    Map<String, Object> item2 = new HashMap<>();
                    item2.put("itemId", "item2");
                    item2.put("review", Arrays.asList(
                            Map.of("rating", 2, "comment", "Poor quality")
                    ));
                    itemsList.add(item1);
                    itemsList.add(item2);
                    Map<String, Object> model = new HashMap<>();
                    model.put("items", itemsList);
                    exchange.getIn().setBody(model);

                })
                .to("freemarker:templates/items-template.ftl")
                .log("Generated XML: ${body}")
                .to("mock:result");
    }
}