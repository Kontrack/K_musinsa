package com.example.musinsa;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class Product {
    public String eventId = "";
    public String brandName;
    public String brandEngName;
    public String productName;
    public String saledPrice = "";
    public String normalPrice = "";
    public List<String> imgs = new ArrayList<>();
    public JSONArray productsArray = new JSONArray();
    public JSONArray size = new JSONArray();
    public String selected_productName = "";
    public String selected_uniqueId = "";
    public String selected_option1 = "";
    public String selected_option2 = "";

    public Product(String brandNm, String brand, String prodNm) {
        this.brandName = brandNm;
        this.brandEngName = brand;
        this.productName = prodNm;
    }
}