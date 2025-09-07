package org.example.util;

import org.example.model.Post;

import java.util.Comparator;

public final class ComparatorFactory {  // 유틸 클래스이므로 final 권장
    // 🔒 private 생성자: 인스턴스화 방지
    private ComparatorFactory() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // 1:가격낮은순  2:가격높은순  3:최신순(내림차) 4:카테고리
    public static Comparator<Post> of(int option) {
        return switch (option) {
            case 1 -> Comparator.comparingInt(Post::getPrice);
            case 2 -> Comparator.comparingInt(Post::getPrice).reversed();
            case 3 -> Comparator.comparing(Post::getCreatedAt).reversed();
            case 4 -> Comparator.comparing(Post::getCategory).thenComparing(Post::getPostId);
            default -> Comparator.comparing(Post::getPostId);
        };
    }
}