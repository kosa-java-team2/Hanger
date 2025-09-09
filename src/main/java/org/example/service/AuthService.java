package org.example.service;

import lombok.Getter;
import org.example.datastore.DataStore;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.util.PasswordUtil;
import org.example.util.ProfanityFilter;
import org.example.util.RegexUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * AuthService
 * -----------------
 * 인증/인가(Authentication & Authorization)를 담당하는 서비스 레이어.
 * <p>
 * 제공 기능:
 * - 기본 관리자 계정 보장 (최초 실행 대비)
 * - 회원가입(Signup)
 * - 로그인(Login) / 로그아웃(Logout)
 * - RRN(주민등록번호) 기반 성별/나이 산출(단순화 버전)
 * <p>
 * 설계/보안 노트:
 * - 비밀번호는 반드시 Salt + Hash로 저장 (평문 저장 금지)
 * - 회원가입 시 ID/닉네임/주민번호 형식 검사 및 중복 검사 수행
 * - 관리자 전용 로그인 경로(adminOnly=true)에서 Role 확인
 * - 주민등록번호(RRN)는 개인정보이므로 저장·출력·로그 취급 시 각별히 주의 필요
 * (암호화·마스킹·접근 통제 권장)
 */
public class AuthService {
    // ===================== 기본 관리자 상수 =====================
    /**
     * 최초 실행 시 자동 생성되는 기본 관리자 계정 정보(데모/학습용).
     * 실제 운영 환경에서는 반드시 제거하거나 환경변수/DB로 대체할 것.
     */
    private static final String DEFAULT_ADMIN_ID = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123!";
    private static final String DEFAULT_ADMIN_NICK = "관리자";
    private static final String DEFAULT_ADMIN_NAME = "관리자";
    private static final String DEFAULT_ADMIN_RRN = "000000-3000000"; // 예시 RRN (실제 주민번호 아님)
    private static final int DEFAULT_ADMIN_AGE = 30;
    private static final String DEFAULT_ADMIN_GENDER = "M";

    // ===================== 상태/협력자 =====================
    /**
     * 애플리케이션 전역 데이터 저장/로드를 담당하는 저장소
     */
    private final DataStore store;

    /**
     * 현재 로그인한 사용자 세션 (없으면 null)
     * - getter 제공, setter는 제공하지 않음
     * - 로그인 시 설정되고, 로그아웃 시 null로 초기화
     */
    @Getter
    private User currentUser;

    public AuthService(DataStore store) {
        this.store = store;
    }

    // ===================== 기본 관리자 보장 =====================

    /**
     * 기본 관리자 계정을 보장한다.
     * <p>
     * 동작:
     * - store.users()에 "admin" 계정이 없으면 생성
     * - salt + hash 기반 비밀번호 생성
     * - Role.ADMIN 부여
     * - 저장 후 안내 메시지 출력 (운영 환경에서는 절대 평문 PW를 출력하지 말 것)
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

    // ===================== 회원가입 =====================

    /**
     * 회원가입 절차:
     * 1) 사용자 ID 입력/검증 (형식 + 중복)
     * 2) 닉네임 입력/검증 (형식 + 중복)
     * 3) 이름 입력
     * 4) 주민번호 입력/검증 (형식 + 중복)
     * 5) 비밀번호 입력 및 확인 (공백/한글/금칙어 제한)
     * 6) RRN 기반 나이/성별 산출
     * 7) salt + hash 비밀번호 저장, User 생성 후 DataStore 저장
     */
    public void signup() {
        System.out.println("====================================");
        System.out.println("회원가입을 진행합니다. 다음 정보를 입력해주세요.");

        // 기본 정보 입력 및 검증
        String userId = readValidUserId();
        String nickname = readValidNickname();
        String name = InputUtil.readNonEmptyLine("이름: ");
        String residentRegistrationNumber = readValidRRN();

        // 비밀번호 입력 및 검증
        String passwordPlain;
        while (true) {
            passwordPlain = InputUtil.readPasswordTwice("비밀번호: ", "비밀번호 확인: ");

            if (passwordPlain.contains(" ")) {
                System.out.println("비밀번호에는 공백(스페이스)을 포함할 수 없습니다.");
                continue;
            }
            if (containsKorean(passwordPlain)) {
                System.out.println("비밀번호에는 한글을 포함할 수 없습니다.");
                continue;
            }
            if (ProfanityFilter.containsBannedWord(passwordPlain)) {
                System.out.println("비밀번호에 금칙어를 포함할 수 없습니다.");
                continue;
            }
            break;
        }

        // 주민번호 기반 나이/성별 계산
        int age = calcAgeFromRRN(residentRegistrationNumber);
        String gender = calcGenderFromRRN(residentRegistrationNumber);

        // 비밀번호 salt + hash 처리
        String salt = PasswordUtil.newSalt();
        String passwordHash = PasswordUtil.hash(passwordPlain, salt);

        // User 객체 생성
        User newUser = new User.Builder(userId, nickname, name, residentRegistrationNumber)
                .age(age)
                .gender(gender)
                .salt(salt)
                .passwordHash(passwordHash)
                .role(Role.MEMBER)
                .build();

        // 저장소 반영
        store.users().put(userId, newUser);
        store.rrnSet().add(residentRegistrationNumber);
        store.saveToDisk();

        // 가입 완료 안내
        System.out.println(
                "로그인: " + newUser.getId() +
                        " (" + newUser.getNickname() +
                        (newUser.getRole() == null ? "" : " - " + newUser.getRole()) +
                        " | 신뢰도: 👍 " + newUser.getTrustGood() +
                        " / 👎 " + newUser.getTrustBad() +
                        " | 시간: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                        ")"
        );
        System.out.println("====================================");
    }

    // ===================== 입력 + 검증 헬퍼 =====================

    // ID 입력 + 형식검사 + 중복검사
    private String readValidUserId() {
        while (true) {
            String inputUserId = InputUtil.readNonEmptyLine("아이디(영문/숫자, 4~16자): ");

            if (ProfanityFilter.containsBannedWord(inputUserId)) {
                System.out.println("아이디에 금칙어를 포함할 수 없습니다.");
                continue;
            }
            if (containsKorean(inputUserId)) {
                System.out.println("아이디에는 한글을 포함할 수 없습니다.");
                continue;
            }
            if (!RegexUtil.isValidUserId(inputUserId)) { // ✅ 올바른 경우 true, 아니면 오류
                System.out.println("형식 오류: 아이디는 영문/숫자만 허용되며, 4~16자여야 합니다.");
                continue;
            }
            if (!isUserIdUnique(inputUserId)) {
                System.out.println("이미 사용중인 아이디입니다.");
                continue;
            }
            return inputUserId;
        }
    }

    // 닉네임 입력 + 형식검사 + 중복검사
    private String readValidNickname() {
        while (true) {
            String inputNickname = InputUtil.readNonEmptyLine("닉네임(공백 불가, 2~20자): ");

            if (ProfanityFilter.containsBannedWord(inputNickname)) {
                System.out.println("닉네임에 금칙어를 포함할 수 없습니다.");
                continue;
            }
            if (!RegexUtil.isValidNickname(inputNickname)) { // ✅ 올바른 경우 true
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

    // RRN 입력 + 형식검사 + 중복검사
    private String readValidRRN() {
        while (true) {
            String inputRrn = InputUtil.readNonEmptyLine("주민번호(예: 000000-0000000): ");

            if (!RegexUtil.isValidRRN(inputRrn)) { // ✅ 올바른 경우 true
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

    // ===================== 중복 검증 =====================

    /**
     * ID 중복 여부 확인
     */
    private boolean isUserIdUnique(String userId) {
        return !store.users().containsKey(userId);
    }

    /**
     * 닉네임 중복 여부 확인
     */
    private boolean isNicknameUnique(String nickname) {
        for (User user : store.users().values()) {
            if (nickname.equals(user.getNickname())) return false;
        }
        return true;
    }

    /**
     * RRN 중복 여부 확인
     */
    private boolean isRRNUnique(String rrn) {
        return !store.rrnSet().contains(rrn);
    }

    // ===================== 로그인/로그아웃 =====================

    /**
     * 로그인 절차:
     * 1) ID/PW 입력
     * 2) 사용자 존재 확인
     * 3) adminOnly=true 인 경우 ADMIN Role 확인
     * 4) 저장된 salt 기반 PW hash 재계산 후 기존 hash와 비교
     * 5) 일치 시 currentUser 설정
     *
     * @param adminOnly 관리자 전용 로그인 여부
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

        // 입력받은 비밀번호를 salt와 합쳐 hash 생성 후 검증
        String computedHash = PasswordUtil.hash(inputPassword, foundUser.getSalt());
        if (!computedHash.equals(foundUser.getPasswordHash())) {
            System.out.println("아이디 또는 비밀번호가 올바르지 않습니다.");
            return;
        }

        // 로그인 성공
        currentUser = foundUser;
        System.out.println("로그인되었습니다. 환영합니다, " + foundUser.getNickname() + "님!");
    }

    /**
     * 현재 로그인 세션 종료
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
     * 주민번호에서 나이를 계산 (세기 코드 단순화 버전)
     */
    private int calcAgeFromRRN(String rrn) {
        String yearTwoDigits = rrn.substring(0, 2);   // 출생 연도 2자리
        char genderCenturyCode = rrn.charAt(7);       // 세기/성별 코드
        int year = Integer.parseInt(yearTwoDigits);
        int century = (genderCenturyCode == '1' || genderCenturyCode == '2') ? 1900 : 2000;
        int birthYear = century + year;
        int currentYear = LocalDate.now().getYear();
        return currentYear - birthYear;
    }

    /**
     * 주민번호에서 성별 계산 (단순화 버전)
     */
    private String calcGenderFromRRN(String rrn) {
        char genderCenturyCode = rrn.charAt(7);
        return (genderCenturyCode == '1' || genderCenturyCode == '3') ? "M" : "F";
    }

    /**
     * 문자열에 한글 포함 여부
     */
    private boolean containsKorean(String text) {
        return text.matches(".*[ㄱ-ㅣ가-힣]+.*");
    }
}