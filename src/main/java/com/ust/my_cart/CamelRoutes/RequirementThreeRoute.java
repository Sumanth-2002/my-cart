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

                            // Get categoryId from header
                            String categoryId = exchange.getIn().getHeader("categoryId", String.class);

                            // Extract categoryName from the first document
                            String categoryName = docs.isEmpty() ? "Unknown" : docs.get(0).getString("categoryName");

                            // Transform each document to Item POJO
                            List<Items> items = docs.stream()
                                    .map(doc -> new Items(
                                            doc.getString("itemId"),
                                            doc.get("sellingPrice"),
                                            doc.get("availableStock"),
                                            categoryId
                                    ))
                                    .collect(Collectors.toList());

                            // Build the final output structure with ItemResponse POJO
                            ItemResponse result = new ItemResponse(categoryName, items, categoryId);

                            // Set the transformed result as the body
                            exchange.getIn().setBody(result);
                        } else {
                            throw new IllegalStateException("Expected List<Document> from MongoDB, but got: " + body.getClass());
                        }
                    })
                    // Store the ItemResponse for later use in XML branch
                    .setProperty("itemResponse", simple("${body}"))
                    // Marshal to JSON for the response
                    .marshal(jsonFormat)
                    .convertBodyTo(String.class)
                    .setHeader("Content-Type", constant("application/json"))
                    .log("JSON Response: ${body}")
                    .process(exchange -> {
                        System.out.println("Final JSON body: " + exchange.getIn().getBody());
                    })
                    // Multicast to handle JSON and XML separately
                    .multicast()
                    .to("direct:jsonResponse", "direct:toXmlRoute")
                    .end();

            // JSON response route
            from("direct:jsonResponse")
                    .routeId("jsonResponseRoute")
                    .log("Sending JSON response: ${body}")
                    // Ensure this is the final response for the caller (e.g., Postman)
                    .setBody(simple("${body}"));

            // XML conversion and file output route
            from("direct:toXmlRoute")
                    .routeId("mongoToXmlRoute")
                    .process(exchange -> {
                        // Retrieve the ItemResponse from the property
                        ItemResponse itemResponse = exchange.getProperty("itemResponse", ItemResponse.class);

                        // Transform ItemResponse to match template expectation ($categories)
                        Map<String, Object> category = new HashMap<>();
                        category.put("id", itemResponse.getCategoryId());
                        category.put("name", itemResponse.getCategoryName());
                        // Convert Items list to a list of maps for Velocity
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

                        // Log the data for debugging
                        System.out.println("Transformed data for Velocity: " + categories);

                        // Set the transformed data for the Velocity template
                        exchange.getIn().setBody(categories);

                        // Set dynamic file name
                        String fileName = "inventory_" + System.currentTimeMillis() + ".xml";
                        exchange.getIn().setHeader("CamelFileName", fileName);
                    })
                    .process(exchange -> {
                        System.out.println("Before Velocity - Body: " + exchange.getIn().getBody());
                    })
                    // Apply Velocity template to generate XML
//                    .to("velocity:file:src/main/resources/templates/inventory-template.vm")
                    .to("velocity:file:src/main/resources/templates/raw-template.vm")
                    .process(exchange -> {
                        System.out.println("After Velocity - Body: " + exchange.getIn().getBody());
                    })
                    // Write to file with absolute path
                    .to("file:C:/Users/290577/Desktop/Task/my-cart/target/output?fileName=${header.CamelFileName}&fileExist=Override")
                    .process(exchange -> {
                        System.out.println("After File Write - File: C:/Users/290577/Desktop/Task/my-cart/target/output/" + exchange.getIn().getHeader("CamelFileName"));
                    })
                    .log("XML file created successfully: ${header.CamelFileName}")
                    // Stop this branch to prevent affecting the response
                    .stop();
        }
    }

