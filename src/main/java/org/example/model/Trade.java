package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    private int tradeId;
    private int postId;
    private String buyerId;
    private String sellerId;
    private TradeStatus status = TradeStatus.REQUESTED;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    private Boolean buyerRatedGood;   // null=미평가, true=good, false=bad
    private Boolean sellerRatedGood;  // 선택적 사용

    public Trade() {}

    public Trade(int tradeId, int postId, String buyerId, String sellerId) {
        this.tradeId = tradeId;
        this.postId = postId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
    }

    public int getTradeId() { return tradeId; }
    public int getPostId() { return postId; }
    public String getBuyerId() { return buyerId; }
    public String getSellerId() { return sellerId; }
    public TradeStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getBuyerRatedGood() { return buyerRatedGood; }
    public Boolean getSellerRatedGood() { return sellerRatedGood; }

    public void setStatus(TradeStatus status) { this.status = status; touch(); }
    public void markAccepted() { this.status = TradeStatus.ACCEPTED; touch(); }
    public void markInProgress() { this.status = TradeStatus.IN_PROGRESS; touch(); }
    public void markCompleted() { this.status = TradeStatus.COMPLETED; touch(); }
    public void markCancelled() { this.status = TradeStatus.CANCELLED; touch(); }

    public void rateByBuyer(boolean good) { this.buyerRatedGood = good; touch(); }
    public void rateBySeller(boolean good) { this.sellerRatedGood = good; touch(); }

    private void touch() { this.updatedAt = LocalDateTime.now(); }

    @Override
    public String toString() {
        return String.format("Trade[%d] post=%d buyer=%s seller=%s status=%s",
                tradeId, postId, buyerId, sellerId, status);
    }
}


