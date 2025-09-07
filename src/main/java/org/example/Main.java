package org.example;

import org.example.datastore.DataStore;
import org.example.model.Role;
import org.example.model.User;
import org.example.service.AdminService;
import org.example.service.AuthService;
import org.example.service.NotificationService;
import org.example.service.PostService;
import org.example.service.TradeService;
import org.example.util.InputUtil;

/**
 * Main
 * -------------------
 * 애플리케이션 진입점(콘솔 기반). 전체 메뉴 흐름과 화면 전환을 담당한다.
 * <p>
 * 주요 책임:
 *  - 애플리케이션 부팅 시 데이터 로드(DataStore.loadAll) 및 기본 관리자 보장
 *  - 로그인 상태(비로그인/관리자/일반회원)에 따른 메뉴 라우팅
 *  - 종료 시 스냅샷 저장(DataStore.saveAll) 및 자원 정리
 * <p>
 * 설계 노트:
 *  - 서비스/저장소는 필드로 구성(간단한 DI). 테스트 시에는 생성자 주입으로 대체 가능.
 *  - 콘솔 입출력은 InputUtil을 통해 일원화한다.
 *  - 종료 경로는 메뉴별로 존재하며, 저장 → 메시지 출력 → 종료의 순서를 지킨다.
 */
public class Main {
    /** 정상 종료 메시지(중복 사용 방지) */
    private static final String MSG_EXIT = "프로그램을 종료합니다. 이용해 주셔서 감사합니다!";
    /** 화면 구분용 라인 */
    private static final String LINE_EQ = "====================================";

    // ===================== 협력 객체(서비스/저장소) =====================
    /** 애플리케이션 전역 데이터 저장/로드 담당 */
    private final DataStore store = new DataStore();
    /** 인증/인가 */
    private final AuthService auth = new AuthService(store);
    /** 게시글 도메인 */
    private final PostService postService = new PostService(store);
    /** 관리자 기능 */
    private final AdminService adminService = new AdminService(store);
    /** 거래 도메인 */
    private final TradeService tradeService = new TradeService(store);
    /** 알림 도메인 */
    private final NotificationService notificationService = new NotificationService(store);

    // ===================== Entry Point =====================
    public static void main(String[] args) {
        new Main().run();
    }

    // ===================== 애플리케이션 루프 =====================

    /**
     * 애플리케이션 메인 루프.
     * <p>
     * 부팅 단계:
     *  1) 직렬화 스냅샷 로드(store.loadAll)
     *  2) 기본 관리자 계정 보장(auth.ensureDefaultAdmin)
     * <p>
     * 루프 로직:
     *  - 비로그인 상태 → 환영 화면(회원가입/로그인/관리자 로그인/종료)
     *  - 로그인 상태 & ADMIN → 관리자 메뉴
     *  - 로그인 상태 & MEMBER → 일반 사용자 메뉴
     */
    private void run() {
        store.loadFromDisk();           // 1) 직렬화 데이터 로드(없으면 무시)
        auth.ensureDefaultAdmin(); // 2) 기본 관리자 계정 확보(최초 실행 대비)

        // 메인 이벤트 루프
        while (true) {
            if (auth.getCurrentUser() == null) {
                // ---- 비로그인 상태: 환영 메뉴 ----
                printWelcome();
                int sel = InputUtil.readIntInRange("선택: ", 0, 3);
                switch (sel) {
                    case 1: auth.signup(); break;          // 회원가입
                    case 2: auth.login(false); break;      // 일반 로그인
                    case 3: auth.login(true);  break;      // 관리자 로그인
                    case 0: // 종료
                        store.saveToDisk();                   // 현재 상태 스냅샷 저장
                        System.out.println(MSG_EXIT);
                        return;                            // run() 종료 → main 종료
                    default:
                        printWelcome();
                        System.out.println("잘못된 선택입니다.");
                        break;
                }
            } else if (auth.getCurrentUser().getRole() == Role.ADMIN) {
                // ---- 관리자 메뉴 ----
                showAdminMenu();
            } else {
                // ---- 일반 사용자 메뉴 ----
                showMemberMenu();
            }
        }
    }

    // ===================== 화면 렌더링(비로그인) =====================

    /** 비로그인 상태에서 보여주는 환영/진입 메뉴 */
    private void printWelcome() {
        System.out.println(LINE_EQ);
        System.out.println("중고거래 시스템에 오신 것을 환영합니다!");
        System.out.println("1. 회원 가입");
        System.out.println("2. 로그인");
        System.out.println("3. 관리자 로그인");
        System.out.println("0. 종료");
        System.out.println(LINE_EQ);
    }

    // ===================== 일반 사용자 메뉴 =====================

    /**
     * 로그인된 일반 사용자용 메인 메뉴.
     * - 사용자의 등급(PostService.getUserRank)과 닉네임을 헤더에 표시
     * - 기능: 게시글 등록/검색·조회/내 글 관리/내 거래/알림/로그아웃/종료
     * <p>
     * 종료 경로:
     * - 메뉴 0 선택 시 saveAll 후 System.exit(0) 호출(전체 프로세스 종료)
     */
    private void showMemberMenu() {
        User me = auth.getCurrentUser();
        System.out.println("\n======== 메인 메뉴 ======");
        String rank = postService.getUserRank(me);
        System.out.println("로그인: " + me.getId() + " (" + me.getNickname() + (rank.isEmpty() ? "" : " - " + rank) + ")");
        System.out.println("1. 게시글 등록");
        System.out.println("2. 게시글 검색/조회");
        System.out.println("3. 내 게시글 수정/삭제");
        System.out.println("4. 내 거래 관리");
        System.out.println("5. 알림 확인");
        System.out.println("6. 로그아웃");
        System.out.println("0. 종료");
        System.out.println("========================");

        int sel = InputUtil.readIntInRange("원하는 메뉴를 선택하세요: ", 0, 6);
        switch (sel) {
            case 1: postService.createPost(me); break;
            case 2: postService.searchAndView(me); break;
            case 3: postService.manageMyPosts(me); break;
            case 4: tradeService.manageTrades(me); break;
            case 5: notificationService.showMyNotifications(me); break;
            case 6: auth.logout(); break;
            case 0:
                store.saveToDisk();
                System.out.println(MSG_EXIT);
                System.exit(0); // 프로세스 종료(현재는 단일 스레드 CLI 기준)
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }

    // ===================== 관리자 메뉴 =====================

    /**
     * 관리자용 메뉴.
     * - 사용자 목록 조회/삭제
     * - 게시글 목록 조회/삭제
     * - 로그아웃/종료
     * <p>
     * 주의:
     * - 종료(0)와 로그아웃(3)을 혼동하지 않도록 메시지를 명확히 유지.
     */
    private void showAdminMenu() {
        System.out.println("\n====== 관리자 메뉴 ======");
        System.out.println("1. 사용자 목록 조회/삭제");
        System.out.println("2. 게시글 목록 조회/삭제");
        System.out.println("3. 로그아웃");
        System.out.println("0. 종료");
        System.out.println("=========================");
        int sel = InputUtil.readIntInRange("선택: ", 0, 3);
        switch (sel) {
            case 1: adminService.manageUsers(); break;
            case 2: adminService.managePosts(); break;
            case 3: auth.logout(); break;  // 로그인 세션만 종료
            case 0:
                store.saveToDisk();
                System.out.println(MSG_EXIT);
                System.exit(0);
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }
}