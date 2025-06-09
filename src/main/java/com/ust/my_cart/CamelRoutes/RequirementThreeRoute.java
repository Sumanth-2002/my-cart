package com.ust.my_cart.CamelRoutes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ust.my_cart.Dto.ItemResponse;
import com.ust.my_cart.Dto.Items;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RequirementThreeRoute extends RouteBuilder {


        public void configure() throws Exception {
            // Configure JacksonDataFormat for JSON serialization
            JacksonDataFormat jsonFormat = new JacksonDataFormat();
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            jsonFormat.setObjectMapper(mapper);

            from("direct:getItemTrendAnalyser")
                    .routeId("itemTrendAnalyserRoute")
                    .setHeader(MongoDbConstants.OPERATION_HEADER, constant("aggregate"))
                    .setBody(exchange -> {
                        String categoryId = exchange.getIn().getHeader("categoryId", String.class);

                        List<Document> pipeline = new ArrayList<>();
                        pipeline.add(Document.parse("{ $match: { categoryId: '" + categoryId + "' } }"));
                        pipeline.add(Document.parse("{ $lookup: { from: 'category', localField: 'categoryId', foreignField: '_id', as: 'categoryDetails' } }"));
                        pipeline.add(Document.parse("{ $unwind: '$categoryDetails' }"));
                        pipeline.add(Document.parse(
                                "{ $project: { " +
                                        "_id: 0, " +
                                        "itemId: '$_id', " +
                                        "itemName: 1, " +
                                        "availableStock: '$stockDetails.availableStock', " +
                                        "sellingPrice: '$itemPrice.sellingPrice', " +
                                        "categoryName: '$categoryDetails.categoryName' " +
                                        "} }"
                        ));

                        return pipeline;
                    })
                    .to("mongodb:myMongoBean?database=cart&collection=item&operation=aggregate")
                    .process(exchange -> {
                        Object body = exchange.getIn().getBody();
                        System.out.println("Raw MongoDB response: " + body.getClass() + " - " + body);

                        if (body instanceof List<?>) {
                            @SuppressWarnings("unchecked")
                            List<Document> docs = (List<Document>) body;
                            String categoryId = exchange.getIn().getHeader("categoryId", String.class);
                            String categoryName = docs.isEmpty() ? "Unknown" : docs.get(0).getString("categoryName");
                            List<Items> items = docs.stream()
                                    .map(doc -> new Items(
                                            doc.getString("itemId"),
                                            doc.get("sellingPrice"),
                                            doc.get("availableStock"),
                                            categoryId
                                    ))
                                    .collect(Collectors.toList());
                            ItemResponse result = new ItemResponse(categoryName, items, categoryId);
                            exchange.getIn().setBody(result);
                        } else {
                            throw new IllegalStateException("Expected List<Document> from MongoDB, but got: " + body.getClass());
                        }
                    })
                    .setProperty("itemResponse", simple("${body}"))
                    .marshal(jsonFormat)
                    .convertBodyTo(String.class)
                    .setHeader("Content-Type", constant("application/json"))
                    .log("JSON Response: ${body}")
                    .process(exchange -> {
                        System.out.println("Final JSON body: " + exchange.getIn().getBody());
                    })
                    .multicast()
                    .to("direct:jsonResponse", "direct:toXmlRoute")
                    .end();

            // JSON response route
            from("direct:jsonResponse")
                    .routeId("jsonResponseRoute")
                    .log("Sending JSON response: ${body}")
                    .setBody(simple("${body}"));

            // XML conversion and file output route
            from("direct:toXmlRoute")
                    .routeId("mongoToXmlRoute")
                    .process(exchange -> {
                        ItemResponse itemResponse = exchange.getProperty("itemResponse", ItemResponse.class);
                        Map<String, Object> category = new HashMap<>();
                        category.put("id", itemResponse.getCategoryId());
                        category.put("name", itemResponse.getCategoryName());
                        List<Map<String, Object>> itemsList = new ArrayList<>();
                        for (Items item : itemResponse.getItems()) {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("itemId", item.getItemId());
                            itemMap.put("categoryId", item.getCategoryId());
                            itemMap.put("availableStock", item.getAvailableStock());
                            itemMap.put("sellingPrice", item.getSellingPrice());
                            itemsList.add(itemMap);
                        }
                        category.put("items", itemsList);
                        List<Map<String, Object>> categories = new ArrayList<>();
                        categories.add(category);
                        exchange.getIn().setBody(categories);
                        String fileName = "inventory_" + System.currentTimeMillis() + ".xml";
                        exchange.getIn().setHeader("CamelFileName", fileName);
                    })
//                    .to("velocity:file:src/main/resources/templates/inventory-template.vm")
                    .to("velocity:file:src/main/resources/templates/raw-template.vm")

                    .to("file:C:/Users/290577/Desktop/Task/my-cart/target/output?fileName=${header.CamelFileName}&fileExist=Override")
                    .log("XML file created successfully: ${header.CamelFileName}")
                    .stop();
        }
    }

