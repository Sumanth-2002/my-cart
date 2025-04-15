package com.ust.my_cart.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;


@AllArgsConstructor
@NoArgsConstructor

public class Category {

    @Field("_id")
    @Id
    @JsonProperty("_id")
    private String _id;
    private String categoryName;
    private String categoryDep;
    private String categoryTax;


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDep() {
        return categoryDep;
    }

    public void setCategoryDep(String categoryDep) {
        this.categoryDep = categoryDep;
    }

    public String getCategoryTax() {
        return categoryTax;
    }

    public void setCategoryTax(String categoryTax) {
        this.categoryTax = categoryTax;
    }
}
