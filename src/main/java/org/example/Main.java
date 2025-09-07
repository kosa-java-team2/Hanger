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

public class Main {
    private static final String MSG_EXIT = "프로그램을 종료합니다. 이용해 주셔서 감사합니다!";
    private static final String LINE_EQ = "====================================";

    private final DataStore store = new DataStore();
    private final AuthService auth = new AuthService(store);
    private final PostService postService = new PostService(store);
    private final AdminService adminService = new AdminService(store);
    private final TradeService tradeService = new TradeService(store);
    private final NotificationService notificationService = new NotificationService(store);

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        store.loadAll();           // 직렬화 데이터 로드
        auth.ensureDefaultAdmin(); // 기본 관리자 계정 확보

        while (true) {
            if (auth.getCurrentUser() == null) {
                printWelcome();
                int sel = InputUtil.readIntInRange("선택: ", 0, 3);
                switch (sel) {
                    case 1: auth.signup(); break;
                    case 2: auth.login(false); break;
                    case 3: auth.login(true);  break; // 관리자 로그인
                    case 0:
                        store.saveAll();
                        System.out.println(MSG_EXIT);
                        return; // main while-loop 종료
                    default:
                        printWelcome();
                        System.out.println("잘못된 선택입니다.");
                        break;
                }
            } else if (auth.getCurrentUser().getRole() == Role.ADMIN) {
                showAdminMenu();
            } else {
                showMemberMenu();
            }
        }
    }

    private void printWelcome() {
        System.out.println(LINE_EQ);
        System.out.println("중고거래 시스템에 오신 것을 환영합니다!");
        System.out.println("1. 회원 가입");
        System.out.println("2. 로그인");
        System.out.println("3. 관리자 로그인");
        System.out.println("0. 종료");
        System.out.println(LINE_EQ);
    }

    // 일반 사용자 메뉴
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
                store.saveAll();
                System.out.println(MSG_EXIT);
                System.exit(0);
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }

    // 관리자 메뉴
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
            case 3: auth.logout(); break;
            case 0:
                store.saveAll();
                System.out.println(MSG_EXIT);
                System.exit(0);
                break;
            default:
                System.out.println("잘못된 선택입니다.");
                break;
        }
    }
}