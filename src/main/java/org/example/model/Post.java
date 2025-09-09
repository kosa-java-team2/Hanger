package org.example.model;

import lombok.Getter;
import org.example.util.PriceUtil;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Post 클래스
 * -------------------
 * 중고거래 플랫폼에서 판매자가 등록하는 판매 게시글(Post)을 표현하는 모델 클래스.
 * <p>
 * 특징:
 * - 직렬화 가능 (Serializable) → DataStore 저장/로드 시 필요
 * - 게시글의 고유 ID, 제목, 카테고리, 가격, 상태, 판매자, 위치, 상품 상태, 설명 등을 관리
 * - Builder 패턴으로 객체 생성 → 필수/선택 필드를 구분하여 유연하고 가독성 있는 초기화 지원
 * - 수정(setter) 시 updatedAt 필드가 자동 갱신됨
 * - 삭제는 실제 객체 제거가 아닌 deleted 플래그(true/false)로 관리 (논리적 삭제)
 */
public class Post implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 필드 =====================

    /**
     * 게시글 고유 ID (DataStore의 nextPostId() 시퀀스를 통해 발급됨)
     */
    @Getter
    private final int postId;

    /**
     * 게시글 제목
     */
    @Getter
    private String title;

    /**
     * 카테고리 (예: 전자기기, 의류, 가구 등)
     */
    @Getter
    private String category;

    /**
     * 판매 가격 (원 단위, 정수 저장)
     */
    private int priceInWon;

    /**
     * 게시글 상태
     * - 기본값: 판매중(ON_SALE)
     * - PostStatus enum 사용 (예: 판매중, 거래중, 거래완료 등)
     */
    @Getter
    private PostStatus status = PostStatus.ON_SALE;

    /**
     * 판매자(User) ID
     */
    @Getter
    private final String sellerId;

    /**
     * 거래 희망 위치 (예: 서울 강남, 부산 서면 등)
     */
    private String preferredLocation;

    /**
     * 상품 상태 (ConditionLevel enum: 상/중/하)
     */
    private ConditionLevel conditionLevel;

    /**
     * 상세 설명 (상품 특징, 상태, 추가 정보 등)
     */
    @Getter
    private String description;

    /**
     * 게시글 생성 시각 (객체 생성 시 자동 기록)
     */
    @Getter
    private final LocalDateTime createdAt;

    /**
     * 마지막 수정 시각 (Setter 호출 시마다 갱신됨)
     */
    @Getter
    private LocalDateTime updatedAt;

    /**
     * 삭제 여부 (true=삭제됨, false=정상 게시글)
     */
    @Getter
    private boolean deleted = false;

    // ===================== 생성자 (private: Builder 전용) =====================

    /**
     * Builder 패턴을 통한 객체 생성만 허용하기 위해 private 처리.
     * - 생성 시 createdAt, updatedAt은 현재 시각(LocalDateTime.now())으로 초기화됨.
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

    // ===================== 유틸리티 메서드 =====================

    /**
     * updatedAt 값을 현재 시각으로 갱신한다.
     * - Setter 또는 게시글 변경 시 자동 호출되어 "마지막 수정일"을 보장한다.
     */
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===================== Builder 클래스 =====================

    /**
     * Builder 패턴을 통해 Post 객체를 생성하기 위한 정적 내부 클래스.
     * <p>
     * 필수값:
     * - postId (DataStore에서 발급)
     * - sellerId (작성자 ID)
     * <p>
     * 선택값:
     * - title, category, price, location, condition, description
     * <p>
     * 사용 예시:
     * Post post = new Post.Builder(postId, sellerId)
     * .title("아이폰 14 판매")
     * .category("전자기기")
     * .price(1000000)
     * .condition(ConditionLevel.HIGH)
     * .description("사용감 거의 없는 아이폰 14 판매합니다.")
     * .build();
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

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder condition(ConditionLevel condition) {
            this.condition = condition;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 최종적으로 Post 객체 생성
         */
        public Post build() {
            return new Post(this);
        }
    }

    // ===================== Getter (직접 정의된 것) =====================

    /**
     * 가격 반환 (원 단위)
     */
    public int getPrice() {
        return priceInWon;
    }

    /**
     * 거래 희망 위치 반환
     */
    public String getLocation() {
        return preferredLocation;
    }

    /**
     * 상품 상태 반환 (상/중/하)
     */
    public ConditionLevel getCondition() {
        return conditionLevel;
    }

    // ===================== Setter (수정 시 updatedAt 자동 갱신) =====================
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

    // ===================== 삭제 처리 =====================

    /**
     * 게시글을 삭제 상태로 표시한다.
     * - 실제 객체 제거 대신 deleted 플래그를 true로 바꿔 "논리적 삭제" 처리.
     * - 데이터 무결성을 유지하면서도 사용자가 보지 못하도록 할 때 유용하다.
     */
    public void markAsDeleted() {
        this.deleted = true;
        refreshUpdatedAt();
    }

    // ===================== toString =====================

    /**
     * 게시글 요약 정보를 문자열로 반환.
     * <p>
     * 출력 형식:
     * [게시글ID] 제목 | 카테고리 | 가격 | 상태 | 판매자ID
     * <p>
     * 예시:
     * [1001] 아이폰 14 판매 | 전자기기 | 1,000,000원 | 판매중 | user123
     */
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | %s | %s원 | %s | %s",
                postId,
                title,
                category,
                PriceUtil.format(priceInWon), // 가격을 "1,000,000"처럼 포맷팅
                status.getLabel(),
                sellerId
        );
    }
}