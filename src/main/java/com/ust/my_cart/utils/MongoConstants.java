package com.ust.my_cart.utils;


public class MongoConstants {

    private static final String BASE_URI = "mongodb:mycartdb?database=cart&collection=";
    public static final String FILTER = "CamelMongoDbFilter";

    // === ITEM Collection Operations ===
    public static final String SAVE_ITEM = BASE_URI + "item&operation=insert";
    public static final String FIND_ALL_ITEMS = BASE_URI + "item&operation=findAll";
    public static final String FIND_ITEM_BY_ID = BASE_URI + "item&operation=findOneByQuery";
    public static final String UPDATE_ITEM = BASE_URI + "item&operation=save";
    public static final String REMOVE_ITEM = BASE_URI + "item&operation=remove";

    // === CATEGORY Collection Operations ===
    public static final String SAVE_CATEGORY = BASE_URI + "category&operation=insert";
    public static final String FIND_ALL_CATEGORIES = BASE_URI + "category&operation=findAll";
    public static final String FIND_CATEGORY_BY_ID = BASE_URI + "category&operation=findOneByQuery";
    public static final String UPDATE_CATEGORY = BASE_URI + "category&operation=update";
    public static final String REMOVE_CATEGORY = BASE_URI + "category&operation=remove";

    // === Custom Examples ===
    public static final String FIND_ITEMS_BY_QUERY = BASE_URI + "item&operation=findAll&dynamicity=true";
    public static final String FIND_CATEGORY_BY_QUERY = BASE_URI + "category&operation=findAll&dynamicity=true";

}

