package org.example.service;

import org.example.model.Post;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.datastore.DataStore;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminService {
    private final DataStore store;

    public AdminService(DataStore store) {
        this.store = store;
    }

    // 사용자 관리 (목록/삭제)
    public void manageUsers() {
        List<User> list = store.users().values().stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
        System.out.println("====== 사용자 목록 ======");
        for (User u : list) {
            System.out.println(String.format("아이디: %s | 닉네임: %s | 나이: %d | 성별: %s | 권한: %s | 생성일: %s | 신뢰도: G%d/B%d",
                    u.getId(), u.getNickname(), u.getAge(), u.getGender(), u.getRole(),
                    u.getCreatedAt(), u.getTrustGood(), u.getTrustBad()));
        }
        System.out.println("========================");
        String id = InputUtil.readNonEmptyLine("삭제할 사용자 ID(0=취소): ");
        if ("0".equals(id)) return;

        User u = store.users().get(id);
        if (u == null) {
            System.out.println("해당 사용자가 없습니다.");
            return;
        }
        if (u.getRole() == Role.ADMIN) {
            System.out.println("관리자 계정은 삭제할 수 없습니다.");
            return;
        }
        // 사용자 글도 함께 삭제 마킹
        store.posts().values().stream()
                .filter(p -> !p.isDeleted() && p.getSellerId().equals(id))
                .forEach(Post::markDeleted);
        store.users().remove(id);
        store.rrnSet().remove(u.getRrn());
        store.saveAll();
        System.out.println("삭제 완료");
    }

    // 게시글 관리 (목록/삭제)
    public void managePosts() {
        List<Post> list = store.posts().values().stream()
                .filter(p -> !p.isDeleted())
                .sorted(Comparator.comparing(Post::getPostId))
                .collect(Collectors.toList());
        System.out.println("====== 게시글 목록 ======");
        list.forEach(System.out::println);
        System.out.println("========================");
        int pid = InputUtil.readInt("삭제할 게시글 번호(0=취소): ");
        if (pid == 0) return;

        Post p = store.posts().get(pid);
        if (p == null || p.isDeleted()) {
            System.out.println("해당 게시글이 없습니다.");
            return;
        }
        p.markDeleted();
        store.saveAll();
        System.out.println("삭제 완료");
    }
}
