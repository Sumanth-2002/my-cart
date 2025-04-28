package com.ust.my_cart.utils;

import com.ust.my_cart.Model.ItemPrice;
import com.ust.my_cart.Model.StockDetails;
import com.ust.my_cart.Dto.ItemDto;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ItemMapper {

    public List<ItemDto> mapToItemDtoList(List<Document> documents) {
        List<ItemDto> itemDtos = new ArrayList<>();
        for (Document doc : documents) {
            itemDtos.add(mapSingleItem(doc));
        }
        return itemDtos;
    }

    public ItemDto mapToItemDto(Document doc) {
        return mapSingleItem(doc);
    }

    private ItemDto mapSingleItem(Document doc) {
        if (doc == null) return null;

        ItemDto dto = new ItemDto();
        dto.set_id(doc.getString("_id"));
        dto.setItemName(doc.getString("itemName"));
        dto.setCategoryName(doc.getString("categoryId")); // can be replaced with enriched name
        dto.setSpecialProduct(doc.getBoolean("specialProduct", false));

        Document priceDoc = doc.get("itemPrice", Document.class);
        if (priceDoc != null) {
            ItemPrice price = new ItemPrice();
            price.setBasePrice(priceDoc.getDouble("basePrice"));
            price.setSellingPrice(priceDoc.getDouble("sellingPrice"));
            dto.setItemPrice(price);
        }

        // stockDetails
        Document stockDoc = doc.get("stockDetails", Document.class);
        if (stockDoc != null) {
            StockDetails stock = new StockDetails();
            stock.setAvailableStock(stockDoc.getInteger("availableStock", 0));
            stock.setUnitOfMeasure(stockDoc.getString("unitOfMeasure"));
            dto.setStockDetails(stock);
        }



        return dto;
    }

}

