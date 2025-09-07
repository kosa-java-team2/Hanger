package org.example.util;

import org.example.model.Post;

import java.util.Comparator;

/**
 * ComparatorFactory
 * -------------------
 * 게시글(Post) 목록 정렬에 사용되는 Comparator를 옵션값에 따라 제공하는 유틸리티 클래스.
 *
 * 사용 예:
 *   List<Post> list = ...;
 *   list.sort(ComparatorFactory.of(3)); // 최신순 정렬
 *
 * 설계 노트:
 * - 인스턴스화가 불필요하므로 클래스는 final, 생성자는 private 으로 막는다.
 * - 옵션별 Comparator를 switch 로 명확하게 분기한다.
 * - 정렬 기준:
 *     1: 가격 오름차순
 *     2: 가격 내림차순
 *     3: 생성일시 내림차순(최신순)
 *     4: 카테고리 사전순 → 동일 카테고리 내 postId 오름차순(안정적 출력용 tie-breaker)
 *   default: postId 오름차순(기본/안전한 정렬)
 *
 * 주의:
 * - null 값 가능성이 있는 필드를 비교할 경우 Comparator.nullsFirst/Last 로 보강 가능.
 * - 최신순(옵션 3)의 경우 생성일이 같은 항목의 표시 순서를 안정화하려면
 *   thenComparing(Post::getPostId) 등을 추가하는 방법도 있다(현재는 변경하지 않음).
 */
public final class ComparatorFactory {  // 유틸 클래스이므로 final 권장
    // 🔒 인스턴스화 방지: 외부에서 new 하지 못하게 막는다.
    private ComparatorFactory() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * 옵션값에 해당하는 Comparator<Post> 를 반환한다.
     *
     * @param option 정렬 옵션
     *               1: 가격낮은순
     *               2: 가격높은순
     *               3: 최신순(생성일시 내림차순)
     *               4: 카테고리(사전순) → 동일 카테고리 내 postId 오름차순
     *             default: postId 오름차순(기본 정렬)
     * @return 옵션에 대응하는 Comparator<Post>
     */
    public static Comparator<Post> of(int option) {
        return switch (option) {
            // 1) 가격 오름차순 (저가 → 고가)
            case 1 -> Comparator.comparingInt(Post::getPrice);

            // 2) 가격 내림차순 (고가 → 저가)
            case 2 -> Comparator.comparingInt(Post::getPrice).reversed();

            // 3) 최신순: 생성일시 내림차순 (최근 생성된 게시물이 먼저)
            //    필요 시 .thenComparing(Post::getPostId)로 동순위 안정화 가능
            case 3 -> Comparator.comparing(Post::getCreatedAt).reversed();

            // 4) 카테고리 사전순 → 동일 카테고리 내 postId 오름차순으로 안정화
            case 4 -> Comparator.comparing(Post::getCategory)
                    .thenComparing(Post::getPostId);

            // 기본: postId 오름차순(일관된 기본 출력 보장)
            default -> Comparator.comparing(Post::getPostId);
        };
    }
}