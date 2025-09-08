package org.example.util;

import org.example.model.User;
import org.example.model.Post;
import org.example.model.Trade;
import org.example.model.Notification;

import java.util.List;

/**
 * 초보자 친화 정렬 유틸리티 (람다/Stream/Comparator 없이 for문만 사용)
 * ----------------------------------------------------
 * 이 클래스는 다양한 엔티티(User, Post, Trade, Notification)를
 * 단순 for문과 직접 비교/교환(swap) 방식으로 정렬할 수 있도록 도와줍니다.
 * <p>
 * 특징:
 * - for문 + swap 만 사용 (Java 초보자도 이해하기 쉽도록 설계)
 * - 중복 코드 방지를 위해 공통 sort() 메서드와 ComparatorLike 인터페이스 사용
 */
public class SortUtil {

    private SortUtil() { throw new AssertionError("No org.example.util.SortUtil instances for you!"); }

    // ===================== 공통 헬퍼 =====================

    /**
     * null 문자열을 안전하게 ""(빈 문자열)로 변환.
     * 문자열 비교 시 NullPointerException 방지.
     */
    private static String safeString(String value) {
        return (value == null) ? "" : value;
    }

    /**
     * 리스트에서 두 요소의 위치를 교환(swap).
     * @param list 교환 대상 리스트
     * @param i    첫 번째 인덱스
     * @param j    두 번째 인덱스
     */
    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * 공통 정렬 루프
     * ----------------------------------------------------
     * - 모든 정렬 메서드에서 공통으로 사용하는 정렬 알고리즘 (버블/선택정렬 유사)
     * - 두 원소를 비교했을 때 comp.compare(a,b) > 0이면 위치 교환
     */
    private static <T> void sort(List<T> list, ComparatorLike<T> comp) {
        for (int outer = 0; outer < list.size() - 1; outer++) {
            for (int inner = outer + 1; inner < list.size(); inner++) {
                if (comp.compare(list.get(outer), list.get(inner)) > 0) {
                    swap(list, outer, inner);
                }
            }
        }
    }

    // ===================== ComparatorLike 인터페이스 =====================

    /**
     * 초보자 버전 Comparator 인터페이스
     * ----------------------------------------------------
     * - Java 표준 Comparator와 비슷하지만 단순화된 버전
     * - compare(a,b)가 양수이면 a와 b의 자리를 바꿔야 함을 의미
     */
    private interface ComparatorLike<T> {
        int compare(T a, T b);
    }

    // ===================== 정렬 메서드 =====================

    /** 사용자 리스트를 ID 기준 오름차순으로 정렬 */
    public static void sortUsersById(List<User> users) {
        sort(users, new ComparatorLike<User>() {
            public int compare(User a, User b) {
                return a.getId().compareTo(b.getId());
            }
        });
    }

    /** 게시글 리스트를 postId 기준 오름차순으로 정렬 */
    public static void sortPostsById(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return a.getPostId() - b.getPostId();
            }
        });
    }

    /** 거래 리스트를 tradeId 기준 오름차순으로 정렬 */
    public static void sortTradesById(List<Trade> trades) {
        sort(trades, new ComparatorLike<Trade>() {
            public int compare(Trade a, Trade b) {
                return a.getTradeId() - b.getTradeId();
            }
        });
    }

    /** 알림 리스트를 notificationId 기준 오름차순으로 정렬 */
    public static void sortNotificationsById(List<Notification> notifications) {
        sort(notifications, new ComparatorLike<Notification>() {
            public int compare(Notification a, Notification b) {
                return a.getNotificationId() - b.getNotificationId();
            }
        });
    }

    // ===================== Post 특화 정렬 =====================

    /** 게시글 리스트를 가격 오름차순으로 정렬 (싼 것 → 비싼 것) */
    public static void sortPostsByPriceAsc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return a.getPrice() - b.getPrice();
            }
        });
    }

    /** 게시글 리스트를 가격 내림차순으로 정렬 (비싼 것 → 싼 것) */
    public static void sortPostsByPriceDesc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return b.getPrice() - a.getPrice();
            }
        });
    }

    /** 게시글 리스트를 작성일 기준 최신순(내림차순)으로 정렬 */
    public static void sortPostsByCreatedDesc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;   // null은 뒤로
                if (b.getCreatedAt() == null) return -1;
                // 최신순: 더 최근(b)이 앞으로 오도록 b - a 비교
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });
    }

    /** 게시글 리스트를 카테고리명 오름차순으로 정렬 (대소문자 무시) */
    public static void sortPostsByCategoryAsc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                String ca = safeString(a.getCategory()).toLowerCase();
                String cb = safeString(b.getCategory()).toLowerCase();
                return ca.compareTo(cb);
            }
        });
    }

    /**
     * 게시글 정렬 옵션 적용
     * ----------------------------------------------------
     * sortOption:
     *   1 → 가격 오름차순
     *   2 → 가격 내림차순
     *   3 → 최신순
     *   4 → 카테고리 오름차순
     *   그 외 → 최신순 (기본값)
     */
    public static void applyPostSort(List<Post> posts, int sortOption) {
        if (sortOption == 1) sortPostsByPriceAsc(posts);
        else if (sortOption == 2) sortPostsByPriceDesc(posts);
        else if (sortOption == 3) sortPostsByCreatedDesc(posts);
        else if (sortOption == 4) sortPostsByCategoryAsc(posts);
        else sortPostsByCreatedDesc(posts); // 기본값
    }
}