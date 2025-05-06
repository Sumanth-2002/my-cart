package com.ust.my_cart.CamelRoutes;

import org.apache.camel.builder.RouteBuilder;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequirementThreeRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        rest("/items")
                .get("/xml/{id}")
                .produces("application/xml")
                .to("direct:fetchOneItemXml");

        from("direct:fetchOneItemXml")
                .log("Received request for item with id: ${header.id}")
                .process(exchange -> {
                    String itemId = exchange.getIn().getHeader("id", String.class);
                    Document query = new Document("_id", itemId);
                    exchange.getIn().setBody(query);
                })
                .log("Fetching item from MongoDB with query: ${body}")
                .to("mongodb:myMongoClient?database=cart&collection=item&operation=findOneByQuery")
                .process(exchange -> {
                    Document item = exchange.getIn().getBody(Document.class);

                    if (item != null) {
                        log.info("Fetched item from MongoDB: " + item);
                    } else {
                        log.error("Item not found in MongoDB");
                    }

                    if (item == null) {
                        throw new RuntimeException("Item not found in MongoDB");
                    }
                    Map<String, Object> model = new HashMap<>();
                    model.put("itemId", item.getString("_id"));
                    List<Map<String, Object>> reviews = new ArrayList<>();
                    List<Document> reviewDocs = (List<Document>) item.get("review");
                    if (reviewDocs != null) {
                        for (Document reviewDoc : reviewDocs) {
                            Map<String, Object> reviewMap = new HashMap<>();
                            reviewMap.put("rating", reviewDoc.get("rating"));
                            reviewMap.put("comment", reviewDoc.get("comment"));
                            reviews.add(reviewMap);
                        }
                    }
                    model.put("review", reviews);
                    // Set exchange body to stringified model
                    String modelString = model.toString();
                    exchange.getIn().setBody(modelString);
                    log.info("Model prepared for FreeMarker: " + modelString);
                })
                .log("Exchange body before FreeMarker: ${body}")
                .to("freemarker:templates/single-item-template.ftl")
                .setHeader("Content-Type", constant("application/xml"))
                .log("FreeMarker output: ${body}");
    }
}