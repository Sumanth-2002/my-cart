package com.ust.my_cart.CamelRoutes;

import com.ust.my_cart.Bean.ItemBean;
import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Processor.LoadCategoryDetailsProcessor;
import com.ust.my_cart.Processor.ValidateCategoryProcessor;
import com.ust.my_cart.utils.MongoConstants;
import com.ust.my_cart.Exception.GlobalExceptionHandler;
import com.ust.my_cart.utils.ResponseHelper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequirementOneRoute extends RouteBuilder {

    @Autowired
    private ItemBean itemBean;

    @Autowired
    private ValidateCategoryProcessor validateCategoryProcessor;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;
    @Autowired
    private LoadCategoryDetailsProcessor findCategoryByIdProcessor;
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
        from("direct:insertCategory")
                .routeId("insertCategoryRoute")
                .process(validateCategoryProcessor)
                .to(MongoConstants.FIND_CATEGORY_BY_ID)
                .choice()
                .when(body().isNull())
                .to(MongoConstants.SAVE_CATEGORY)
                .end()
                .bean(responseHelper, "insertCategoryResponse");


        // Route to retrieve all categories from MongoDB
        from("direct:findAllCategories")
                .to(MongoConstants.FIND_ALL_CATEGORIES);

        // Route to retrieve a category by its ID
        from("direct:findCategoryById")
                .process(findCategoryByIdProcessor)
                .to(MongoConstants.FIND_CATEGORY_BY_ID)
                .bean(responseHelper, "findCategoryByIdResponse");

        // Route to insert a new item into MongoDB
        from("direct:insertItem")
                .routeId("createItemRoute")
                .bean(itemBean, "validateItem")
                .to(MongoConstants.SAVE_ITEM)
                .bean(responseHelper, "itemInsertionResponse");

        // Route to find items by category ID
        from("direct:findItemsByCategoryId")
                .routeId("findItemsByCategoryIdRoute")
                .setBody(simple("{ '_id': '${header.categoryid}' }"))
                .to("mongodb:myMongoClient?database=cart&collection=category&operation=findOneByQuery")
//                .to(MongoConstants.FIND_CATEGORY_BY_ID)
                .bean(itemBean,"mapCategoryDetails")
                .bean(itemBean, "buildCategoryItemQueryWithSpecialFilter")
                .to(MongoConstants.FIND_ITEMS_BY_QUERY)
                .bean(responseHelper, "findItemsByCategoryIdResponse");

        // Route to retrieve all items from MongoDB
        from("direct:findAllItems")
                .routeId("findAllItemsRoute")
                .to(MongoConstants.FIND_ALL_ITEMS);

        // Route; to retrieve an item by its ID
        from("direct:findItemById")
                .routeId("findItemByIdRoute")
                .setBody(simple("{ '_id': '${header.id}' }"))
                .setHeader(MongoDbConstants.FIELDS_PROJECTION, constant("{ 'review': 0,'stockDetails.soldOut':0,'stockDetails.damaged':0,'lastUpdateDate':0 }"))
                .to("mongodb:myMongoClient?database=cart&collection=item&operation=findOneByQuery")
                .log("Fetched item: ${body}")
                .process(exchange -> {
                    Document item = exchange.getIn().getBody(Document.class);
                    if (item == null) {
                        throw new ProcessException("Item not found",404);
                    }
                    Object categoryId = item.get("categoryId");
                    exchange.setProperty("itemDocument", item); // Save for later
                    exchange.getIn().setBody(new Document("_id", categoryId));
                })
                .to("mongodb:myMongoClient?database=cart&collection=category&operation=findOneByQuery")
                .log("Fetched category: ${body}")
                .process(exchange -> {
                    Document category = exchange.getIn().getBody(Document.class);
                    String categoryName = category != null ? category.getString("categoryName") : null;
                    Document item = exchange.getProperty("itemDocument", Document.class);
                    item.remove("categoryId");
                    item.put("categoryName", categoryName);
                    exchange.getIn().setBody(item);
                })
                .log("Final transformed item: ${body}");




        from("direct:updateInventory")
                .routeId("updateInventoryRoute")
                .bean(itemBean,"validateInventoryUpdates")
                .split(simple("${exchangeProperty.updates}")).streaming()
                .setHeader("CamelMongoDbCriteria", simple("{ \"_id\": \"${body[itemId]}\" }"))
                .setProperty("itemId", simple("${body[itemId]}"))
                .setProperty("soldout", simple("${body[soldout]}"))
                .setProperty("damaged", simple("${body[damaged]}"))
                .to(MongoConstants.FIND_ITEM_BY_ID)
                .bean(itemBean, "prepareInventoryUpdates")
                .choice()
                .when(simple("${exchangeProperty.skipSave} == false"))
                .to(MongoConstants.UPDATE_ITEM)
                .end()
                .end()
                .bean(responseHelper, "prepareInventoryUpdateResponse")
                .log("Returning response: ${body}");

    }
}


