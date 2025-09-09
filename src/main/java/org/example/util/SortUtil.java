package org.example.util;

import org.example.model.User;
import org.example.model.Post;
import org.example.model.Trade;
import org.example.model.Notification;

import java.util.List;

/**
 * SortUtil
 * -------------------
 * 초보자 친화 정렬 유틸리티 클래스.
 * <p>
 * 특징:
 * - Java의 Comparator/람다/Stream을 사용하지 않고 오직 for문 + swap으로 정렬 구현
 * - 단순한 알고리즘(선택/버블 정렬 유사)으로 작성 → 초보자가 이해하기 쉬움
 * - 중복 방지를 위해 공통 sort() 메서드와 ComparatorLike 인터페이스 사용
 * <p>
 * 한계:
 * - 시간 복잡도 O(n^2) → 데이터가 많을 경우 성능 저하 발생
 * - 학습/데모 용도로 적합, 실무에서는 Collections.sort() 또는 Stream API 권장
 */
public class SortUtil {

    /**
     * 인스턴스화 방지
     */
    private SortUtil() {
        throw new AssertionError("No org.example.util.SortUtil instances for you!");
    }

    // ===================== 공통 헬퍼 =====================

    /**
     * null 안전 문자열 변환
     * - null이면 "" 반환
     * - 문자열 비교 시 NullPointerException 방지
     */
    private static String safeString(String value) {
        return (value == null) ? "" : value;
    }

    /**
     * 리스트 내 두 원소의 위치를 교환
     *
     * @param list 대상 리스트
     * @param i    첫 번째 인덱스
     * @param j    두 번째 인덱스
     */
    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * 공통 정렬 로직
     * ----------------------------------------------------
     * - for문 2중 루프 기반
     * - comp.compare(a,b) > 0 인 경우 swap 수행
     * - 선택/버블 정렬과 유사
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
     * 초보자 친화 버전 Comparator 인터페이스
     * - compare(a,b) > 0 → 두 요소의 자리를 바꿔야 함
     * - Java 표준 Comparator와 유사하지만 단순화됨
     */
    private interface ComparatorLike<T> {
        int compare(T a, T b);
    }

    // ===================== 정렬 메서드 =====================

    /**
     * 사용자 리스트를 ID 기준 오름차순 정렬
     */
    public static void sortUsersById(List<User> users) {
        sort(users, new ComparatorLike<User>() {
            public int compare(User a, User b) {
                return a.getId().compareTo(b.getId());
            }
        });
    }

    /**
     * 게시글 리스트를 postId 기준 오름차순 정렬
     */
    public static void sortPostsById(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return a.getPostId() - b.getPostId();
            }
        });
    }

    /**
     * 거래 리스트를 tradeId 기준 오름차순 정렬
     */
    public static void sortTradesById(List<Trade> trades) {
        sort(trades, new ComparatorLike<Trade>() {
            public int compare(Trade a, Trade b) {
                return a.getTradeId() - b.getTradeId();
            }
        });
    }

    /**
     * 알림 리스트를 notificationId 기준 오름차순 정렬
     */
    public static void sortNotificationsById(List<Notification> notifications) {
        sort(notifications, new ComparatorLike<Notification>() {
            public int compare(Notification a, Notification b) {
                return a.getNotificationId() - b.getNotificationId();
            }
        });
    }

    // ===================== Post 특화 정렬 =====================

    /**
     * 게시글 리스트를 가격 오름차순(저가 → 고가)으로 정렬
     */
    public static void sortPostsByPriceAsc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return a.getPrice() - b.getPrice();
            }
        });
    }

    /**
     * 게시글 리스트를 가격 내림차순(고가 → 저가)으로 정렬
     */
    public static void sortPostsByPriceDesc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                return b.getPrice() - a.getPrice();
            }
        });
    }

    /**
     * 게시글 리스트를 작성일 기준 최신순(최근 → 오래된)으로 정렬
     */
    public static void sortPostsByCreatedDesc(List<Post> posts) {
        sort(posts, new ComparatorLike<Post>() {
            public int compare(Post a, Post b) {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;   // null은 뒤로 보냄
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt()); // 최신순
            }
        });
    }

    /**
     * 게시글 리스트를 카테고리명 오름차순(알파벳/가나다 순) 정렬
     */
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
     * sortOption 값:
     * 1 → 가격 오름차순
     * 2 → 가격 내림차순
     * 3 → 최신순
     * 4 → 카테고리 오름차순
     * 그 외 → 최신순 (기본값)
     */
    public static void applyPostSort(List<Post> posts, int sortOption) {
        if (sortOption == 1) sortPostsByPriceAsc(posts);
        else if (sortOption == 2) sortPostsByPriceDesc(posts);
        else if (sortOption == 3) sortPostsByCreatedDesc(posts);
        else if (sortOption == 4) sortPostsByCategoryAsc(posts);
        else sortPostsByCreatedDesc(posts); // 기본값
    }
}