package org.example.model;

import org.example.util.PriceUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Post implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int postId;
    private String title;
    private String category;
    private int price;
    private PostStatus status = PostStatus.ON_SALE;
    private final String sellerId;
    private String location;
    private ConditionLevel condition;
    private String description;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted = false;

    // 🔒 private 생성자: Builder만 사용 가능
    private Post(Builder builder) {
        this.postId = builder.postId;
        this.title = builder.title;
        this.category = builder.category;
        this.price = builder.price;
        this.sellerId = builder.sellerId;
        this.location = builder.location;
        this.condition = builder.condition;
        this.description = builder.description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void touch() { this.updatedAt = LocalDateTime.now(); }

    // ====== Builder 클래스 ======
    public static class Builder {
        private final int postId;
        private final String sellerId;
        private String title;
        private String category;
        private int price;
        private String location;
        private ConditionLevel condition;
        private String description;

        public Builder(int postId, String sellerId) {
            this.postId = postId;
            this.sellerId = sellerId;
        }

        public Builder title(String title) { this.title = title; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder price(int price) { this.price = price; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder condition(ConditionLevel condition) { this.condition = condition; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public Post build() { return new Post(this); }
    }

    // ====== Getters/Setters ======
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
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