package com.teamblackhole.realtimechat.himan;

public class FacebookPage {
    private String category;
    private String id;
    private String name;

    public FacebookPage() {
    }

    public FacebookPage(String category, String id, String name) {
        this.category = category;
        this.id = id;
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
