package com.ust.my_cart.CamelRoutes;

import com.ust.my_cart.utils.MongoConstants;
import com.ust.my_cart.Exception.GlobalExceptionHandler;
import com.ust.my_cart.Processor.CategoryProcessor;
import com.ust.my_cart.Processor.ItemProcessor;
import com.ust.my_cart.utils.ItemMapper;
import com.ust.my_cart.utils.MongoService;
import com.ust.my_cart.utils.ResponseHelper;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequirementOneRoute extends RouteBuilder {

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

        // Global exception handler for all exceptions in this route
        onException(Exception.class)
                .handled(true)
                .process(globalExceptionHandler)
                .end();

        // REST endpoints for category and item operations
        // Route to insert a new category into MongoDB
        from("direct:insertCategory")
                .routeId("insertCategoryRoute")
                .bean(categoryProcessor, "validateCategory")
                .to(MongoConstants.SAVE_CATEGORY)
                .bean(responseHelper, "insertCategoryResponse");

        // Route to retrieve all categories from MongoDB
        from("direct:findAllCategories")
                .to(MongoConstants.FIND_ALL_CATEGORIES);

        // Route to retrieve a category by its ID
        from("direct:findCategoryById")
                .process(categoryProcessor.findCategoryByIdProcessor())
                .to(MongoConstants.FIND_CATEGORY_BY_ID)
                .bean(responseHelper, "findCategoryByIdResponse");

        // Route to insert a new item into MongoDB
        from("direct:insertItem")
                .routeId("createItemRoute")
                .bean(ItemProcessor.class, "createItemProcessor")
                .to(MongoConstants.SAVE_ITEM)
                .bean(responseHelper, "itemInsertionResponse");

        // Route to find items by category ID
        from("direct:findItemsByCategoryId")
                .routeId("findItemsByCategoryIdRoute")
                .bean(itemProcessor, "findItemsByCategoryId")
                .to(MongoConstants.FIND_ITEMS_BY_QUERY)
                .bean(responseHelper, "findItemsByCategoryIdResponse");

        // Route to retrieve all items from MongoDB
        from("direct:findAllItems")
                .routeId("findAllItemsRoute")
                .to(MongoConstants.FIND_ALL_ITEMS)
                .bean(itemMapper, "mapToItemDtoList");

        // Route to retrieve an item by its ID
        from("direct:findItemById")
                .routeId("findItemByIdRoute")
                .setBody(simple("{ '_id': '${header.id}' }"))
                .to(MongoConstants.FIND_ITEM_BY_ID)
                .bean(itemMapper, "mapToItemDto");

        // Route to update inventory for items
//        from("direct:updateInventory")
//                .routeId("updateInventoryRoute")
//                .process(itemProcessor::validateAndPrepareInventoryUpdates)
//                .process(mongoService::applyInventoryUpdates)
//                .process(itemProcessor::prepareInventoryUpdateResponse)
//                .log("Returning response: ${body}");

        from("direct:updateInventory")
                .routeId("updateInventoryRoute")

                .process(itemProcessor::validateAndPrepareInventoryUpdates)

                .split(simple("${exchangeProperty.updates}")).streaming()
                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${body[itemId]}\" }"))
                .setProperty("itemId", simple("${body[itemId]}"))
                .setProperty("totalReduction", simple("${body[totalReduction]}"))
                .to("mongodb:mycartdb?database=cart&collection=item&operation=findOneByQuery")
                .bean(itemProcessor, "updateStock")
                .choice()
                .when(simple("${exchangeProperty.skipSave} == false"))
                .to("mongodb:mycartdb?database=cart&collection=item&operation=save")
                .end()
                .end() // <--- This ends the split block

                // Only executed once after all splits
                .bean(itemProcessor,"prepareInventoryUpdateResponse")
                .log("Returning response: ${body}");


    }
}


