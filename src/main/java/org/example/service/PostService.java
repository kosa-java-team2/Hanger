package org.example.service;

import java.util.*;
import java.util.stream.Collectors;

public class PostService {
    private final DataStore store;

    // 금칙어 예시(원하는 단어로 교체하세요)
    private static final Set<String> BANNED = new HashSet<>(Arrays.asList(
        "금지어", "비속어", "욕설" // 예시. 프로젝트에 맞게 확장하세요.
    ));

    public PostService(DataStore store) {
        this.store = store;
    }

    // 등록
    public void createPost(User user) {
        System.out.println("====== 게시물 등록 ====== ");
        String title = InputUtil.readNonEmptyLine("상품명: ");
        if (containsBanned(title)) { System.out.println("금칙어가 포함되어 등록할 수 없습니다."); return; }

        String category = InputUtil.readNonEmptyLine("카테고리(상의/하의/모자/신발 등): ");

        Integer price = InputUtil.readPriceAsInt("가격(숫자 또는 1,000 형식): ");
        if (price == null || price < 0) { System.out.println("가격 형식이 올바르지 않습니다."); return; }

        String condInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ").trim();
        ConditionLevel cond = mapCondition(condInput);
        if (cond == null) { System.out.println("상품 상태는 상/중/하 중 하나여야 합니다."); return; }

        String desc = InputUtil.readNonEmptyLine("상품 상세설명: ");
        if (containsBanned(desc)) { System.out.println("금칙어가 포함되어 등록할 수 없습니다."); return; }

        String location = InputUtil.readNonEmptyLine("원하는 거래위치: ");

        int postId = store.nextPostId();
        Post p = new Post(postId, title, category, price, user.getId(), location, cond, desc);
        store.posts().put(postId, p);
        store.saveAll();
        System.out.println("======================");
        System.out.println("게시물이 등록되었습니다! (번호: " + postId + ")");
    }

    // 검색/조회 (간단한 검색 + 정렬)
    public void searchAndView(User me) {
        System.out.println("====== 상품 검색 ======");
        String keyword = InputUtil.readNonEmptyLine("검색어(미입력 시 전체): ").trim();

        List<Post> base = store.posts().values().stream()
                .filter(p -> !p.isDeleted() && p.getStatus() != PostStatus.COMPLETED)
                .filter(p -> keyword.isEmpty()
                        || p.getTitle().toLowerCase().contains(keyword.toLowerCase())
                        || p.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> me == null || !p.getSellerId().equals(me.getId())) // 내 글 제외
                .collect(Collectors.toList());

        if (base.isEmpty()) {
            System.out.println("검색 결과 없음");
            return;
        }

        // 정렬 선택
        System.out.println("정렬 방식을 선택하세요: 1.가격낮은순 2.가격높은순 3.최신순 4.카테고리");
        int opt = InputUtil.readIntInRange("선택: ", 1, 4);
        Comparator<Post> cmp = ComparatorFactory.of(opt);
        base.sort(cmp);

        // 목록 출력
        base.forEach(p -> System.out.println(p));

        // 상세 조회
        int pid = InputUtil.readInt("상세조회할 게시글 번호(0=메뉴로): ");
        if (pid == 0) return;

        Post sel = store.posts().get(pid);
        if (sel == null || sel.isDeleted()) {
            System.out.println("해당 게시글이 존재하지 않습니다.");
            return;
        }
        printDetail(sel);
    }

    // 내 게시글 관리(수정/삭제)
    public void manageMyPosts(User me) {
        List<Post> mine = store.posts().values().stream()
                .filter(p -> !p.isDeleted() && p.getSellerId().equals(me.getId()))
                .sorted(Comparator.comparing(Post::getPostId))
                .collect(Collectors.toList());
        if (mine.isEmpty()) {
            System.out.println("내 게시글이 없습니다.");
            return;
        }
        System.out.println("====== 내 게시글 ======");
        mine.forEach(p -> System.out.println(p));

        int pid = InputUtil.readInt("수정/삭제할 게시글 번호(0=뒤로): ");
        if (pid == 0) return;
        Post target = store.posts().get(pid);
        if (target == null || target.isDeleted() || !target.getSellerId().equals(me.getId())) {
            System.out.println("본인의 게시글만 관리할 수 있습니다.");
            return;
        }

        System.out.println("======================");
        System.out.println("1. 수정  2. 삭제  (기타=취소)");
        int sel = InputUtil.readInt("선택: ");
        if (sel == 1) {
            editPost(target);
        } else if (sel == 2) {
            if (target.getStatus() == PostStatus.COMPLETED) {
                System.out.println("이 게시물은 이미 거래 완료되어 삭제할 수 없습니다.");
                return;
            }
            target.markDeleted();
            store.saveAll();
            System.out.println("삭제 완료");
        }
    }

    // 수정
    private void editPost(Post p) {
        System.out.println("수정할 항목 선택");
        System.out.println("1. 제목(상품명)");
        System.out.println("2. 카테고리");
        System.out.println("3. 가격");
        System.out.println("4. 상품 상태");
        System.out.println("5. 상품 상세설명");
        System.out.println("6. 원하는 거래위치");
        int opt = InputUtil.readIntInRange("선택: ", 1, 6);

        switch (opt) {
            case 1:
                String t = InputUtil.readNonEmptyLine("새 제목: ");
                if (containsBanned(t)) { System.out.println("금칙어 포함"); return; }
                p.setTitle(t); break;
            case 2:
                p.setCategory(InputUtil.readNonEmptyLine("새 카테고리: ")); break;
            case 3:
                Integer price = InputUtil.readPriceAsInt("새 가격(숫자 or 1,000): ");
                if (price == null || price < 0) { System.out.println("가격 오류"); return; }
                p.setPrice(price); break;
            case 4:
                String condInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ");
                ConditionLevel cond = mapCondition(condInput.trim());
                if (cond == null) { System.out.println("상/중/하 중 하나여야 함"); return; }
                p.setCondition(cond); break;
            case 5:
                String desc = InputUtil.readNonEmptyLine("새 설명: ");
                if (containsBanned(desc)) { System.out.println("금칙어 포함"); return; }
                p.setDescription(desc); break;
            case 6:
                p.setLocation(InputUtil.readNonEmptyLine("새 거래위치: ")); break;
        }
        store.saveAll();
        System.out.println("수정 완료");
    }

    private void printDetail(Post p) {
        System.out.println("====== 상품 조회 ======");
        System.out.println("상품번호: " + p.getPostId());
        System.out.println("제목: " + p.getTitle());
        System.out.println("카테고리: " + p.getCategory());
        System.out.println("가격: " + PriceUtil.format(p.getPrice()));
        System.out.println("상품 상태: " + p.getStatus());
        System.out.println("컨디션: " + p.getCondition());
        System.out.println("상세설명: " + p.getDescription());
        System.out.println("거래위치: " + p.getLocation());
        System.out.println("판매자: " + p.getSellerId());
        System.out.println("======================");
    }

    private boolean containsBanned(String s) {
        String lower = s.toLowerCase();
        for (String bad : BANNED) {
            if (lower.contains(bad.toLowerCase())) return true;
        }
        return false;
    }

    private ConditionLevel mapCondition(String s) {
        switch (s) {
            case "상": return ConditionLevel.HIGH;
            case "중": return ConditionLevel.MEDIUM;
            case "하": return ConditionLevel.LOW;
            default: return null;
        }
    }
}
