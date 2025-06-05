package com.ust.my_cart.Dto;

import java.util.List;

public class ItemResponse {
    private String categoryName;
    private List<Items> items;
    private String categoryId;

    // Default constructor (required for Jackson)
    public ItemResponse() {
    }

    // Constructor for convenience
    public ItemResponse(String categoryName, List<Items> items, String categoryId) {
        this.categoryName = categoryName;
        this.items = items;
        this.categoryId = categoryId;
    }

    // Getters and Setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<Items> getItems() {
        return items;
    }

    public void setItems(List<Items> items) {
        this.items = items;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}