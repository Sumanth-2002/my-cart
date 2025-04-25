package com.ust.my_cart.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryItemsResponse {
    private String categoryName;
    private String categoryDepartment;
    private List<ItemDto> items;
    private String message;
    // Getters & Setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDepartment() {
        return categoryDepartment;
    }

    public void setCategoryDepartment(String categoryDepartment) {
        this.categoryDepartment = categoryDepartment;
    }

    public List<ItemDto> getItems() {
        return items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
