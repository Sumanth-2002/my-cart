//package com.ust.my_cart.CamelRoutes;
//
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//import com.mongodb.client.model.Filters;
//import com.mongodb.client.model.Updates;
//import com.ust.my_cart.Exception.GlobalExceptionHandler;
//import com.ust.my_cart.Model.Category;
//import com.ust.my_cart.Model.Item;
//import com.ust.my_cart.Dto.CategoryItemsResponse;
//import com.ust.my_cart.Dto.ItemDto;
//import com.ust.my_cart.Processor.CategoryProcessor;
//import com.ust.my_cart.Dummy.CategoryRouteProcessor;
//import com.ust.my_cart.Processor.ItemProcessor;
//import com.ust.my_cart.Dummy.ItemRouteProcessor;
//import com.ust.my_cart.utils.ItemMapper;
//import com.ust.my_cart.Dummy.MongoConstants;
//import com.ust.my_cart.utils.MongoService;
//import com.ust.my_cart.Dummy.ResponseHelper;
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.model.dataformat.JsonLibrary;
//import org.apache.camel.model.rest.RestBindingMode;
//import org.bson.Document;
//import org.bson.conversions.Bson;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class ItemRoute extends RouteBuilder {
//    @Autowired
//    private ItemRouteProcessor itemProcessor;
//    @Autowired
//    private ResponseHelper responseHelper;
//    @Autowired
//    private CategoryRouteProcessor categoryProcessor;
//    @Autowired
//    private GlobalExceptionHandler globalExceptionHandler;
//    @Autowired
//    private MongoService mongoService;
//
//    @Override
//    public void configure() throws Exception {
//        onException(Exception.class).handled(true).process(globalExceptionHandler).end();
//        restConfiguration()
//                .component("netty-http")
//                .host("0.0.0.0")
//                .port(9091)
//                .bindingMode(RestBindingMode.json)
//                .dataFormatProperty("prettyPrint", "true");
//        rest("/category")
//                .post()
//                .type(Category.class)
//                .consumes("application/json")
//                .produces("application/json")
//                .to("direct:insertCategory")
//                .get()
//                .to("direct:findAllCategories")
//                .get("/{id}")
//                .to("direct:findCategoryById");
//
//        from("direct:insertCategory")
//                .routeId("insertCategoryRoute")
//                .process(categoryProcessor.insertCategoryProcessor())
//                .to(MongoConstants.SAVE_CATEGORY)
//        ;
//
//        from("direct:findAllCategories")
//                .bean(mongoService, "findAllCategories");
//        from("direct:findCategoryById")
////                .process(categoryProcessor.findCategoryByIdProcessor());
//                .to(MongoConstants.FIND_CATEGORY_BY_ID);
//
//        rest("/item")
//                .post()
//                .type(Item.class)
//                .to("direct:insertItem")
//                .get()
//                .to("direct:findAllItems")
//                .get("/{id}")
//                .to("direct:findItemById")
//                .get("/category/{categoryid}")
//                .to("direct:findItemsByCategoryId");
//
//        from("direct:insertItem")
//                .routeId("createItemRoute")
//                .process(itemProcessor.createItemProcessor());
//
//        from("direct:findItemsByCategoryId")
//                .routeId("findItemsByCategoryIdRoute")
//                .process(itemProcessor.findItemsByCategoryIdProcessor());
//
//        from("direct:findAllItems")
//                .routeId("findAllItemsRoute")
//                .bean(mongoService, "findAllItems")
//                .bean(ItemMapper.class, "mapToItemDtoList");
//
//        from("direct:findItemById")
//                .routeId("findItemByIdRoute")
//                .setBody(simple("{ '_id': '${header.id}' }"))
//                .bean(mongoService, "findItemById(${header.id})")
//                .bean(ItemMapper.class, "mapToItemDto");
//
//
//        rest("/inventory")
//                .post()
//                .consumes("application/json")
//                .to("direct:updateInventory");
//        rest("/inventory/async")
//                .post()
//                .consumes("application/json")
//                .to("direct:updateInventoryAsync");
//
//
//        from("direct:updateInventory")
//                .routeId("updateInventoryRoute")
//                .process(itemProcessor::validateAndPrepareInventoryUpdates)
//                .process(mongoService::applyInventoryUpdates)
//                .process(itemProcessor::prepareInventoryUpdateResponse)
//                .log("Returning response: ${body}");
//        from("direct:updateInventoryAsync")
//                .routeId("updateInventoryRouteAsync")
//                .process(itemProcessor::validateAndPrepareAsyncUpdate)
//                .split(body()).parallelProcessing()
//                .marshal().json(JsonLibrary.Jackson)
//                .to("activemq:queue:inventory.update.queue")
//                .end()
//                .process(itemProcessor::prepareAsyncQueueResponse);
//
//
//        from("activemq:queue:inventory.update.queue")
//                .routeId("inventoryAsyncUpdater")
//                .threads(10)
//                .unmarshal().json(JsonLibrary.Jackson, Map.class)
//                .process(mongoService::applySingleInventoryUpdate)
//                .log("Stock updated asynchronously for item: ${body}");
//
//    }
//}
