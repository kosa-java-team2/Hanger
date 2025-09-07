package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.ConditionLevel;
import org.example.model.Post;
import org.example.model.PostStatus;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PriceUtil;
import org.example.util.SortUtil;

import java.util.*;

/**
 * PostService
 * -------------------
 * 게시글(Post) 등록/검색/조회/수정/삭제 등 게시글 전반 비즈니스 로직을 담당하는 서비스 레이어.
 * <p>
 * 주요 기능:
 * - 게시물 등록(createPost)
 * - 검색 & 페이징 & 정렬 & 상세보기 & 거래요청(searchAndView)
 * - 내 게시글 관리(수정/삭제)(manageMyPosts)
 * <p>
 * 설계 노트:
 * - 영속 계층(DataStore)을 주입받아 사용한다.
 * - 콘솔 인터랙션은 InputUtil을 사용한다.
 * - 정렬 기준은 ComparatorFactory에 위임한다(옵션값 기반).
 * - 삭제는 논리 삭제(soft delete)로 처리한다(Post.isDeleted).
 * - 금칙어 필터(BANNED)로 제목/설명 입력을 가볍게 필터링한다.
 */
public class PostService {
    /**
     * 전역 데이터 저장/로드 및 컬렉션 보관소
     */
    private final DataStore store;

    // ===================== 금칙어 사전 =====================
    /**
     * 제목/설명에 등장하면 등록/수정이 차단되는 금칙어 목록.
     * 실제 운영에서는 외부 설정(파일/DB) + 정규식/토큰화 + 금칙어 타입 분류 등을 권장.
     */
    private static final Set<String> BANNED = new HashSet<>(Arrays.asList(
            "금지어", "비속어", "욕설" // 예시. 프로젝트에 맞게 확장하세요.
    ));

    public PostService(DataStore store) {
        this.store = store;
    }

    // ===================== 등록 =====================

    /**
     * 게시물 등록 플로우.
     * 1) 제목/카테고리/가격/상태/설명/거래위치 입력
     * 2) 제목/설명 금칙어 검사
     * 3) 상태(상/중/하) → ConditionLevel 매핑
     * 4) postId 시퀀스 발급 → Post.Builder로 객체 생성
     * 5) store.posts()에 저장 후 saveAll()
     * <p>
     * 주의:
     * - 가격 입력은 정수로 통일하며, 입력 유틸이 "1,000" 형식도 정수로 파싱.
     * - 음수 가격 방지.
     */
    public void createPost(User user) {
        System.out.println("====== 게시물 등록 ====== ");
        String title = InputUtil.readNonEmptyLine("상품명: ");
        if (containsBanned(title)) {
            System.out.println("금칙어가 포함되어 등록할 수 없습니다.");
            return;
        }

        String category = InputUtil.readNonEmptyLine("카테고리(상의/하의/모자/신발 등): ");

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

        // 고유 postId 발급 후 엔티티 생성
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

    // ===================== 검색/조회(리팩터링) =====================

    /**
     * 검색 & 페이징 & 정렬 & 상세조회 & 거래요청까지 하나의 루프에서 처리하는 UI 흐름.
     * <p>
     * 기본 흐름:
     * 1) 검색어 입력(빈칸=전체) → filteredPosts()로 필터링(삭제/완료 제외, 내 글 제외)
     * 2) 기본 정렬(최신순=옵션 3) 적용
     * 3) 페이지 단위(10건)로 목록 출력 → 명령어 입력
     * - n: 다음 페이지
     * - p: 이전 페이지
     * - s: 정렬 변경 (ComparatorFactory.of)
     * - g: 페이지 이동
     * - v: 상세 조회(게시글 번호 입력)
     * - r: 거래 요청(게시글 번호 입력 → TradeService.requestTrade)
     * - 0: 뒤로(종료)
     */
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

        final int pageSize = 10;
        int currentPage = 1;

        while (true) {
            Page page = paginate(filtered, currentPage, pageSize);
            renderPageHeader(page.total, page.currentPage, page.totalPages, sortOption);
            renderPosts(page.items);

            Command command = readCommand();
            if (command == Command.EXIT) return;

            switch (command) {
                case NEXT -> currentPage = nextPage(currentPage, page.totalPages);
                case PREV -> currentPage = prevPage(currentPage);
                case SORT -> {
                    sortOption = readSortOption();
                    SortUtil.applyPostSort(filtered, sortOption);
                    currentPage = 1; // 정렬 변경 시 1페이지로 이동
                }
                case GOTO -> currentPage = readGoto(page.totalPages);
                case VIEW -> handleViewDetail();
                case REQUEST -> handleRequest(currentUser);
                default -> System.out.println("알 수 없는 명령입니다.");
            }
        }
    }

    /**
     * 콘솔 명령어 열거
     */
    private enum Command {NEXT, PREV, SORT, GOTO, VIEW, REQUEST, EXIT, UNKNOWN}

    /**
     * 페이징 결과 컨테이너(현재 페이지 목록/번호/총 페이지/총 건수)
     */
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

    /**
     * 검색 필터링:
     * - 삭제되지 않았고(soft delete 미적용)
     * - 거래 완료 상태가 아니며(PostStatus != COMPLETED)
     * - 키워드가 제목/설명에 포함(대소문자 무시)
     * - 로그인 상태이면, 내 글은 리스트에서 제외
     */
    private List<Post> filterPostsForSearch(User currentUser, String keyword) {
        final String normalizedKeyword = normalizeToLower(keyword);
        List<Post> result = new ArrayList<>();

        for (Post post : store.posts().values()) {
            if (shouldSkipPostForSearch(post, currentUser, normalizedKeyword)) continue;
            result.add(post);
        }
        return result;
    }

    // === 헬퍼들 ===
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

    /**
     * 페이지 계산(고정 페이지 크기=10).
     * - 범위를 벗어난 페이지 요청은 안전하게 보정.
     * <p>
     * 주의:
     * - Math.clamp는 Java 21+에 존재합니다. 더 낮은 버전 사용 시 직접 보정 코드를 사용하세요.
     */
    private Page paginate(List<Post> posts, int currentPage, int pageSize) {
        int total = posts.size();
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);

        int safePage = currentPage;
        if (safePage < 1) safePage = 1;
        if (safePage > totalPages) safePage = totalPages;

        int fromIndex = (safePage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new Page(posts.subList(fromIndex, toIndex), safePage, totalPages, total);
    }

    /**
     * 페이지 헤더(총 건수/페이지/정렬 라벨) 출력
     */
    private void renderPageHeader(int total, int page, int totalPages, int sortOpt) {
        System.out.println("======================");
        System.out.println("총 " + total + "건 | 페이지 " + page + "/" + totalPages + " | 정렬: " + sortLabel(sortOpt));
    }

    /**
     * 게시글 목록 한 페이지 출력.
     * - 판매자 닉네임 및 등급(getUserRank) 표시
     * - 가격은 PriceUtil.format으로 쉼표 포맷
     */
    private void renderPosts(List<Post> posts) {
        for (Post post : posts) {
            User seller = store.users().get(post.getSellerId());
            String sellerNick = seller != null ? seller.getNickname() : post.getSellerId();
            String rank = seller != null ? getUserRank(seller) : "";
            System.out.printf("[%d] %s | %s | %s원 | %s | %s%s | %s%n",
                    post.getPostId(),
                    post.getTitle(),
                    post.getCategory(),
                    PriceUtil.format(post.getPrice()),
                    post.getStatus(),
                    sellerNick,
                    rank.isEmpty() ? "" : " (" + rank + ")",
                    post.getCreatedAt());
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

    /**
     * 콘솔 명령어 입력을 Command로 변환
     */
    private Command readCommand() {
        String raw = InputUtil.readLine();
        if (raw == null) raw = "";
        raw = raw.trim().toLowerCase();

        return switch (raw) {
            case "0" -> Command.EXIT;
            case "n" -> Command.NEXT;
            case "p" -> Command.PREV;
            case "s" -> Command.SORT;
            case "g" -> Command.GOTO;
            case "v" -> Command.VIEW;
            case "r" -> Command.REQUEST;
            default -> Command.UNKNOWN;
        };
    }

    /**
     * 다음 페이지로 이동(마지막 페이지면 유지)
     */
    private int nextPage(int current, int totalPages) {
        if (current < totalPages) return current + 1;
        System.out.println("마지막 페이지입니다.");
        return current;
    }

    /**
     * 이전 페이지로 이동(첫 페이지면 유지)
     */
    private int prevPage(int current) {
        if (current > 1) return current - 1;
        System.out.println("첫 페이지입니다.");
        return current;
    }

    /**
     * 정렬 옵션 입력(1~4)
     */
    private int readSortOption() {
        System.out.println("정렬 방식을 선택하세요: 1.가격낮은순 2.가격높은순 3.최신순 4.카테고리");
        return InputUtil.readIntInRange("선택: ", 1, 4);
    }

    /**
     * 페이지 이동 입력(1~totalPages)
     */
    private int readGoto(int totalPages) {
        return InputUtil.readIntInRange("이동할 페이지(1-" + totalPages + "): ", 1, totalPages);
    }

    // ===================== 상세조회/거래요청 액션 =====================

    /**
     * 게시글 번호 입력 → 상세조회 출력
     */
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

    /**
     * 게시글 번호 입력 → 거래 요청(로그인 필요)
     */
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
    // ===================== /검색/조회 =====================

    // ===================== 내 게시글 관리 =====================

    /**
     * 내 게시글 목록 → 선택 후 수정/삭제.
     * - 본인 글만 대상
     * - 삭제는 논리 삭제 (거래 완료(PostStatus.COMPLETED)면 삭제 불가)
     */
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
            editPost(targetPost);
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

    // ===================== 수정 =====================

    /**
     * 단일 게시글 수정 메뉴.
     * - 각 항목 수정 시 Post의 setter가 updatedAt을 자동 갱신(touch)한다.
     * - 제목/설명 수정 시 금칙어 검사 재적용.
     */
    private void editPost(Post post) {
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
            case 1 -> {
                String newTitle = InputUtil.readNonEmptyLine("새 제목: ");
                if (containsBanned(newTitle)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setTitle(newTitle);
            }
            case 2 -> post.setCategory(InputUtil.readNonEmptyLine("새 카테고리: "));
            case 3 -> {
                Integer newPrice = InputUtil.readPriceAsInt("새 가격(숫자 or 1,000): ");
                if (newPrice == null || newPrice < 0) {
                    System.out.println("가격 오류");
                    return;
                }
                post.setPrice(newPrice);
            }
            case 4 -> {
                String conditionInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ");
                ConditionLevel conditionLevel = mapConditionFromLabel(conditionInput.trim());
                if (conditionLevel == null) {
                    System.out.println("상/중/하 중 하나여야 함");
                    return;
                }
                post.setCondition(conditionLevel);
            }
            case 5 -> {
                String newDescription = InputUtil.readNonEmptyLine("새 설명: ");
                if (containsBanned(newDescription)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setDescription(newDescription);
            }
            case 6 -> post.setLocation(InputUtil.readNonEmptyLine("새 거래위치: "));
            case 7 -> {
                System.out.println("상태 선택: 1.판매중 2.거래중 3.거래완료");
                int statusOption = InputUtil.readIntInRange("선택: ", 1, 3);
                if (statusOption == 1) post.setStatus(PostStatus.ON_SALE);
                else if (statusOption == 2) post.setStatus(PostStatus.IN_PROGRESS);
                else post.setStatus(PostStatus.COMPLETED);
            }
            default -> {
                System.out.println("취소");
                return;
            }
        }
        store.saveToDisk();
        System.out.println("수정 완료");
    }

    // ===================== 상세 출력 =====================

    /**
     * 단일 게시글 상세 정보 출력(가격 포맷/판매자 닉네임/등급 포함)
     */
    private void printDetail(Post post) {
        System.out.println("====== 상품 조회 ======");
        System.out.println("상품번호: " + post.getPostId());
        System.out.println("제목: " + post.getTitle());
        System.out.println("카테고리: " + post.getCategory());
        System.out.println("가격: " + PriceUtil.format(post.getPrice()));
        System.out.println("상품 상태: " + post.getStatus());
        System.out.println("컨디션: " + post.getCondition());
        System.out.println("상세설명: " + post.getDescription());
        System.out.println("거래위치: " + post.getLocation());
        System.out.println("생성일: " + post.getCreatedAt());
        System.out.println("수정일: " + post.getUpdatedAt());
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
        return switch (label) {
            case "상" -> ConditionLevel.HIGH;
            case "중" -> ConditionLevel.MEDIUM;
            case "하" -> ConditionLevel.LOW;
            default -> null;
        };
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
        return switch (opt) {
            case 1 -> "가격낮은순";
            case 2 -> "가격높은순";
            case 3 -> "최신순";
            case 4 -> "카테고리";
            default -> "기본";
        };
    }
}