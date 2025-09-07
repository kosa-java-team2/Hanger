package org.example.util;

import org.example.model.Post;

import java.util.List;

/**
 * ComparatorFactory (초보자 친화 버전)
 * -------------------
 * 기존: 옵션값에 따라 Comparator<Post>를 반환
 * 변경: 옵션값에 따라 리스트를 직접 정렬 (SortUtil 내부 for문 기반)
 *
 * 사용 예:
 *   List<Post> list = ...;
 *   ComparatorFactory.sort(list, 3); // 최신순 정렬
 *
 * 정렬 기준:
 *   1: 가격 오름차순
 *   2: 가격 내림차순
 *   3: 생성일시 내림차순(최신순)
 *   4: 카테고리 사전순 (동일 카테고리면 postId 오름차순)
 *   그 외: postId 오름차순
 */
public final class ComparatorFactory {

    // 인스턴스화 방지
    private ComparatorFactory() {
        throw new AssertionError("No org.example.util.ComparatorFactory instances for you!");
    }

    /**
     * 옵션값에 따라 리스트를 직접 정렬한다.
     *
     * @param posts      정렬 대상 리스트
     * @param sortOption 정렬 옵션
     */
    public static void sort(List<Post> posts, int sortOption) {
        switch (sortOption) {
            case 1:
                // 가격 오름차순
                SortUtil.sortPostsByPriceAsc(posts);
                break;
            case 2:
                // 가격 내림차순
                SortUtil.sortPostsByPriceDesc(posts);
                break;
            case 3:
                // 최신순 (생성일 내림차순)
                SortUtil.sortPostsByCreatedDesc(posts);
                break;
            case 4:
                // 카테고리 오름차순
                SortUtil.sortPostsByCategoryAsc(posts);
                break;
            default:
                // 기본값: postId 오름차순
                SortUtil.sortPostsById(posts);
                break;
        }
    }
}