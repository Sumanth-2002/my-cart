package com.ust.my_cart.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StockDetails {

    @JsonProperty("availableStock")
    private int availableStock;

    @JsonProperty("unitOfMeasure")
    private String unitOfMeasure;

    public StockDetails() {}

    public StockDetails(int availableStock, String unitOfMeasure) {
        this.availableStock = availableStock;
        this.unitOfMeasure = unitOfMeasure;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
