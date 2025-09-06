package org.example.model;

import org.example.util.PriceUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Post implements Serializable {
    private static final long serialVersionUID = 1L;

    private int postId;
    private String title;
    private String category;
    private int price;
    private PostStatus status = PostStatus.ON_SALE;
    private String sellerId;
    private String location;
    private ConditionLevel condition;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean isDeleted = false;

    public Post() {}

    public Post(int postId, String title, String category, int price,
                String sellerId, String location, ConditionLevel condition, String description) {
        this.postId = postId;
        this.title = title;
        this.category = category;
        this.price = price;
        this.sellerId = sellerId;
        this.location = location;
        this.condition = condition;
        this.description = description;
    }

    public void touch() { this.updatedAt = LocalDateTime.now(); }

    // getters/setters
    public int getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public int getPrice() { return price; }
    public PostStatus getStatus() { return status; }
    public String getSellerId() { return sellerId; }
    public String getLocation() { return location; }
    public ConditionLevel getCondition() { return condition; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isDeleted() { return isDeleted; }

    public void setTitle(String title) { this.title = title; touch(); }
    public void setCategory(String category) { this.category = category; touch(); }
    public void setPrice(int price) { this.price = price; touch(); }
    public void setStatus(PostStatus status) { this.status = status; touch(); }
    public void setLocation(String location) { this.location = location; touch(); }
    public void setCondition(ConditionLevel condition) { this.condition = condition; touch(); }
    public void setDescription(String description) { this.description = description; touch(); }
    public void markDeleted() { this.isDeleted = true; touch(); }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s원 | %s | %s",
            postId, title, category, PriceUtil.format(price), status, sellerId);
    }
}