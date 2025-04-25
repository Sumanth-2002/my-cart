package com.ust.my_cart.Dummy;



import com.ust.my_cart.Exception.ProcessException;
import com.ust.my_cart.Model.Category;
import com.ust.my_cart.utils.MongoService;
import org.apache.camel.Processor;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class CategoryRouteProcessor {
    private final MongoService mongoService;

    public CategoryRouteProcessor(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    public Processor findCategoryByIdProcessor() {
        return exchange -> {
            String categoryId = exchange.getIn().getHeader("_id", String.class);

            if (categoryId == null || categoryId.trim().isEmpty()) {
                throw new ProcessException("Missing required header: id", 400);
            }

            Document category = mongoService.findCategoryById(categoryId);
            if (category == null) {
                throw new ProcessException("Category with ID " + categoryId + " not found", 404);
            }

            exchange.getIn().setBody(category);
        };
    }
    public Processor insertCategoryProcessor() {
        return exchange -> {
            Category category = exchange.getIn().getBody(Category.class);

            if (category.get_id() == null || category.get_id().trim().isEmpty()) {
                throw new ProcessException("Category _id is required", 400);
            }

            Document existingCategory = exchange.getContext().createProducerTemplate()
                    .requestBody("direct:findCategoryById", new Document("_id", category.get_id()), Document.class);

            if (existingCategory != null) {
                throw new ProcessException("Category with this ID already exists", 409);
            }};
    }

}
