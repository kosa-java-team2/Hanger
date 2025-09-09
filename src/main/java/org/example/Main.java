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
 * 애플리케이션 진입점(콘솔 기반).
 * 전체 메뉴 흐름과 화면 전환을 담당하는 클래스.
 * <p>
 * 주요 책임:
 * - 애플리케이션 부팅 시 직렬화 데이터 로드(DataStore.loadFromDisk)
 * - 최초 실행 시 기본 관리자 계정 생성 보장(AuthService.ensureDefaultAdmin)
 * - 로그인 여부 및 사용자 역할(Role)에 따라 메뉴 분기:
 * · 비로그인 → 환영 메뉴
 * · ADMIN → 관리자 메뉴
 * · MEMBER → 일반 사용자 메뉴
 * - 종료 시 스냅샷 저장(DataStore.saveToDisk) 및 프로그램 종료
 * <p>
 * 설계 노트:
 * - 서비스/저장소는 필드로 구성하여 의존성을 주입(DI)한 구조
 * - 콘솔 입력은 InputUtil로 일원화
 * - 종료 경로는 메뉴에서 선택 가능하며,
 * 데이터 저장 → 종료 메시지 출력 → JVM 종료(System.exit) 순서로 진행
 */
public class Main {
    /**
     * 정상 종료 메시지(중복 방지용 상수)
     */
    private static final String MSG_EXIT = "프로그램을 종료합니다. 이용해 주셔서 감사합니다!";
    /**
     * 화면 구분용 출력 라인
     */
    private static final String LINE_EQ = "====================================";

    // ===================== 협력 객체(서비스/저장소) =====================
    /**
     * 전역 데이터 저장/로드 담당
     */
    private final DataStore store = new DataStore();
    /**
     * 회원가입, 로그인/로그아웃 등 인증/인가
     */
    private final AuthService auth = new AuthService(store);
    /**
     * 게시글 등록/검색/수정/삭제 기능
     */
    private final PostService postService = new PostService(store);
    /**
     * 관리자 전용 기능 (회원 관리, 게시글 관리)
     */
    private final AdminService adminService = new AdminService(store);
    /**
     * 거래 요청/상태 변경/평가 기능
     */
    private final TradeService tradeService = new TradeService(store);
    /**
     * 알림 확인/관리 기능
     */
    private final NotificationService notificationService = new NotificationService(store);

    // ===================== Entry Point =====================
    public static void main(String[] args) {
        new Main().run(); // Main 객체 생성 후 실행
    }

    // ===================== 애플리케이션 루프 =====================

    /**
     * 애플리케이션 메인 루프.
     * <p>
     * 부팅 단계:
     * 1) DataStore.loadFromDisk() 호출 → 저장된 데이터 불러오기
     * 2) AuthService.ensureDefaultAdmin() 호출 → 기본 관리자 계정 생성 보장
     * <p>
     * 루프 단계:
     * - 현재 로그인 사용자(auth.getCurrentUser())가 null인 경우 → 비로그인 메뉴 출력
     * - 로그인 사용자 Role이 ADMIN인 경우 → 관리자 메뉴 출력
     * - 그 외 (일반 사용자) → 회원 메뉴 출력
     */
    private void run() {
        store.loadFromDisk();      // 1) 직렬화 데이터 로드 (없으면 무시)
        auth.ensureDefaultAdmin(); // 2) 기본 관리자 계정 확보 (최초 실행 대비)

        // 메인 이벤트 루프
        while (true) {
            if (auth.getCurrentUser() == null) {
                // ---- 비로그인 상태 ----
                printWelcome();
                int sel = InputUtil.readIntInRange("선택: ", 0, 3);
                switch (sel) {
                    case 1:
                        auth.signup();
                        break;          // 회원가입
                    case 2:
                        auth.login(false);
                        break;      // 일반 로그인
                    case 3:
                        auth.login(true);
                        break;       // 관리자 로그인
                    case 0: // 프로그램 종료
                        store.saveToDisk();
                        System.out.println(MSG_EXIT);
                        return; // run() 종료 → main 종료
                    default:
                        System.out.println("잘못된 선택입니다.");
                        break;
                }
            } else if (auth.getCurrentUser().getRole() == Role.ADMIN) {
                // ---- 관리자 로그인 상태 ----
                showAdminMenu();
            } else {
                // ---- 일반 사용자 로그인 상태 ----
                showMemberMenu();
            }
        }
    }

    // ===================== 화면 렌더링(비로그인) =====================

    /**
     * 비로그인 상태에서 출력되는 환영/진입 메뉴
     */
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
     * 일반 회원 로그인 상태에서 출력되는 메인 메뉴.
     * <p>
     * - 헤더에 사용자 ID, 닉네임, 등급(PostService.getUserRank), 신뢰도 평가(👍/👎) 표시
     * - 기능:
     * 1. 게시글 등록
     * 2. 게시글 검색/조회
     * 3. 내 게시글 관리(수정/삭제)
     * 4. 거래 관리
     * 5. 알림 확인
     * 6. 로그아웃
     * 0. 종료 (System.exit 호출)
     * <p>
     * 종료 경로:
     * - 메뉴 0 선택 시 → store.saveToDisk → 종료 메시지 → System.exit(0)
     */
    private void showMemberMenu() {
        User me = auth.getCurrentUser();
        System.out.println("\n======== 메인 메뉴 ======");
        String rank = postService.getUserRank(me);
        System.out.println(
                "로그인: " + me.getId() +
                        " (" + me.getNickname() +
                        (rank.isEmpty() ? "" : " - " + rank) +
                        " | 신뢰도: 👍 " + me.getTrustGood() + " / 👎 " + me.getTrustBad() +
                        ")"
        );
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
            case 1:
                postService.createPost(me);
                break;
            case 2:
                postService.searchAndView(me);
                break;
            case 3:
                postService.manageMyPosts(me);
                break;
            case 4:
                tradeService.manageTrades(me);
                break;
            case 5:
                notificationService.showMyNotifications(me);
                break;
            case 6:
                auth.logout();
                break;
            case 0:
                store.saveToDisk();
                System.out.println(MSG_EXIT);
                System.exit(0); // JVM 프로세스 종료
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }

    // ===================== 관리자 메뉴 =====================

    /**
     * 관리자 로그인 상태에서 출력되는 메뉴.
     * <p>
     * - 기능:
     * 1. 사용자 목록 조회/삭제
     * 2. 게시글 목록 조회/삭제
     * 3. 로그아웃 (세션만 종료)
     * 0. 종료 (전체 프로그램 종료)
     * <p>
     * 주의:
     * - 메뉴 3(로그아웃)은 세션만 종료 → 다시 로그인 화면으로 이동
     * - 메뉴 0(종료)은 저장 후 JVM 종료
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
            case 1:
                adminService.manageUsers();
                break;
            case 2:
                adminService.managePosts();
                break;
            case 3:
                auth.logout();
                break;
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