package org.example.model;

import lombok.Getter;
import org.example.util.PriceUtil;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Post 클래스
 * -------------------
 * 중고거래 플랫폼에서 게시되는 판매 게시글(Post)을 표현하는 모델 클래스.
 * <p>
 * 특징:
 * - 직렬화 가능 (DataStore 저장/로드 시 필요)
 * - 게시글의 고유 ID, 제목, 카테고리, 가격, 상태, 판매자, 위치, 상품 상태, 설명 등을 관리
 * - Builder 패턴을 사용하여 객체 생성 (가독성과 유연한 초기화 지원)
 * - 수정 시 updatedAt 자동 갱신
 * - 삭제 여부를 boolean 플래그로 관리
 */
public class Post implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    // ===================== Getter/Setter =====================
    /** 게시글 고유 ID (DataStore에서 시퀀스를 통해 발급) */
    @Getter
    private final int postId;

    /** 게시글 제목 */
    @Getter
    private String title;

    /** 카테고리 (예: 전자기기, 의류, 가구 등) */
    @Getter
    private String category;

    /** 판매 가격 (원 단위) */
    private int priceInWon;

    /** 게시글 상태 (기본값: 판매중 ON_SALE) */
    @Getter
    private PostStatus status = PostStatus.ON_SALE;

    /** 판매자 ID */
    @Getter
    private final String sellerId;

    /** 거래 희망 위치 */
    private String preferredLocation;

    /** 상품 상태 (예: 새상품, 중고, 사용감 있음 등) */
    private ConditionLevel conditionLevel;

    /** 상세 설명 */
    @Getter
    private String description;

    /** 게시글 생성 시각 */
    @Getter
    private final LocalDateTime createdAt;

    /** 마지막 수정 시각 */
    @Getter
    private LocalDateTime updatedAt;

    /** 삭제 여부 (true=삭제됨, false=정상) */
    @Getter
    private boolean deleted = false;

    // ===================== 생성자 (private: Builder 전용) =====================
    /**
     * Builder를 통해서만 생성 가능하도록 private 처리
     */
    private Post(Builder builder) {
        this.postId = builder.postId;
        this.title = builder.title;
        this.category = builder.category;
        this.priceInWon = builder.price;
        this.sellerId = builder.sellerId;
        this.preferredLocation = builder.location;
        this.conditionLevel = builder.condition;
        this.description = builder.description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * updatedAt 값을 현재 시각으로 갱신하는 메서드
     * (게시글 수정이 일어날 때마다 호출)
     */
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===================== Builder 클래스 =====================
    /**
     * Builder 패턴으로 Post 객체를 생성하기 위한 클래스
     * - 필수값: postId, sellerId
     * - 선택값: title, category, price, location, condition, description
     */
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

        /** 최종적으로 Post 객체 생성 */
        public Post build() { return new Post(this); }
    }

    public int getPrice() { return priceInWon; }

    public String getLocation() { return preferredLocation; }
    public ConditionLevel getCondition() { return conditionLevel; }

    // ---- Setter (수정 시 updatedAt 자동 갱신) ----
    public void setTitle(String title) {
        this.title = title;
        refreshUpdatedAt();
    }
    public void setCategory(String category) {
        this.category = category;
        refreshUpdatedAt();
    }
    public void setPrice(int price) {
        this.priceInWon = price;
        refreshUpdatedAt();
    }
    public void setStatus(PostStatus status) {
        this.status = status;
        refreshUpdatedAt();
    }
    public void setLocation(String location) {
        this.preferredLocation = location;
        refreshUpdatedAt();
    }
    public void setCondition(ConditionLevel condition) {
        this.conditionLevel = condition;
        refreshUpdatedAt();
    }
    public void setDescription(String description) {
        this.description = description;
        refreshUpdatedAt();
    }

    /**
     * 게시글을 삭제 상태로 표시
     * (실제 데이터를 지우지 않고 논리적 삭제 처리)
     */
    public void markAsDeleted() {
        this.deleted = true;
        refreshUpdatedAt();
    }

    // ===================== toString =====================
    /**
     * 게시글 정보를 사람이 읽기 쉬운 문자열로 반환
     * 형식: [게시글ID] 제목 | 카테고리 | 가격 | 상태 | 판매자ID
     */
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | %s | %s원 | %s | %s",
                postId, title, category, PriceUtil.format(priceInWon), status.getLabel(), sellerId
        );
    }
}