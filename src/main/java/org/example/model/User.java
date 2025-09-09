package org.example.model;

import lombok.Getter;
import org.example.util.RegexUtil;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User 클래스
 * -------------------
 * 시스템 내 회원(User)을 표현하는 도메인 모델 클래스.
 * <p>
 * 특징:
 * - 직렬화 가능 (DataStore 저장/로드 지원)
 * - 고유 ID, 기본 정보(이름, 나이, 성별, 주민번호), 보안 정보(비밀번호 해시, salt) 보관
 * - 역할(Role: MEMBER, ADMIN 등) 포함
 * - 신뢰도 시스템(좋은 평가/나쁜 평가 횟수) 지원
 * - Builder 패턴을 사용해 필수/선택 속성을 구분하여 객체 생성
 * <p>
 * 설계 의도:
 * - `RegexUtil`을 이용하여 ID/닉네임/주민번호 형식 검증
 * - 신뢰도(Trust) 점수를 통해 사용자 평가 시스템 지원
 * - equals/hashCode는 ID 기준으로 동일 사용자 판단
 */
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 기본 정보 =====================
    /**
     * 사용자 고유 ID (Primary Key 역할, RegexUtil로 4~16자 영문/숫자 검증)
     */
    @Getter
    private final String id;

    /**
     * 닉네임 (변경 가능, RegexUtil로 공백 제외 2~20자 검증)
     */
    @Getter
    private String nickname;

    /**
     * 이름 (실명)
     */
    @Getter
    private final String name;

    /**
     * 주민등록번호 (예: 123456-1234567, RegexUtil로 형식 검증)
     */
    private final String residentRegistrationNumber;

    /**
     * 나이
     */
    @Getter
    private final int age;

    /**
     * 성별 ("M" 또는 "F")
     */
    @Getter
    private final String gender;

    /**
     * 사용자 역할 (기본값: MEMBER)
     */
    @Getter
    private Role role;

    // ===================== 보안 정보 =====================
    /**
     * 비밀번호 해싱을 위한 salt (Base64 인코딩)
     */
    @Getter
    private final String salt;

    /**
     * 비밀번호 해시값 (Base64 인코딩)
     */
    @Getter
    private final String passwordHash;

    // ===================== 메타데이터 =====================
    /**
     * 가입일시 (객체 생성 시 자동 기록)
     */
    @Getter
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 마지막 수정일시 (정보 변경 시 갱신)
     */
    @Getter
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===================== 신뢰도 =====================
    /**
     * 좋은 평가 횟수
     */
    @Getter
    private int trustGood;

    /**
     * 나쁜 평가 횟수
     */
    @Getter
    private int trustBad;

    // ===================== 생성자 =====================

    /**
     * Builder 전용 생성자
     * 외부에서 직접 호출하지 못하고 반드시 Builder를 통해 객체를 생성해야 함.
     */
    private User(Builder builder) {
        this.id = builder.id;
        this.nickname = builder.nickname;
        this.name = builder.name;
        this.residentRegistrationNumber = builder.rrn;
        this.age = builder.age;
        this.gender = builder.gender;
        this.salt = builder.salt;
        this.passwordHash = builder.passwordHash;
        this.role = (builder.role != null) ? builder.role : Role.MEMBER; // 기본 권한은 MEMBER
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // ===================== Builder =====================

    /**
     * User 객체 생성을 위한 Builder 클래스
     * - 필수: id, nickname, name, rrn
     * - 선택: age, gender, salt, passwordHash, role
     * <p>
     * 사용 예시:
     * User user = new User.Builder("user123", "닉네임", "홍길동", "000000-0000000")
     * .age(25)
     * .gender("M")
     * .role(Role.MEMBER)
     * .build();
     */
    public static class Builder {
        // 필수
        private final String id;
        private final String nickname;
        private final String name;
        private final String rrn;

        // 선택
        private int age;
        private String gender;
        private String salt;
        private String passwordHash;
        private Role role;

        public Builder(String id, String nickname, String name, String rrn) {
            // ===================== 입력 형식 검증 =====================
            if (!RegexUtil.isValidUserId(id)) {
                throw new IllegalArgumentException("❌ 잘못된 사용자 ID 형식입니다. (영문/숫자 4~16자)");
            }
            if (!RegexUtil.isValidNickname(nickname)) {
                throw new IllegalArgumentException("❌ 잘못된 닉네임 형식입니다. (공백 제외 2~20자)");
            }
            if (!RegexUtil.isValidRRN(rrn)) {
                throw new IllegalArgumentException("❌ 잘못된 주민등록번호 형식입니다. (######-#######)");
            }

            this.id = id;
            this.nickname = nickname;
            this.name = name;
            this.rrn = rrn;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder salt(String salt) {
            this.salt = salt;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        /**
         * 최종 User 객체 생성
         */
        public User build() {
            return new User(this);
        }
    }

    // ===================== 도메인 동작 =====================

    /**
     * updatedAt을 현재 시각으로 갱신
     */
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신뢰도 좋은 평가 1 증가
     */
    public void incrementTrustGood() {
        this.trustGood++;
        refreshUpdatedAt();
    }

    /**
     * 신뢰도 나쁜 평가 1 증가
     */
    public void incrementTrustBad() {
        this.trustBad++;
        refreshUpdatedAt();
    }

    /**
     * (호환용) 기존 메서드 이름 유지
     */
    public void addTrustGood() {
        incrementTrustGood();
    }

    /**
     * (호환용) 기존 메서드 이름 유지
     */
    public void addTrustBad() {
        incrementTrustBad();
    }

    /**
     * 주민등록번호 반환
     */
    public String getRrn() {
        return residentRegistrationNumber;
    }

    // ===================== equals & hashCode =====================

    /**
     * 두 User 객체는 id가 같으면 동일한 사용자로 간주한다.
     * (닉네임, 나이, 성별 등은 변할 수 있으므로 비교하지 않음)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User other)) return false;  // Java 16+ 패턴 매칭
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ===================== toString =====================

    /**
     * 사용자 요약 정보를 문자열로 반환
     * (비밀번호 및 주민번호는 포함하지 않는다 — 보안/개인정보 보호 목적)
     * <p>
     * 예시:
     * User{id='user123', nick='닉네임', age=25, gender=M, role=MEMBER, createdAt=2025-01-01T10:00, trust(G:2,B:0)}
     */
    @Override
    public String toString() {
        return String.format(
                "User{id='%s', nick='%s', age=%d, gender=%s, role=%s, createdAt=%s, trust(G:%d,B:%d)}",
                id, nickname, age, gender, role, createdAt, trustGood, trustBad
        );
    }
}