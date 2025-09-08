package org.example.service;

import lombok.Getter;
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
 * - 기본 관리자 계정 보장(최초 실행 대비)
 * - 회원가입(Signup)
 * - 로그인(Login) / 로그아웃(Logout)
 * - RRN(주민번호) 기반 성별/나이 산출(단순화)
 * <p>
 * 설계/보안 노트:
 * - 비밀번호는 salt + hash로 저장(평문 저장 금지).
 * - 회원가입 시 ID/닉네임/주민번호 형식 검사 및 중복 검사 수행.
 * - 관리자 전용 로그인 경로(adminOnly=true)에서 Role 확인.
 * - RRN을 저장/처리하므로 개인정보 취급에 각별한 주의 필요(암호화·마스킹·접근통제 권장).
 */
public class AuthService {
    // ===================== 기본 관리자 상수 =====================
    /**
     * 최초 실행 시 자동 생성되는 기본 관리자 계정 정보(데모/학습용). 실제 운영에서는 제거/환경변수화 권장.
     */
    private static final String DEFAULT_ADMIN_ID = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123!";
    private static final String DEFAULT_ADMIN_NICK = "관리자";
    private static final String DEFAULT_ADMIN_NAME = "관리자";
    private static final String DEFAULT_ADMIN_RRN = "000000-3000000"; // 예시 RRN(가짜 값)
    private static final int DEFAULT_ADMIN_AGE = 30;
    private static final String DEFAULT_ADMIN_GENDER = "M";

    // ===================== 상태/협력자 =====================
    private final DataStore store; // 영속 데이터 저장/로드를 담당
    /**
     * -- GETTER --
     *  현재 로그인한 사용자 조회(없으면 null)
     */
    @Getter
    private User currentUser;      // 현재 로그인 사용자(세션 개념)

    public AuthService(DataStore store) {
        this.store = store;
    }

    // ===================== 기본 관리자 보장 =====================

    /**
     * 기본 관리자 계정을 보장한다(최초 실행 대비).
     * - 'admin' 계정이 없으면 salt+hash를 생성하여 ADMIN 권한 사용자 생성 후 저장.
     * - 콘솔에 임시 비밀번호를 안내(운영 환경에서는 노출 금지/로그 남기지 말 것).
     */
    public void ensureDefaultAdmin() {
        Map<String, User> usersById = store.users();
        if (!usersById.containsKey(DEFAULT_ADMIN_ID)) {
            String salt = PasswordUtil.newSalt();
            String passwordHash = PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD, salt);

            User adminUser = new User.Builder(
                    DEFAULT_ADMIN_ID,
                    DEFAULT_ADMIN_NICK,
                    DEFAULT_ADMIN_NAME,
                    DEFAULT_ADMIN_RRN
            )
                    .age(DEFAULT_ADMIN_AGE)
                    .gender(DEFAULT_ADMIN_GENDER)
                    .salt(salt)
                    .passwordHash(passwordHash)
                    .role(Role.ADMIN)
                    .build();

            usersById.put(DEFAULT_ADMIN_ID, adminUser);
            store.saveToDisk();
            System.out.println("기본 관리자 계정 생성: " +
                    DEFAULT_ADMIN_ID + " / " + DEFAULT_ADMIN_PASSWORD);
        }
    }

    /**
     * 회원가입 절차:
     * 1) ID 입력/검증(형식 + 중복)
     * 2) 닉네임 입력/검증(형식 + 중복)
     * 3) 이름 입력
     * 4) 주민번호 입력/검증(형식 + 중복 가입 방지)
     * 5) 비밀번호 2회 확인
     * 6) RRN에서 나이/성별 산출(단순 규칙)
     * 7) salt 생성 → hash 생성 → User 생성/저장
     */
    // ===================== 회원가입 =====================
    public void signup() {
        System.out.println("====================================");
        System.out.println("회원가입을 진행합니다. 다음 정보를 입력해주세요.");

        String userId = readValidUserId();
        String nickname = readValidNickname();
        String name = InputUtil.readNonEmptyLine("이름: ");
        String residentRegistrationNumber = readValidRRN();

        String passwordPlain = InputUtil.readPasswordTwice("비밀번호: ", "비밀번호 확인: ");
        int age = calcAgeFromRRN(residentRegistrationNumber);
        String gender = calcGenderFromRRN(residentRegistrationNumber);

        String salt = PasswordUtil.newSalt();
        String passwordHash = PasswordUtil.hash(passwordPlain, salt);

        User newUser = new User.Builder(userId, nickname, name, residentRegistrationNumber)
                .age(age)
                .gender(gender)
                .salt(salt)
                .passwordHash(passwordHash)
                .role(Role.MEMBER)
                .build();

        store.users().put(userId, newUser);
        store.rrnSet().add(residentRegistrationNumber);
        store.saveToDisk();

        System.out.println("회원가입이 완료되었습니다. (" + newUser + ")");
        System.out.println("====================================");
    }

    // ========== 입력 + 검증 헬퍼들 ==========

    /**
     * 형식 + 중복을 모두 통과하는 ID를 읽어온다.
     */
    private String readValidUserId() {
        while (true) {
            String inputUserId = InputUtil.readNonEmptyLine("아이디(영문/숫자 4~16, 특수문자 불가): ");

            if (!RegexUtil.isValidUserId(inputUserId)) {
                System.out.println("형식 오류: 영문/숫자 4~16자만 허용됩니다.");
                continue;
            }
            if (!isUserIdUnique(inputUserId)) {
                System.out.println("이미 사용중인 아이디입니다.");
                continue;
            }
            return inputUserId;
        }
    }

    /**
     * 형식 + 중복을 모두 통과하는 닉네임을 읽어온다.
     */
    private String readValidNickname() {
        while (true) {
            String inputNickname = InputUtil.readNonEmptyLine("닉네임(공백 불가, 2~20자): ");

            if (!RegexUtil.isValidNickname(inputNickname)) {
                System.out.println("형식 오류: 공백 없이 2~20자여야 합니다.");
                continue;
            }
            if (!isNicknameUnique(inputNickname)) {
                System.out.println("이미 사용중인 닉네임입니다.");
                continue;
            }
            return inputNickname;
        }
    }

    /**
     * 형식 + 중복가입 방지를 모두 통과하는 RRN을 읽어온다.
     */
    private String readValidRRN() {
        while (true) {
            String inputRrn = InputUtil.readNonEmptyLine("주민번호(예: 000000-0000000): ");

            if (!RegexUtil.isValidRRN(inputRrn)) {
                System.out.println("형식 오류: 6자리-7자리 형식이어야 합니다.");
                continue;
            }
            if (!isRRNUnique(inputRrn)) {
                System.out.println("기존 회원가입 이력이 있습니다.");
                continue;
            }
            return inputRrn;
        }
    }

    // ========== 중복/형식 보조 ==========

    /**
     * ID 중복 여부(없어야 통과).
     */
    private boolean isUserIdUnique(String userId) {
        return !store.users().containsKey(userId);
    }

    /**
     * 닉네임 중복 여부(없어야 통과).  stream 대신 반복문으로 초보자 친화
     */
    private boolean isNicknameUnique(String nickname) {
        for (User user : store.users().values()) {
            if (nickname.equals(user.getNickname())) return false;
        }
        return true;
    }

    /**
     * RRN 중복 여부(없어야 통과).
     */
    private boolean isRRNUnique(String rrn) {
        return !store.rrnSet().contains(rrn);
    }

    // ===================== 로그인/로그아웃 =====================

    /**
     * 로그인 절차:
     * 1) ID/PW 입력
     * 2) 사용자 존재 확인
     * 3) adminOnly=true 인 경우 Role.ADMIN인지 확인
     * 4) 저장된 salt 기반으로 PW 해시 재계산 후 기존 hash와 비교
     * 5) 일치 시 currentUser 설정
     *
     * @param adminOnly 관리자 전용 로그인 경로 여부(true면 ADMIN만 허용)
     */
    public void login(boolean adminOnly) {
        System.out.println("로그인을 시작합니다.");
        String inputUserId = InputUtil.readNonEmptyLine("아이디: ");
        String inputPassword = InputUtil.readNonEmptyLine("비밀번호: ");

        User foundUser = store.users().get(inputUserId);
        if (foundUser == null) {
            System.out.println("아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }
        if (adminOnly && foundUser.getRole() != Role.ADMIN) {
            System.out.println("관리자 전용 로그인입니다.");
            return;
        }
        String computedHash = PasswordUtil.hash(inputPassword, foundUser.getSalt());
        if (!computedHash.equals(foundUser.getPasswordHash())) {
            System.out.println("아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }
        currentUser = foundUser;
        System.out.println("로그인되었습니다. 환영합니다, " + foundUser.getNickname() + "님!");
    }

    /**
     * 현재 로그인 세션을 종료한다.
     */
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
     * - rrn[7] = 1,2 → 1900년대 출생
     * - rrn[7] = 3,4 → 2000년대 출생
     * (예외/외국인 코드 등은 단순화를 위해 제외)
     */
    private int calcAgeFromRRN(String rrn) {
        String yearTwoDigits = rrn.substring(0, 2);      // 출생 연도(두 자리)
        char genderCenturyCode = rrn.charAt(7);          // 성별/세기 코드
        int year = Integer.parseInt(yearTwoDigits);
        int century = (genderCenturyCode == '1' || genderCenturyCode == '2') ? 1900 : 2000;
        int birthYear = century + year;
        int currentYear = LocalDate.now().getYear();
        return currentYear - birthYear;
    }

    /**
     * 주민번호에서 성별을 계산(단순화).
     * 규칙:
     * - rrn[7] = 1,3 → "M"
     * - rrn[7] = 2,4 → "F"
     */
    private String calcGenderFromRRN(String rrn) {
        char genderCenturyCode = rrn.charAt(7);
        return (genderCenturyCode == '1' || genderCenturyCode == '3') ? "M" : "F";
    }
}