package org.example.util;

import org.example.model.Post;

import java.util.Comparator;

public class ComparatorFactory {
    // 1:가격낮은순  2:가격높은순  3:최신순(내림차) 4:카테고리
    public static Comparator<Post> of(int option) {
        switch (option) {
            case 1: return Comparator.comparingInt(Post::getPrice);
            case 2: return Comparator.comparingInt(Post::getPrice).reversed();
            case 3: return Comparator.comparing(Post::getCreatedAt).reversed();
            case 4: return Comparator.comparing(Post::getCategory).thenComparing(Post::getPostId);
            default: return Comparator.comparing(Post::getPostId);
        }
    }
}
