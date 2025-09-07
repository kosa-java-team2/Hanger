package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PasswordUtil;
import org.example.util.RegexUtil;

import java.time.LocalDate;
import java.util.Map;

public class AuthService {
    private static final String DEFAULT_ADMIN_ID = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123!";
    private static final String DEFAULT_ADMIN_NICK = "관리자";
    private static final String DEFAULT_ADMIN_NAME = "관리자";
    private static final String DEFAULT_ADMIN_RRN = "000000-3000000";
    private static final int    DEFAULT_ADMIN_AGE = 30;
    private static final String DEFAULT_ADMIN_GENDER = "M";

    private final DataStore store;
    private User currentUser;

    public AuthService(DataStore store) {
        this.store = store;
    }

    public User getCurrentUser() { return currentUser; }

    // 기본 관리자 보장 (최초 실행 대비)
    public void ensureDefaultAdmin() {
        Map<String, User> users = store.users();
        if (!users.containsKey(DEFAULT_ADMIN_ID)) {
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD, salt);

            User admin = new User.Builder(
                    DEFAULT_ADMIN_ID,
                    DEFAULT_ADMIN_NICK,
                    DEFAULT_ADMIN_NAME,
                    DEFAULT_ADMIN_RRN
            )
                    .age(DEFAULT_ADMIN_AGE)
                    .gender(DEFAULT_ADMIN_GENDER)
                    .salt(salt)
                    .passwordHash(hash)
                    .role(Role.ADMIN)
                    .build();

            users.put(DEFAULT_ADMIN_ID, admin);
            store.saveAll();
            System.out.println("기본 관리자 계정 생성: " +
                    DEFAULT_ADMIN_ID + " / " + DEFAULT_ADMIN_PASSWORD);
        }
    }

    // 회원가입
    public void signup() {
        System.out.println("====================================");
        System.out.println("회원가입을 진행합니다. 다음 정보를 입력해주세요.");

        String id;
        while (true) {
            id = InputUtil.readNonEmptyLine("아이디(영문/숫자 4~16, 특수문자 불가): ");
            if (!RegexUtil.isValidUserId(id)) {
                System.out.println("형식 오류: 영문/숫자 4~16자만 허용됩니다.");
                continue;
            }
            if (store.users().containsKey(id)) {
                System.out.println("이미 사용중인 아이디입니다.");
                continue;
            }
            break;
        }

        String nickname;
        while (true) {
            nickname = InputUtil.readNonEmptyLine("닉네임(공백 불가, 2~20자): ");
            if (!RegexUtil.isValidNickname(nickname)) {
                System.out.println("형식 오류: 공백 없이 2~20자여야 합니다.");
                continue;
            }
            boolean dup = false;
            for (User u : store.users().values()) {
                if (nickname.equals(u.getNickname())) {
                    dup = true;
                    break;
                }
            }
            if (dup) {
                System.out.println("이미 사용중인 닉네임입니다.");
                continue;
            }
            break;
        }

        String name = InputUtil.readNonEmptyLine("이름: ");

        String rrn;
        while (true) {
            rrn = InputUtil.readNonEmptyLine("주민번호(예: 000000-0000000): ");
            if (!RegexUtil.isValidRRN(rrn)) {
                System.out.println("형식 오류: 6자리-7자리 형식이어야 합니다.");
                continue;
            }
            if (store.rrnSet().contains(rrn)) {
                System.out.println("기존 회원가입 이력이 있습니다.");
                continue;
            }
            break;
        }

        String pw = InputUtil.readPasswordTwice("비밀번호: ", "비밀번호 확인: ");

        // 나이/성별 계산
        int age = calcAgeFromRRN(rrn);
        String gender = calcGenderFromRRN(rrn);

        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(pw, salt);

        User u = new User.Builder(id, nickname, name, rrn)
                .age(age)
                .gender(gender)
                .salt(salt)
                .passwordHash(hash)
                .role(Role.MEMBER)
                .build();

        store.users().put(id, u);
        store.rrnSet().add(rrn);
        store.saveAll();

        System.out.println("회원가입이 완료되었습니다. (" + u + ")");
        System.out.println("====================================");
    }

    public void login(boolean adminOnly) {
        System.out.println("로그인을 시작합니다.");
        String id = InputUtil.readNonEmptyLine("아이디: ");
        String pw = InputUtil.readNonEmptyLine("비밀번호: ");

        User u = store.users().get(id);
        if (u == null) {
            System.out.println("아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }
        if (adminOnly && u.getRole() != Role.ADMIN) {
            System.out.println("관리자 전용 로그인입니다.");
            return;
        }
        String hash = PasswordUtil.hash(pw, u.getSalt());
        if (!hash.equals(u.getPasswordHash())) {
            System.out.println("아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }
        currentUser = u;
        System.out.println("로그인되었습니다. 환영합니다, " + u.getNickname() + "님!");
    }

    public void logout() {
        if (currentUser == null) {
            System.out.println("현재 로그인 상태가 아닙니다.");
            return;
        }
        System.out.println("로그아웃되었습니다.");
        currentUser = null;
    }

    // 주민번호에서 나이 계산(국제식, 단순화)
    private int calcAgeFromRRN(String rrn) {
        String yy = rrn.substring(0, 2);
        char g = rrn.charAt(7);
        int year = Integer.parseInt(yy);
        int century = (g == '1' || g == '2') ? 1900 : 2000;
        int birthYear = century + year;
        int nowYear = LocalDate.now().getYear();
        return nowYear - birthYear;
    }

    private String calcGenderFromRRN(String rrn) {
        char g = rrn.charAt(7);
        return (g == '1' || g == '3') ? "M" : "F";
    }
}