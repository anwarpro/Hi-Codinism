package com.teamblackhole.realtimechat.himan;

import java.util.List;

public class AppCategory {
    private String company;
    private List<String> categories;

    public AppCategory(String company, List<String> categories) {
        this.company = company;
        this.categories = categories;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(String category) {
        this.categories.add(category);
    }
}
