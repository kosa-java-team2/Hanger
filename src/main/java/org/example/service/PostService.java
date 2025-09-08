package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.ConditionLevel;
import org.example.model.Post;
import org.example.model.PostStatus;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PriceUtil;
import org.example.util.SortUtil;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class PostService {
    private final DataStore store;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 금칙어 목록
    private static final Set<String> BANNED = new HashSet<>(Arrays.asList(
            "금지어", "비속어", "욕설"
    ));

    // ✅ 카테고리 필터 선택 값 저장
    private String selectedCategory = null;

    public PostService(DataStore store) {
        this.store = store;
    }

    // ===================== 등록 =====================
    public void createPost(User user) {
        System.out.println("====== 게시물 등록 ====== ");
        String title = InputUtil.readNonEmptyLine("상품명: ");
        if (containsBanned(title)) {
            System.out.println("금칙어가 포함되어 등록할 수 없습니다.");
            return;
        }

        Set<String> allowedCategories = new HashSet<>(Arrays.asList("상의", "하의", "모자", "신발"));
        String category = InputUtil.readNonEmptyLine("카테고리(상의/하의/모자/신발): ").trim();
        if (!allowedCategories.contains(category)) {
            System.out.println("카테고리는 상의/하의/모자/신발 중 하나여야 합니다.");
            return;
        }

        Integer price = InputUtil.readPriceAsInt("가격(숫자 또는 1,000 형식): ");
        if (price == null || price < 0) {
            System.out.println("가격 형식이 올바르지 않습니다.");
            return;
        }

        String conditionInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ").trim();
        ConditionLevel conditionLevel = mapConditionFromLabel(conditionInput);
        if (conditionLevel == null) {
            System.out.println("상품 상태는 상/중/하 중 하나여야 합니다.");
            return;
        }

        String description = InputUtil.readNonEmptyLine("상품 상세설명: ");
        if (containsBanned(description)) {
            System.out.println("금칙어가 포함되어 등록할 수 없습니다.");
            return;
        }

        String location = InputUtil.readNonEmptyLine("원하는 거래위치: ");

        int postId = store.nextPostId();
        Post newPost = new Post.Builder(postId, user.getId())
                .title(title)
                .category(category)
                .price(price)
                .location(location)
                .condition(conditionLevel)
                .description(description)
                .build();

        store.posts().put(postId, newPost);
        store.saveToDisk();
        System.out.println("======================");
        System.out.println("게시물이 등록되었습니다! (번호: " + postId + ")");
    }

    // ===================== 검색/조회 =====================
    public void searchAndView(User currentUser) {
        System.out.println("====== 상품 검색 ======");
        System.out.print("검색어(빈칸=전체): ");
        String inputKeyword = InputUtil.readLine();
        final String keyword = (inputKeyword == null) ? "" : inputKeyword.trim();

        List<Post> filtered = new ArrayList<>(filterPostsForSearch(currentUser, keyword));
        if (filtered.isEmpty()) {
            System.out.println("검색 결과 없음");
            return;
        }

        int sortOption = 3; // 기본: 최신순
        SortUtil.applyPostSort(filtered, sortOption);

        int currentPage = 1;

        while (true) {
            Page page = paginate(filtered, currentPage);
            renderPageHeader(page.total, page.currentPage, page.totalPages, sortOption);
            renderPosts(page.items);

            Command command = readCommand();
            if (command == Command.EXIT) return;

            switch (command) {
                case NEXT:
                    currentPage = nextPage(currentPage, page.totalPages);
                    break;
                case PREV:
                    currentPage = prevPage(currentPage);
                    break;
                case SORT:
                    sortOption = readSortOption();
                    // ✅ 정렬/카테고리 변경 시, 리스트 다시 필터링
                    filtered = new ArrayList<>(filterPostsForSearch(currentUser, keyword));
                    SortUtil.applyPostSort(filtered, sortOption);
                    currentPage = 1;
                    break;
                case GOTO:
                    currentPage = readGoto(page.totalPages);
                    break;
                case VIEW:
                    handleViewDetail();
                    break;
                case REQUEST:
                    handleRequest(currentUser);
                    break;
                default:
                    System.out.println("알 수 없는 명령입니다.");
                    break;
            }
        }
    }

    private enum Command {NEXT, PREV, SORT, GOTO, VIEW, REQUEST, EXIT, UNKNOWN}

    private static final class Page {
        final List<Post> items;
        final int currentPage;
        final int totalPages;
        final int total;

        Page(List<Post> items, int currentPage, int totalPages, int total) {
            this.items = items;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.total = total;
        }
    }

    // ✅ 카테고리 필터 반영
    private List<Post> filterPostsForSearch(User currentUser, String keyword) {
        final String normalizedKeyword = normalizeToLower(keyword);
        List<Post> result = new ArrayList<>();

        for (Post post : store.posts().values()) {
            if (shouldSkipPostForSearch(post, currentUser, normalizedKeyword)) continue;

            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                if (!post.getCategory().equals(selectedCategory)) continue;
            }

            result.add(post);
        }
        return result;
    }

    private boolean shouldSkipPostForSearch(Post post, User currentUser, String keywordLower) {
        if (post.isDeleted()) return true;
        if (post.getStatus() == PostStatus.COMPLETED) return true;
        if (isPostOwnedByUser(post, currentUser)) return true;
        if (!keywordLower.isEmpty() && !postMatchesKeyword(post, keywordLower)) return true;
        return false;
    }

    private boolean isPostOwnedByUser(Post post, User user) {
        return user != null && post.getSellerId() != null && post.getSellerId().equals(user.getId());
    }

    private boolean postMatchesKeyword(Post post, String keywordLower) {
        String titleLower = toLowerOrEmpty(post.getTitle());
        String descriptionLower = toLowerOrEmpty(post.getDescription());
        return titleLower.contains(keywordLower) || descriptionLower.contains(keywordLower);
    }

    private String toLowerOrEmpty(String text) {
        return (text == null) ? "" : text.toLowerCase();
    }

    private String normalizeToLower(String text) {
        return (text == null) ? "" : text.toLowerCase();
    }

    private Page paginate(List<Post> posts, int currentPage) {
        int total = posts.size();
        int totalPages = Math.max(1, (total + 10 - 1) / 10);

        int safePage = Math.max(1, Math.min(currentPage, totalPages));
        int fromIndex = (safePage - 1) * 10;
        int toIndex = Math.min(fromIndex + 10, total);
        return new Page(posts.subList(fromIndex, toIndex), safePage, totalPages, total);
    }

    private void renderPageHeader(int total, int page, int totalPages, int sortOpt) {
        System.out.println("======================");
        System.out.println("총 " + total + "건 | 페이지 " + page + "/" + totalPages + " | 정렬: " + sortLabel(sortOpt));
    }

    private void renderPosts(List<Post> posts) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Post post : posts) {
            User seller = store.users().get(post.getSellerId());
            String sellerNick = seller != null ? seller.getNickname() : post.getSellerId();
            String rank = seller != null ? getUserRank(seller) : "";
            String createdAt = post.getCreatedAt().format(formatter);

            System.out.printf("[%d] %s | %s | %s원 | %s | %s%s | %s%n",
                    post.getPostId(),
                    post.getTitle(),
                    post.getCategory(),
                    PriceUtil.format(post.getPrice()),
                    post.getStatus().getLabel(),
                    sellerNick,
                    rank.isEmpty() ? "" : " (" + rank + ")",
                    createdAt);
        }
        System.out.println("===== 명령어 안내 =====");
        System.out.println(" n : 다음 페이지 (next)");
        System.out.println(" p : 이전 페이지 (previous)");
        System.out.println(" s : 정렬 변경 (sort)");
        System.out.println(" g : 페이지 이동 (goto)");
        System.out.println(" v : 상세 조회 (view)");
        System.out.println(" r : 거래 요청 (request)");
        System.out.println(" 0 : 뒤로 가기 (exit)");
        System.out.println("======================");
    }

    private Command readCommand() {
        String raw = InputUtil.readLine();
        if (raw == null) raw = "";
        raw = raw.trim().toLowerCase();

        switch (raw) {
            case "0": return Command.EXIT;
            case "n": return Command.NEXT;
            case "p": return Command.PREV;
            case "s": return Command.SORT;
            case "g": return Command.GOTO;
            case "v": return Command.VIEW;
            case "r": return Command.REQUEST;
            default:  return Command.UNKNOWN;
        }
    }

    private int nextPage(int current, int totalPages) {
        if (current < totalPages) return current + 1;
        System.out.println("마지막 페이지입니다.");
        return current;
    }

    private int prevPage(int current) {
        if (current > 1) return current - 1;
        System.out.println("첫 페이지입니다.");
        return current;
    }

    private int readSortOption() {
        System.out.println("정렬 방식을 선택하세요: 1.가격낮은순 2.가격높은순 3.최신순 4.카테고리");
        int option = InputUtil.readIntInRange("선택: ", 1, 4);

        if (option == 4) { // ✅ 카테고리 선택 → selectedCategory 저장
            selectedCategory = readCategoryOption();
            System.out.println("선택한 카테고리: " + selectedCategory);
        } else {
            selectedCategory = null; // 다른 정렬 시 카테고리 필터 해제
        }
        return option;
    }

    private String readCategoryOption() {
        System.out.println("카테고리를 선택하세요: 1.상의 2.하의 3.모자 4.신발");
        int categoryOption = InputUtil.readIntInRange("선택: ", 1, 4);

        switch (categoryOption) {
            case 1: return "상의";
            case 2: return "하의";
            case 3: return "모자";
            case 4: return "신발";
            default: return "";
        }
    }

    private int readGoto(int totalPages) {
        return InputUtil.readIntInRange("이동할 페이지(1-" + totalPages + "): ", 1, totalPages);
    }

    // ===================== 상세조회/거래요청 =====================
    private void handleViewDetail() {
        int selectedPostId = InputUtil.readInt("상세조회할 게시글 번호(0=취소): ");
        if (selectedPostId == 0) return;
        Post selectedPost = store.posts().get(selectedPostId);
        if (selectedPost == null || selectedPost.isDeleted()) {
            System.out.println("해당 게시글이 존재하지 않습니다.");
            return;
        }
        printDetail(selectedPost);
    }

    private void handleRequest(User currentUser) {
        int selectedPostId = InputUtil.readInt("거래요청할 게시글 번호(0=취소): ");
        if (selectedPostId == 0) return;

        Post selectedPost = store.posts().get(selectedPostId);
        if (selectedPost == null || selectedPost.isDeleted()) {
            System.out.println("해당 게시글이 존재하지 않습니다.");
            return;
        }
        if (currentUser == null) {
            System.out.println("로그인이 필요합니다.");
            return;
        }
        new TradeService(store).requestTrade(currentUser, selectedPost);
    }

    // ===================== 내 게시글 관리 =====================
    public void manageMyPosts(User currentUser) {
        List<Post> myPosts = new ArrayList<>();
        for (Post post : store.posts().values()) {
            if (!post.isDeleted() && post.getSellerId().equals(currentUser.getId())) {
                myPosts.add(post);
            }
        }
        SortUtil.sortPostsById(myPosts);
        if (myPosts.isEmpty()) {
            System.out.println("내 게시글이 없습니다.");
            return;
        }
        System.out.println("====== 내 게시글 ======");
        for (Post post : myPosts) {
            System.out.println(post);
        }

        int selectedPostId = InputUtil.readInt("수정/삭제할 게시글 번호(0=뒤로): ");
        if (selectedPostId == 0) return;
        Post targetPost = store.posts().get(selectedPostId);
        if (targetPost == null || targetPost.isDeleted() || !targetPost.getSellerId().equals(currentUser.getId())) {
            System.out.println("본인의 게시글만 관리할 수 있습니다.");
            return;
        }

        System.out.println("======================");
        System.out.println("1. 수정  2. 삭제  (기타=취소)");
        int menuSelection = InputUtil.readInt("선택: ");
        if (menuSelection == 1) {
            editPost(currentUser, targetPost);
        } else if (menuSelection == 2) {
            if (targetPost.getStatus() == PostStatus.COMPLETED) {
                System.out.println("이 게시물은 이미 거래 완료되어 삭제할 수 없습니다.");
                return;
            }
            targetPost.markAsDeleted();
            store.saveToDisk();
            System.out.println("삭제 완료");
        }
    }

    private void editPost(User currentUser, Post post) {
        if (!post.getSellerId().equals(currentUser.getId())) {
            System.out.println("판매자만 게시물을 수정할 수 있습니다.");
            return;
        }

        System.out.println("수정할 항목 선택");
        System.out.println("1. 제목(상품명)");
        System.out.println("2. 카테고리");
        System.out.println("3. 가격");
        System.out.println("4. 상품 상태");
        System.out.println("5. 상품 상세설명");
        System.out.println("6. 원하는 거래위치");
        System.out.println("7. 거래상태 변경(판매중/거래중/완료)");
        int menuSelection = InputUtil.readIntInRange("선택: ", 1, 7);

        switch (menuSelection) {
            case 1:
                String newTitle = InputUtil.readNonEmptyLine("새 제목: ");
                if (containsBanned(newTitle)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setTitle(newTitle);
                break;
            case 2:
                post.setCategory(InputUtil.readNonEmptyLine("새 카테고리: "));
                break;
            case 3:
                Integer newPrice = InputUtil.readPriceAsInt("새 가격(숫자 or 1,000): ");
                if (newPrice == null || newPrice < 0) {
                    System.out.println("가격 오류");
                    return;
                }
                post.setPrice(newPrice);
                break;
            case 4:
                String conditionInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ");
                ConditionLevel conditionLevel = mapConditionFromLabel(conditionInput.trim());
                if (conditionLevel == null) {
                    System.out.println("상/중/하 중 하나여야 함");
                    return;
                }
                post.setCondition(conditionLevel);
                break;
            case 5:
                String newDescription = InputUtil.readNonEmptyLine("새 설명: ");
                if (containsBanned(newDescription)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setDescription(newDescription);
                break;
            case 6:
                post.setLocation(InputUtil.readNonEmptyLine("새 거래위치: "));
                break;
            case 7:
                System.out.println("상태 선택: 1.판매중 2.거래중 3.거래완료");
                int statusOption = InputUtil.readIntInRange("선택: ", 1, 3);
                if (statusOption == 1) {
                    post.setStatus(PostStatus.ON_SALE);
                } else if (statusOption == 2) {
                    post.setStatus(PostStatus.IN_PROGRESS);
                } else {
                    post.setStatus(PostStatus.COMPLETED);
                }
                break;
            default:
                System.out.println("취소");
                return;
        }
        store.saveToDisk();
        System.out.println("수정 완료");
    }

    private void printDetail(Post post) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("====== 상품 조회 ======");
        System.out.println("상품번호: " + post.getPostId());
        System.out.println("제목: " + post.getTitle());
        System.out.println("카테고리: " + post.getCategory());
        System.out.println("가격: " + PriceUtil.format(post.getPrice()));
        System.out.println("상품 상태: " + post.getStatus().getLabel());
        System.out.println("컨디션: " + post.getCondition().getLabel());
        System.out.println("상세설명: " + post.getDescription());
        System.out.println("거래위치: " + post.getLocation());
        System.out.println("생성일: " + post.getCreatedAt().format(formatter));
        System.out.println("수정일: " + post.getUpdatedAt().format(formatter));
        User seller = store.users().get(post.getSellerId());
        String sellerNick = seller != null ? seller.getNickname() : post.getSellerId();
        String rank = seller != null ? getUserRank(seller) : "";
        System.out.println("판매자: " + sellerNick + (rank.isEmpty() ? "" : " (" + rank + ")"));
        System.out.println("======================");
    }

    // ===================== 유틸 =====================

    /**
     * 문자열에 금칙어가 포함되는지 단순 검사(부분 일치, 대소문자 무시)
     */
    private boolean containsBanned(String text) {
        String lower = text.toLowerCase();
        for (String bannedWord : BANNED) {
            if (lower.contains(bannedWord.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * "상/중/하" → ConditionLevel 매핑
     */
    private ConditionLevel mapConditionFromLabel(String label) {
        switch (label) {
            case "상":
                return ConditionLevel.HIGH;
            case "중":
                return ConditionLevel.MEDIUM;
            case "하":
                return ConditionLevel.LOW;
            default:
                return null;
        }
    }

    /**
     * 판매자 등급 산정(간단 규칙): 작성 게시물 수 기준.
     * - 30개 이상: PLATINUM
     * - 15개 이상: GOLD
     * - 5개 이상 : SILVER
     * - 그 외    : BRONZE
     */
    public String getUserRank(User user) {
        int count = 0;
        for (Post post : store.posts().values()) {
            if (!post.isDeleted() && user.getId().equals(post.getSellerId())) {
                count++;
            }
        }
        if (count >= 30) return "PLATINUM";
        if (count >= 15) return "GOLD";
        if (count >= 5) return "SILVER";
        return "BRONZE";
    }

    /**
     * 정렬 옵션 라벨 문자열
     */
    private String sortLabel(int opt) {
        switch (opt) {
            case 1:
                return "가격낮은순";
            case 2:
                return "가격높은순";
            case 3:
                return "최신순";
            case 4:
                return "카테고리";
            default:
                return "기본";
        }
    }
}