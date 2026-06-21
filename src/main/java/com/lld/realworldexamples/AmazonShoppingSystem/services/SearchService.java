package com.lld.realworldexamples.AmazonShoppingSystem.services;

import com.lld.realworldexamples.AmazonShoppingSystem.enums.ProductCategory;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Product;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SearchService {
    private final Collection<Product> productCatalog;

    public SearchService(Collection<Product> productCatalog) {
        this.productCatalog = productCatalog;
    }

    public List<Product> searchByName(String name) {
        return productCatalog.stream()
                .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Product> searchByCategory(ProductCategory category) {
        return productCatalog.stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }
}
