package org.example.service;

import org.example.datastore.DataStore;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PasswordUtil;
import org.example.util.RegexUtil;

import java.time.LocalDate;
import java.util.Map;

/**
 * AuthService
 * -----------------
 * 인증/인가를 담당하는 서비스 레이어.
 * 제공 기능:
 *  - 기본 관리자 계정 보장(최초 실행 대비)
 *  - 회원가입(Signup)
 *  - 로그인(Login) / 로그아웃(Logout)
 *  - RRN(주민번호) 기반 성별/나이 산출(단순화)
 *
 * 설계/보안 노트:
 * - 비밀번호는 salt + hash로 저장(평문 저장 금지).
 * - 회원가입 시 ID/닉네임/주민번호 형식 검사 및 중복 검사 수행.
 * - 관리자 전용 로그인 경로(adminOnly=true)에서 Role 확인.
 * - RRN을 저장/처리하므로 개인정보 취급에 각별한 주의 필요(암호화·마스킹·접근통제 권장).
 */
public class AuthService {
    // ===================== 기본 관리자 상수 =====================
    /** 최초 실행 시 자동 생성되는 기본 관리자 계정 정보(데모/학습용). 실제 운영에서는 제거/환경변수화 권장. */
    private static final String DEFAULT_ADMIN_ID = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123!";
    private static final String DEFAULT_ADMIN_NICK = "관리자";
    private static final String DEFAULT_ADMIN_NAME = "관리자";
    private static final String DEFAULT_ADMIN_RRN = "000000-3000000"; // 예시 RRN(가짜 값)
    private static final int    DEFAULT_ADMIN_AGE = 30;
    private static final String DEFAULT_ADMIN_GENDER = "M";

    // ===================== 상태/협력자 =====================
    private final DataStore store; // 영속 데이터 저장/로드를 담당
    private User currentUser;      // 현재 로그인 사용자(세션 개념)

    public AuthService(DataStore store) {
        this.store = store;
    }

    /** 현재 로그인한 사용자 조회(없으면 null) */
    public User getCurrentUser() { return currentUser; }

    // ===================== 기본 관리자 보장 =====================

    /**
     * 기본 관리자 계정을 보장한다(최초 실행 대비).
     * - 'admin' 계정이 없으면 salt+hash를 생성하여 ADMIN 권한 사용자 생성 후 저장.
     * - 콘솔에 임시 비밀번호를 안내(운영 환경에서는 노출 금지/로그 남기지 말 것).
     */
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

    // ===================== 회원가입 =====================

    /**
     * 회원가입 절차:
     *  1) ID 입력/검증(형식 + 중복)
     *  2) 닉네임 입력/검증(형식 + 중복)
     *  3) 이름 입력
     *  4) 주민번호 입력/검증(형식 + 중복 가입 방지)
     *  5) 비밀번호 2회 확인
     *  6) RRN에서 나이/성별 산출(단순 규칙)
     *  7) salt 생성 → hash 생성 → User 생성/저장
     */
    public void signup() {
        System.out.println("====================================");
        System.out.println("회원가입을 진행합니다. 다음 정보를 입력해주세요.");

        // --- 아이디 입력/검증 ---
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

        // --- 닉네임 입력/검증 ---
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

        // --- 이름 입력 ---
        String name = InputUtil.readNonEmptyLine("이름: ");

        // --- 주민번호 입력/검증 ---
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

        // --- 비밀번호 입력/확인 ---
        String pw = InputUtil.readPasswordTwice("비밀번호: ", "비밀번호 확인: ");

        // --- RRN → 나이/성별 산출(단순화 로직) ---
        int age = calcAgeFromRRN(rrn);
        String gender = calcGenderFromRRN(rrn);

        // --- 비밀번호 해싱 ---
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(pw, salt);

        // --- User 생성 및 저장 ---
        User u = new User.Builder(id, nickname, name, rrn)
                .age(age)
                .gender(gender)
                .salt(salt)
                .passwordHash(hash)
                .role(Role.MEMBER)
                .build();

        store.users().put(id, u);
        store.rrnSet().add(rrn); // RRN 중복 방지 인덱스에 추가
        store.saveAll();

        System.out.println("회원가입이 완료되었습니다. (" + u + ")");
        System.out.println("====================================");
    }

    // ===================== 로그인/로그아웃 =====================

    /**
     * 로그인 절차:
     *  1) ID/PW 입력
     *  2) 사용자 존재 확인
     *  3) adminOnly=true 인 경우 Role.ADMIN인지 확인
     *  4) 저장된 salt 기반으로 PW 해시 재계산 후 기존 hash와 비교
     *  5) 일치 시 currentUser 설정
     *
     * @param adminOnly 관리자 전용 로그인 경로 여부(true면 ADMIN만 허용)
     */
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

    /** 현재 로그인 세션을 종료한다. */
    public void logout() {
        if (currentUser == null) {
            System.out.println("현재 로그인 상태가 아닙니다.");
            return;
        }
        System.out.println("로그아웃되었습니다.");
        currentUser = null;
    }

    // ===================== RRN 유틸(단순화) =====================

    /**
     * 주민번호에서 나이를 계산(국제식/단순화).
     * 규칙:
     *  - rrn[7] = 1,2 → 1900년대 출생
     *  - rrn[7] = 3,4 → 2000년대 출생
     *  (예외/외국인 코드 등은 단순화를 위해 제외)
     */
    private int calcAgeFromRRN(String rrn) {
        String yy = rrn.substring(0, 2); // 출생 연도(두 자리)
        char g = rrn.charAt(7);          // 성별/세기 코드
        int year = Integer.parseInt(yy);
        int century = (g == '1' || g == '2') ? 1900 : 2000;
        int birthYear = century + year;
        int nowYear = LocalDate.now().getYear();
        return nowYear - birthYear;
    }

    /**
     * 주민번호에서 성별을 계산(단순화).
     * 규칙:
     *  - rrn[7] = 1,3 → "M"
     *  - rrn[7] = 2,4 → "F"
     */
    private String calcGenderFromRRN(String rrn) {
        char g = rrn.charAt(7);
        return (g == '1' || g == '3') ? "M" : "F";
    }
}