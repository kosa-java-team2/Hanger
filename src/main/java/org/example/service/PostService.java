package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.ConditionLevel;
import org.example.model.Post;
import org.example.model.PostStatus;
import org.example.model.User;
import org.example.util.ComparatorFactory;
import org.example.util.InputUtil;
import org.example.util.PriceUtil;

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
        System.out.print("검색어(빈칸=전체): ");
        String kw = InputUtil.readLine();
        final String keyword = (kw == null) ? "" : kw.trim();

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

        // 기본 정렬: 최신순
        int sortOpt = 3;
        Comparator<Post> cmp = ComparatorFactory.of(sortOpt);
        base.sort(cmp);

        final int pageSize = 10;
        int currentPage = 1;

        while (true) {
            int total = base.size();
            int totalPages = (total + pageSize - 1) / pageSize;
            if (totalPages == 0) totalPages = 1;
            if (currentPage < 1) currentPage = 1;
            if (currentPage > totalPages) currentPage = totalPages;

            int fromIndex = (currentPage - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            System.out.println("======================");
            System.out.println("총 " + total + "건 | 페이지 " + currentPage + "/" + totalPages + " | 정렬: " + sortLabel(sortOpt));
            for (int i = fromIndex; i < toIndex; i++) {
                Post p = base.get(i);
                User seller = store.users().get(p.getSellerId());
                String sellerNick = seller != null ? seller.getNickname() : p.getSellerId();
                String rank = seller != null ? getUserRank(seller) : "";
                System.out.println(String.format("[%d] %s | %s | %s원 | %s | %s%s | %s",
                        p.getPostId(),
                        p.getTitle(),
                        p.getCategory(),
                        PriceUtil.format(p.getPrice()),
                        p.getStatus(),
                        sellerNick,
                        rank.isEmpty() ? "" : " (" + rank + ")",
                        p.getCreatedAt()));
            }
            System.out.println("----------------------");
            System.out.println("명령: n=다음, p=이전, s=정렬변경, g=페이지이동, v=상세조회, r=거래요청, 0=뒤로");
            String cmd = InputUtil.readLine();
            if (cmd == null) cmd = "";
            cmd = cmd.trim().toLowerCase();

            if (cmd.equals("0")) {
                return;
            } else if (cmd.equals("n")) {
                if (currentPage < totalPages) currentPage++;
                else System.out.println("마지막 페이지입니다.");
            } else if (cmd.equals("p")) {
                if (currentPage > 1) currentPage--;
                else System.out.println("첫 페이지입니다.");
            } else if (cmd.equals("s")) {
                System.out.println("정렬 방식을 선택하세요: 1.가격낮은순 2.가격높은순 3.최신순 4.카테고리");
                int opt = InputUtil.readIntInRange("선택: ", 1, 4);
                sortOpt = opt;
                cmp = ComparatorFactory.of(sortOpt);
                base.sort(cmp);
                currentPage = 1;
            } else if (cmd.equals("g")) {
                int move = InputUtil.readIntInRange("이동할 페이지(1-" + totalPages + "): ", 1, totalPages);
                currentPage = move;
            } else if (cmd.equals("v")) {
                int pid = InputUtil.readInt("상세조회할 게시글 번호(0=취소): ");
                if (pid == 0) continue;
                Post sel = store.posts().get(pid);
                if (sel == null || sel.isDeleted()) {
                    System.out.println("해당 게시글이 존재하지 않습니다.");
                } else {
                    printDetail(sel);
                }
            } else if (cmd.equals("r")) {
                int pid = InputUtil.readInt("거래요청할 게시글 번호(0=취소): ");
                if (pid == 0) continue;
                Post sel = store.posts().get(pid);
                if (sel == null || sel.isDeleted()) { System.out.println("해당 게시글이 존재하지 않습니다."); continue; }
                if (me == null) { System.out.println("로그인이 필요합니다."); continue; }
                new TradeService(store).requestTrade(me, sel);
            } else {
                System.out.println("알 수 없는 명령입니다.");
            }
        }
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
        System.out.println("7. 거래상태 변경(판매중/거래중/완료)");
        int opt = InputUtil.readIntInRange("선택: ", 1, 7);

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
            case 7:
                System.out.println("상태 선택: 1.판매중 2.거래중 3.거래완료");
                int s = InputUtil.readIntInRange("선택: ", 1, 3);
                if (s == 1) p.setStatus(PostStatus.ON_SALE);
                else if (s == 2) p.setStatus(PostStatus.IN_PROGRESS);
                else p.setStatus(PostStatus.COMPLETED);
                break;
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
        System.out.println("생성일: " + p.getCreatedAt());
        System.out.println("수정일: " + p.getUpdatedAt());
        User seller = store.users().get(p.getSellerId());
        String sellerNick = seller != null ? seller.getNickname() : p.getSellerId();
        String rank = seller != null ? getUserRank(seller) : "";
        System.out.println("판매자: " + sellerNick + (rank.isEmpty() ? "" : " (" + rank + ")"));
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

    // 등급 산정: 작성 게시물 수 기준
    public String getUserRank(User user) {
        long count = store.posts().values().stream()
                .filter(p -> !p.isDeleted() && user.getId().equals(p.getSellerId()))
                .count();
        if (count >= 30) return "PLATINUM";
        if (count >= 15) return "GOLD";
        if (count >= 5) return "SILVER";
        return "BRONZE";
    }

    private String sortLabel(int opt) {
        switch (opt) {
            case 1: return "가격낮은순";
            case 2: return "가격높은순";
            case 3: return "최신순";
            case 4: return "카테고리";
            default: return "기본";
        }
    }
}
