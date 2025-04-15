//package com.ust.my_cart.config;
//
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//import org.apache.camel.component.mongodb.MongoDbComponent;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MongoConfig {
//
//    @Bean
//    public MongoClient mongoClient() {
//        return MongoClients.create("mongodb://localhost:27017");
//    }
//
//    @Bean
//    public MongoDbComponent mongoDbComponent(MongoClient mongoClient) {
//        MongoDbComponent component = new MongoDbComponent();
//        component.setMongoConnection(mongoClient);
//        return component;
//    }
//}
