package com.ust.my_cart.Dto;

public class Items {
    private String itemId;
    private Object sellingPrice;
    private Object availableStock;
    private String categoryId;

    public Items() {
    }

    // Constructor for convenience
    public Items(String itemId, Object sellingPrice, Object availableStock, String categoryId) {
        this.itemId = itemId;
        this.sellingPrice = sellingPrice;
        this.availableStock = availableStock;
        this.categoryId = categoryId;
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Object getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Object sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Object getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Object availableStock) {
        this.availableStock = availableStock;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}