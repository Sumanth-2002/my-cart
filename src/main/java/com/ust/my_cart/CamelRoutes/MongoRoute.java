package com.ust.my_cart.CamelRoutes;

import com.ust.my_cart.utils.MongoConstants;
import com.ust.my_cart.Exception.GlobalExceptionHandler;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.Model.Item;
import com.ust.my_cart.Processor.CategoryProcessor;
import com.ust.my_cart.Processor.ItemProcessor;
import com.ust.my_cart.utils.ItemMapper;
import com.ust.my_cart.utils.MongoService;
import com.ust.my_cart.utils.ResponseHelper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;

@Component
public class MongoRoute extends RouteBuilder {
    @Autowired
    private ItemProcessor itemProcessor;
    @Autowired
    private CategoryProcessor categoryProcessor;
    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;
    @Autowired
    private MongoService mongoService;

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private ResponseHelper responseHelper;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).handled(true).process(globalExceptionHandler).end();
        restConfiguration()
                .component("netty-http")
                .host("0.0.0.0")
                .port(9091)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true");
        rest("/myCart")
                .post("/category/addCategory")
                .type(Category.class)
                .consumes("application/json")
                .produces("application/json")
                .to("direct:insertCategory")
                .get("/category")
                .to("direct:findAllCategories")
                .get("/category/{id}")
                .to("direct:findCategoryById")
                .post("/item/addItem")
                .type(Item.class)
                .to("direct:insertItem")
                .get("/item")
                .to("direct:findAllItems")
                .get("/item/{id}")
                .to("direct:findItemById")
                .get("/item/category/{categoryid}")
                .to("direct:findItemsByCategoryId")
                .post("/item/update")
                .consumes("application/json")
                .to("direct:updateInventory")
                .post("/item/update/async")
                .consumes("application/json")
                .to("direct:updateInventoryAsync");

        from("direct:insertCategory")
                .routeId("insertCategoryRoute")
                .bean(categoryProcessor, "insertCategoryProcessor")
                .to(MongoConstants.SAVE_CATEGORY).bean(responseHelper, "categorySuccessResponse");
        from("direct:findAllCategories")
                .to(MongoConstants.FIND_ALL_CATEGORIES);
        from("direct:findCategoryById")
                .process(categoryProcessor.findCategoryByIdProcessor())
                .to(MongoConstants.FIND_CATEGORY_BY_ID)
                .bean(responseHelper, "handleCategoryResponseProcessor");
        from("direct:insertItem")
                .routeId("createItemRoute")
                .bean(ItemProcessor.class, "createItemProcessor")
                .to(MongoConstants.SAVE_ITEM)
                .bean(responseHelper, "handleItemInsertion");

        from("direct:findItemsByCategoryId")
                .routeId("findItemsByCategoryIdRoute")
                .bean(itemProcessor, "findItemsByCategoryId")
                .to(MongoConstants.FIND_ITEMS_BY_QUERY)
                .bean(responseHelper, "findItemsByCategoryIdResponse");
        from("direct:findAllItems")
                .routeId("findAllItemsRoute")
                .to(MongoConstants.FIND_ALL_ITEMS)
                .bean(itemMapper, "mapToItemDtoList");

        from("direct:findItemById")
                .routeId("findItemByIdRoute")
                .setBody(simple("{ '_id': '${header.id}' }"))
                .to(MongoConstants.FIND_ITEM_BY_ID)
                .bean(itemMapper, "mapToItemDto");

        from("direct:updateInventory")
                .routeId("updateInventoryRoute")
                .process(itemProcessor::validateAndPrepareInventoryUpdates)
                .process(mongoService::applyInventoryUpdates)
                .process(itemProcessor::prepareInventoryUpdateResponse)
                .log("Returning response: ${body}");

        from("direct:updateInventoryAsync")
                .routeId("updateInventoryRouteAsync")
                .process(itemProcessor::validateAndPrepareAsyncUpdate)
                .split(body()).parallelProcessing()
                .marshal().json(JsonLibrary.Jackson)
                .to("activemq:queue:inventory.update.queue")
                .end();


        from("activemq:queue:inventory.update.queue")
                .routeId("inventoryAsyncUpdater")
                .threads(10)
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .process(mongoService::applySingleInventoryUpdate)
                .log("Stock updated asynchronously for item: ${body}");

    }
}
