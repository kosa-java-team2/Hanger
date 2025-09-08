package org.example.model;

import lombok.Getter;

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
 */
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ===================== 기본 정보 =====================
    // ===================== Getter/Setter =====================
    /** 사용자 고유 ID (Primary Key 역할) */
    @Getter
    private final String id;

    /** 닉네임 (변경 가능) */
    @Getter
    private String nickname;

    /** 이름 (실명) */
    @Getter
    private final String name;

    /** 주민등록번호 (예: 123456-1234567) */
    private final String residentRegistrationNumber;

    /** 나이 */
    @Getter
    private final int age;

    /** 성별 ("M" 또는 "F") */
    @Getter
    private final String gender;

    /** 사용자 역할 (기본값: MEMBER) */
    @Getter
    private Role role;

    // ===================== 보안 정보 =====================
    /** 비밀번호 해싱을 위한 salt (Base64 인코딩) */
    @Getter
    private final String salt;

    /** 비밀번호 해시값 (Base64 인코딩) */
    @Getter
    private final String passwordHash;

    // ===================== 메타데이터 =====================
    /** 가입일시 */
    @Getter
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 마지막 수정일시 */
    @Getter
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===================== 신뢰도 =====================
    /** 좋은 평가 횟수 */
    @Getter
    private int trustGood;

    /** 나쁜 평가 횟수 */
    @Getter
    private int trustBad;

    // ===================== 생성자 =====================
    /**
     * Builder 전용 생성자
     * 외부에서 직접 생성자 호출을 막고, 반드시 Builder를 통해 생성하도록 강제한다.
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
        this.role = (builder.role != null) ? builder.role : Role.MEMBER; // 기본 역할은 MEMBER
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // ===================== Builder =====================
    /**
     * User 객체 생성을 위한 Builder 클래스
     * - 필수: id, nickname, name, rrn
     * - 선택: age, gender, salt, passwordHash, role
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
            this.id = id;
            this.nickname = nickname;
            this.name = name;
            this.rrn = rrn;
        }

        public Builder age(int age) { this.age = age; return this; }
        public Builder gender(String gender) { this.gender = gender; return this; }
        public Builder salt(String salt) { this.salt = salt; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(Role role) { this.role = role; return this; }

        /** 최종 User 객체 생성 */
        public User build() { return new User(this); }
    }

    // ===================== 도메인 동작 =====================
    /** updatedAt을 현재 시각으로 갱신 */
    public void refreshUpdatedAt() { this.updatedAt = LocalDateTime.now(); }

    /** (호환용) 기존 이름 유지 — 내부적으로 refreshUpdatedAt 호출 */
    public void touch() { refreshUpdatedAt(); }

    /** 신뢰도 좋은 평가 1 증가 */
    public void incrementTrustGood() { this.trustGood++; refreshUpdatedAt(); }

    /** 신뢰도 나쁜 평가 1 증가 */
    public void incrementTrustBad() { this.trustBad++; refreshUpdatedAt(); }

    /** (호환용) 기존 메서드 이름 유지 */
    public void addTrustGood() { incrementTrustGood(); }

    /** (호환용) 기존 메서드 이름 유지 */
    public void addTrustBad() { incrementTrustBad(); }

    public String getRrn() { return residentRegistrationNumber; }

    public void setNickname(String nickname) { this.nickname = nickname; refreshUpdatedAt(); }
    public void setRole(Role role) { this.role = role; refreshUpdatedAt(); }

    // ===================== equals & hashCode =====================
    /**
     * 두 User 객체는 id가 같으면 동일한 사용자로 간주한다.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User other)) return false;  // Java 16+ 패턴 매칭
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    // ===================== toString =====================
    /**
     * 사용자 정보를 사람이 읽기 쉬운 문자열로 반환
     * (비밀번호 관련 정보는 포함하지 않는다)
     */
    @Override
    public String toString() {
        return String.format(
                "User{id='%s', nick='%s', age=%d, gender=%s, role=%s, createdAt=%s, trust(G:%d,B:%d)}",
                id, nickname, age, gender, role, createdAt, trustGood, trustBad
        );
    }
}