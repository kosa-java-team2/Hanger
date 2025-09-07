package org.example.util;

import org.example.model.User;
import org.example.model.Post;
import org.example.model.Trade;
import org.example.model.Notification;

import java.util.List;

/**
 * 초보자 친화 정렬 유틸리티
 * - Stream/Comparator 없이 for문만 사용
 * - 서비스 코드에서 바로 호출해서 사용
 */
public class SortUtil {

    private SortUtil() { throw new AssertionError("No org.example.util.SortUtil instances for you!"); }

    // ===================== 공통 헬퍼 =====================
    private static String safeString(String value) { return (value == null) ? "" : value; }

    private static void swapPosts(List<Post> posts, int leftIndex, int rightIndex) {
        Post tempPost = posts.get(leftIndex);
        posts.set(leftIndex, posts.get(rightIndex));
        posts.set(rightIndex, tempPost);
    }

    // ===================== 기본 ID 정렬 =====================
    /** 사용자 리스트를 ID 기준 오름차순으로 정렬 */
    public static void sortUsersById(List<User> users) {
        for (int outer = 0; outer < users.size() - 1; outer++) {
            for (int inner = outer + 1; inner < users.size(); inner++) {
                if (users.get(outer).getId().compareTo(users.get(inner).getId()) > 0) {
                    User tempUser = users.get(outer);
                    users.set(outer, users.get(inner));
                    users.set(inner, tempUser);
                }
            }
        }
    }

    /** 게시글 리스트를 postId 기준 오름차순으로 정렬 */
    public static void sortPostsById(List<Post> posts) {
        for (int outer = 0; outer < posts.size() - 1; outer++) {
            for (int inner = outer + 1; inner < posts.size(); inner++) {
                if (posts.get(outer).getPostId() > posts.get(inner).getPostId()) {
                    swapPosts(posts, outer, inner);
                }
            }
        }
    }

    /** 거래 리스트를 tradeId 기준 오름차순으로 정렬 */
    public static void sortTradesById(List<Trade> trades) {
        for (int outer = 0; outer < trades.size() - 1; outer++) {
            for (int inner = outer + 1; inner < trades.size(); inner++) {
                if (trades.get(outer).getTradeId() > trades.get(inner).getTradeId()) {
                    Trade tempTrade = trades.get(outer);
                    trades.set(outer, trades.get(inner));
                    trades.set(inner, tempTrade);
                }
            }
        }
    }

    /** 알림 리스트를 notificationId 기준 오름차순으로 정렬 */
    public static void sortNotificationsById(List<Notification> notifications) {
        for (int outer = 0; outer < notifications.size() - 1; outer++) {
            for (int inner = outer + 1; inner < notifications.size(); inner++) {
                if (notifications.get(outer).getNotificationId() > notifications.get(inner).getNotificationId()) {
                    Notification tempNotification = notifications.get(outer);
                    notifications.set(outer, notifications.get(inner));
                    notifications.set(inner, tempNotification);
                }
            }
        }
    }

    // ===================== Post 특화 정렬 =====================
    /** 가격 오름차순 */
    public static void sortPostsByPriceAsc(List<Post> posts) {
        for (int outer = 0; outer < posts.size() - 1; outer++) {
            for (int inner = outer + 1; inner < posts.size(); inner++) {
                if (posts.get(outer).getPrice() > posts.get(inner).getPrice()) {
                    swapPosts(posts, outer, inner);
                }
            }
        }
    }

    /** 가격 내림차순 */
    public static void sortPostsByPriceDesc(List<Post> posts) {
        for (int outer = 0; outer < posts.size() - 1; outer++) {
            for (int inner = outer + 1; inner < posts.size(); inner++) {
                if (posts.get(outer).getPrice() < posts.get(inner).getPrice()) {
                    swapPosts(posts, outer, inner);
                }
            }
        }
    }

    /** 최신순(작성일 내림차순) */
    public static void sortPostsByCreatedDesc(List<Post> posts) {
        for (int outer = 0; outer < posts.size() - 1; outer++) {
            for (int inner = outer + 1; inner < posts.size(); inner++) {
                if (posts.get(outer).getCreatedAt().isBefore(posts.get(inner).getCreatedAt())) {
                    swapPosts(posts, outer, inner);
                }
            }
        }
    }

    /** 카테고리 오름차순(대소문자 무시) */
    public static void sortPostsByCategoryAsc(List<Post> posts) {
        for (int outer = 0; outer < posts.size() - 1; outer++) {
            for (int inner = outer + 1; inner < posts.size(); inner++) {
                String categoryA = safeString(posts.get(outer).getCategory()).toLowerCase();
                String categoryB = safeString(posts.get(inner).getCategory()).toLowerCase();
                if (categoryA.compareTo(categoryB) > 0) {
                    swapPosts(posts, outer, inner);
                }
            }
        }
    }

    /** 정렬 옵션 적용(ComparatorFactory 대체: 1.가격↑ 2.가격↓ 3.최신 4.카테고리) */
    public static void applyPostSort(List<Post> posts, int sortOption) {
        if (sortOption == 1)       sortPostsByPriceAsc(posts);
        else if (sortOption == 2)  sortPostsByPriceDesc(posts);
        else if (sortOption == 3)  sortPostsByCreatedDesc(posts);
        else if (sortOption == 4)  sortPostsByCategoryAsc(posts);
        else                       sortPostsByCreatedDesc(posts); // 기본값: 최신순
    }
}