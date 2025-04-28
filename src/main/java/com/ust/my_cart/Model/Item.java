package com.ust.my_cart.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    @Field("_id")
    @JsonProperty("_id")
    private String _id;

    @JsonProperty("itemName")
    private String itemName;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("lastUpdateDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String lastUpdateDate;

    @JsonProperty("itemPrice")
    private ItemPrice itemPrice;

    @JsonProperty("stockDetails")
    private StockDetails stockDetails;

    @JsonProperty("specialProduct")
    private boolean specialProduct;

    @JsonProperty("review")
    private List<Review> review;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }


    public ItemPrice getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(ItemPrice itemPrice) {
        this.itemPrice = itemPrice;
    }

    public StockDetails getStockDetails() {
        return stockDetails;
    }

    public void setStockDetails(StockDetails stockDetails) {
        this.stockDetails = stockDetails;
    }

    public boolean isSpecialProduct() {
        return specialProduct;
    }

    public void setSpecialProduct(boolean specialProduct) {
        this.specialProduct = specialProduct;
    }

    public List<Review> getReview() {
        return review;
    }

    public void setReview(List<Review> review) {
        this.review = review;
    }

}


class Review {

    @JsonProperty("rating")
    private String rating;

    @JsonProperty("comment")
    private String comment;

    public Review() {}

    public Review(String rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
