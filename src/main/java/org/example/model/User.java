package hanger;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


//public enum Role { MEMBER, ADMIN }
//public enum PostStatus { ON_SALE, SOLD, COMPLETED }
//public enum ConditionLevel { HIGH, MEDIUM, LOW }

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
//    public enum Role { MEMBER, ADMIN }
//    public enum PostStatus { ON_SALE, SOLD, COMPLETED }
//    public enum ConditionLevel { HIGH, MEDIUM, LOW }

    
    
    private String id;           // key
    private String nickname;
    private String name;
    private String rrn;          // 123456-1234567
    private int age;
    private String gender;       // "M"/"F"
    private Role role = Role.MEMBER;

    // 보안
    private String salt;         // Base64
    private String passwordHash; // Base64

    public User() {}

    public User(String id, String nickname, String name, String rrn, int age, String gender,
                String salt, String passwordHash, Role role) {
        this.id = id;
        this.nickname = nickname;
        this.name = name;
        this.rrn = rrn;
        this.age = age;
        this.gender = gender;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.MEMBER;
    }

    // getters/setters
    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public String getRrn() { return rrn; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public Role getRole() { return role; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(id, ((User)o).id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("User{id='%s', nick='%s', age=%d, gender=%s, role=%s}", id, nickname, age, gender, role);
    }
}


