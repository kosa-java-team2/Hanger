package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // 기본 정보
    private final String id;           // key
    private String nickname;
    private final String name;
    private final String rrn;          // 123456-1234567
    private final int age;
    private final String gender;       // "M"/"F"
    private Role role;

    // 보안
    private final String salt;         // Base64
    private final String passwordHash; // Base64

    // 메타
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 신뢰도
    private int trustGood;
    private int trustBad;

    // 🔧 프레임워크/직렬화를 위한 기본 생성자 (예: Java Serialization)

    // 🔒 Builder 전용 생성자 (외부에서 직접 9개 인자 생성자 사용 금지)
    private User(Builder b) {
        this.id = b.id;
        this.nickname = b.nickname;
        this.name = b.name;
        this.rrn = b.rrn;
        this.age = b.age;
        this.gender = b.gender;
        this.salt = b.salt;
        this.passwordHash = b.passwordHash;
        this.role = (b.role != null) ? b.role : Role.MEMBER;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // ====== Builder ======
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

        public User build() {
            return new User(this);
        }
    }

    // ====== 도메인 동작 ======
    public void touch() { this.updatedAt = LocalDateTime.now(); }
    public void addTrustGood() { this.trustGood++; touch(); }
    public void addTrustBad() { this.trustBad++; touch(); }

    // ====== Getters/Setters ======
    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public String getRrn() { return rrn; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public Role getRole() { return role; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getTrustGood() { return trustGood; }
    public int getTrustBad() { return trustBad; }

    public void setNickname(String nickname) { this.nickname = nickname; touch(); }
    public void setRole(Role role) { this.role = role; touch(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;  // Java 16+ 패턴 매칭
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format(
                "User{id='%s', nick='%s', age=%d, gender=%s, role=%s, createdAt=%s, trust(G:%d,B:%d)}",
                id, nickname, age, gender, role, createdAt, trustGood, trustBad
        );
    }
}