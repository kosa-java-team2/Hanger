package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.ConditionLevel;
import org.example.model.Post;
import org.example.model.PostStatus;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PriceUtil;
import org.example.util.SortUtil;
import org.example.util.ProfanityFilter;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PostService 클래스
 * - 게시물 등록, 검색/조회, 수정/삭제 등 게시물과 관련된 모든 기능을 담당
 * - DataStore를 통해 데이터 접근 및 저장을 수행
 * - 콘솔 기반 사용자 입력(InputUtil)과 출력(System.out)을 사용
 */
public class PostService {
    private final DataStore store;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 검색 시 카테고리 필터링에 사용되는 변수
    private String selectedCategory = null;

    public PostService(DataStore store) {
        this.store = store;
    }

    // ===================== 등록 =====================

    /**
     * createPost
     * - 새로운 게시물을 생성하는 메서드
     * - 입력 단계: 제목, 카테고리, 가격, 상태, 상세설명, 위치
     * - 각 입력 값은 금칙어/유효성 검사를 거침
     * - 정상 입력 시 Post 객체를 생성하여 DataStore에 저장
     */
    public void createPost(User user) {
        System.out.println("====== 게시물 등록 ====== ");

        // 제목 입력 및 금칙어 검사
        String title = InputUtil.readNonEmptyLine("상품명: ");
        if (ProfanityFilter.containsBannedWord(title)) {
            System.out.println("금칙어가 포함되어 등록할 수 없습니다.");
            return;
        }

        // 카테고리 입력 및 검증 (사전에 정의된 카테고리만 허용)
        Set<String> allowedCategories = new HashSet<>(Arrays.asList("상의", "하의", "모자", "신발"));
        String category = InputUtil.readNonEmptyLine("카테고리(상의/하의/모자/신발): ").trim();
        if (!allowedCategories.contains(category)) {
            System.out.println("카테고리는 상의/하의/모자/신발 중 하나여야 합니다.");
            return;
        }

        // 가격 입력 및 숫자 형식 검증
        Integer price = InputUtil.readPriceAsInt("가격(숫자 또는 1,000 형식): ");
        if (price == null || price < 0) {
            System.out.println("가격 형식이 올바르지 않습니다.");
            return;
        }

        // 상품 상태 입력 (상/중/하) → ConditionLevel enum 매핑
        String conditionInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ").trim();
        ConditionLevel conditionLevel = mapConditionFromLabel(conditionInput);
        if (conditionLevel == null) {
            System.out.println("상품 상태는 상/중/하 중 하나여야 합니다.");
            return;
        }

        // 상세 설명 입력 및 금칙어 검사
        String description = InputUtil.readNonEmptyLine("상품 상세설명: ");
        if (ProfanityFilter.containsBannedWord(description)) {
            System.out.println("금칙어가 포함되어 등록할 수 없습니다.");
            return;
        }

        // 거래 희망 위치 입력
        String location = InputUtil.readNonEmptyLine("원하는 거래위치: ");

        // 고유 게시물 번호 발급 후 Post 객체 생성
        int postId = store.nextPostId();
        Post newPost = new Post.Builder(postId, user.getId())
                .title(title)
                .category(category)
                .price(price)
                .location(location)
                .condition(conditionLevel)
                .description(description)
                .build();

        // DataStore에 저장 후 디스크 반영
        store.posts().put(postId, newPost);
        store.saveToDisk();

        System.out.println("======================");
        System.out.println("게시물이 등록되었습니다! (번호: " + postId + ")");
    }

    // ===================== 검색/조회 =====================

    /**
     * searchAndView
     * - 게시물을 검색하고, 페이지 단위로 출력
     * - 키워드, 정렬, 카테고리 필터링 지원
     * - 상세조회, 거래 요청, 정렬 변경, 페이지 이동 가능
     */
    public void searchAndView(User currentUser) {
        System.out.println("====== 상품 검색 ======");
        System.out.print("검색어(빈칸=전체): ");
        String inputKeyword = InputUtil.readLine();
        final String keyword = (inputKeyword == null) ? "" : inputKeyword.trim();

        // 검색 필터링 적용
        List<Post> filtered = new ArrayList<>(filterPostsForSearch(currentUser, keyword));
        if (filtered.isEmpty()) {
            System.out.println("검색 결과 없음");
            return;
        }

        int sortOption = 3; // 기본 정렬: 최신순
        SortUtil.applyPostSort(filtered, sortOption);

        int currentPage = 1;

        // 무한 루프 → 명령어 입력에 따라 동작
        while (true) {
            // 현재 페이지에 맞는 게시물만 추출
            Page page = paginate(filtered, currentPage);

            // 페이지 정보와 게시물 출력
            renderPageHeader(page.total, page.currentPage, page.totalPages, sortOption);
            renderPosts(page.items);

            // 사용자 명령어 입력
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

    // 명령어 종류 정의 (검색/조회 화면에서 사용)
    private enum Command {NEXT, PREV, SORT, GOTO, VIEW, REQUEST, EXIT, UNKNOWN}

    /**
     * @param items       현재 페이지에 속한 게시물
     * @param currentPage 현재 페이지 번호
     * @param totalPages  총 페이지 수
     * @param total       전체 게시물 수
     */ // 페이지 정보 저장용 내부 클래스
    private record Page(List<Post> items, int currentPage, int totalPages, int total) {
    }

    /**
     * filterPostsForSearch
     * - 검색 조건에 맞는 게시물만 필터링
     * - 제외 조건: 삭제됨, 완료 상태, 본인 게시물
     * - 포함 조건: 키워드 포함 여부, 카테고리 필터링
     */
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

    /**
     * shouldSkipPostForSearch
     * - 검색에서 제외해야 할 게시물 조건 판별
     */
    private boolean shouldSkipPostForSearch(Post post, User currentUser, String keywordLower) {
        if (post.isDeleted()) return true;
        if (post.getStatus() == PostStatus.COMPLETED) return true;
        if (isPostOwnedByUser(post, currentUser)) return true;
        return !keywordLower.isEmpty() && !postMatchesKeyword(post, keywordLower);
    }

    // 본인 소유 게시물 여부 확인
    private boolean isPostOwnedByUser(Post post, User user) {
        return user != null && post.getSellerId() != null && post.getSellerId().equals(user.getId());
    }

    // 키워드가 제목/설명에 포함되는지 확인
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
     * paginate
     * - 전체 게시물을 페이지 단위로 나누는 기능
     * - 페이지당 게시물 수 = 10개
     */
    private Page paginate(List<Post> posts, int currentPage) {
        int total = posts.size();
        int totalPages = Math.max(1, (total + 10 - 1) / 10);

        int safePage = Math.max(1, Math.min(currentPage, totalPages));
        int fromIndex = (safePage - 1) * 10;
        int toIndex = Math.min(fromIndex + 10, total);
        return new Page(posts.subList(fromIndex, toIndex), safePage, totalPages, total);
    }

    // 페이지 상단 정보 출력
    private void renderPageHeader(int total, int page, int totalPages, int sortOpt) {
        System.out.println("======================");
        System.out.println("총 " + total + "건 | 페이지 " + page + "/" + totalPages + " | 정렬: " + sortLabel(sortOpt));
    }

    // 페이지 내 게시물 리스트 출력
    private void renderPosts(List<Post> posts) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

    // 사용자 명령어 입력 처리
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

    // 페이지 이동(다음)
    private int nextPage(int current, int totalPages) {
        if (current < totalPages) return current + 1;
        System.out.println("마지막 페이지입니다.");
        return current;
    }

    // 페이지 이동(이전)
    private int prevPage(int current) {
        if (current > 1) return current - 1;
        System.out.println("첫 페이지입니다.");
        return current;
    }

    /**
     * 정렬 방식 선택
     * - 1. 가격 낮은순
     * - 2. 가격 높은순
     * - 3. 최신순
     * - 4. 카테고리별
     */
    private int readSortOption() {
        System.out.println("정렬 방식을 선택하세요: 1.가격낮은순 2.가격높은순 3.최신순 4.카테고리");
        int option = InputUtil.readIntInRange("선택: ", 1, 4);

        if (option == 4) {
            selectedCategory = readCategoryOption();
            System.out.println("선택한 카테고리: " + selectedCategory);
        } else {
            selectedCategory = null;
        }
        return option;
    }

    // 카테고리 선택 입력
    private String readCategoryOption() {
        System.out.println("카테고리를 선택하세요: 1.상의 2.하의 3.모자 4.신발");
        int categoryOption = InputUtil.readIntInRange("선택: ", 1, 4);

        return switch (categoryOption) {
            case 1 -> "상의";
            case 2 -> "하의";
            case 3 -> "모자";
            case 4 -> "신발";
            default -> "";
        };
    }

    // 페이지 번호 직접 입력
    private int readGoto(int totalPages) {
        return InputUtil.readIntInRange("이동할 페이지(1-" + totalPages + "): ", 1, totalPages);
    }

    // ===================== 상세조회/거래요청 =====================

    /**
     * 게시물 상세조회
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
     * 거래 요청 처리
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

    // ===================== 내 게시글 관리 =====================

    /**
     * manageMyPosts
     * - 사용자가 작성한 게시물을 조회/수정/삭제 가능
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
            System.out.println("정말 수정하시겠습니까?");
            System.out.println("1. 예   2. 아니요");
            int confirm = InputUtil.readInt("선택: ");
            if (confirm == 1) {
                editPost(currentUser, targetPost);
                System.out.println("수정 완료되었습니다.");
            } else {
                System.out.println("수정을 취소했습니다.");
            }
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

    /**
     * editPost
     * - 특정 게시글을 수정하는 기능
     * - 판매자 본인만 수정 가능
     * - 수정 가능한 항목:
     * 1. 제목 (금칙어 검사 적용)
     * 2. 카테고리 (제한 없음, 단순 입력)
     * 3. 가격 (숫자/천 단위 문자열 입력 허용, 음수 불가)
     * 4. 상품 상태 (상/중/하 → ConditionLevel 매핑)
     * 5. 상세 설명 (금칙어 검사 적용)
     * 6. 거래 위치 (단순 문자열 입력)
     * 7. 거래 상태 (판매중/거래중/거래완료 → PostStatus 매핑)
     * - 수정 후 DataStore에 저장하여 영구 반영
     */
    private void editPost(User currentUser, Post post) {
        // 판매자 본인 확인
        if (!post.getSellerId().equals(currentUser.getId())) {
            System.out.println("판매자만 게시물을 수정할 수 있습니다.");
            return;
        }

        // 수정할 항목 선택 메뉴
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
            case 1: // 제목 수정
                String newTitle = InputUtil.readNonEmptyLine("새 제목: ");
                if (ProfanityFilter.containsBannedWord(newTitle)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setTitle(newTitle);
                break;
            case 2: // 카테고리 수정
                post.setCategory(InputUtil.readNonEmptyLine("새 카테고리: "));
                break;
            case 3: // 가격 수정
                Integer newPrice = InputUtil.readPriceAsInt("새 가격(숫자 or 1,000): ");
                if (newPrice == null || newPrice < 0) {
                    System.out.println("가격 오류");
                    return;
                }
                post.setPrice(newPrice);
                break;
            case 4: // 상품 상태 수정 (상/중/하)
                String conditionInput = InputUtil.readNonEmptyLine("상품 상태(상/중/하): ");
                ConditionLevel conditionLevel = mapConditionFromLabel(conditionInput.trim());
                if (conditionLevel == null) {
                    System.out.println("상/중/하 중 하나여야 함");
                    return;
                }
                post.setCondition(conditionLevel);
                break;
            case 5: // 상세 설명 수정
                String newDescription = InputUtil.readNonEmptyLine("새 설명: ");
                if (ProfanityFilter.containsBannedWord(newDescription)) {
                    System.out.println("금칙어 포함");
                    return;
                }
                post.setDescription(newDescription);
                break;
            case 6: // 거래 위치 수정
                post.setLocation(InputUtil.readNonEmptyLine("새 거래위치: "));
                break;
            case 7: // 거래 상태 변경
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
        // 수정 결과 저장
        store.saveToDisk();
    }

    /**
     * printDetail
     * - 게시글의 상세 내용을 출력
     * - 출력 항목:
     * 게시글 번호, 제목, 카테고리, 가격, 거래 상태, 컨디션, 상세설명, 거래 위치, 생성일/수정일, 판매자 정보
     * - 판매자 정보에는 닉네임과 등급 표시
     */
    private void printDetail(Post post) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
     * mapConditionFromLabel
     * - 문자열("상/중/하")을 ConditionLevel enum 값으로 매핑
     * - 잘못된 입력일 경우 null 반환
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
     * getUserRank
     * - 판매자 등급 계산
     * - 기준: 작성한 게시물 수(삭제 제외)
     * · 30개 이상 → PLATINUM
     * · 15개 이상 → GOLD
     * · 5개 이상  → SILVER
     * · 나머지    → BRONZE
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
     * sortLabel
     * - 정렬 옵션 번호를 라벨 문자열로 변환
     * - 출력용으로 사용
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