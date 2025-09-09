package org.example.model;

import lombok.Getter;

/**
 * Role
 * -------------------
 * 사용자 권한(역할, Role)을 표현하는 열거형(Enum).
 * <p>
 * 사용 목적:
 * - 시스템 내에서 사용자 계정이 가진 권한 수준을 구분한다.
 * - UI 표시, 인증/인가(Auth) 로직, 관리자 기능 제한 등에 활용된다.
 * <p>
 * 사용처 예시:
 * - {@link org.example.service.AuthService} : 로그인 시 관리자 전용 기능 접근(adminOnly) 검증
 * - {@link org.example.service.AdminService}: 관리자만 수행 가능한 사용자 관리, 게시글 관리 등
 * <p>
 * 설계/호환성 노트:
 * - DataStore 직렬화를 통해 Role이 저장되므로, **상수명 변경/삭제 금지**.
 * (저장된 데이터와 호환성이 깨질 수 있음)
 * - 새로운 권한이 필요하다면 상수를 추가하는 방식으로 확장한다.
 * (예: MODERATOR, SUPER_ADMIN 등)
 * - enum의 **ordinal(순서)에 의존 금지**.
 * → 권한 비교 시에는 명시적으로 equals() 또는 별도의 권한 체계 매핑을 사용해야 한다.
 */
@Getter
public enum Role {
    /**
     * 일반 사용자 (기본 권한)
     * - 서비스의 기본 기능(회원가입, 로그인, 거래 요청, 게시글 작성 등) 사용 가능
     * - 관리자 전용 기능에는 접근 불가
     */
    MEMBER("일반 사용자"),

    /**
     * 관리자 권한
     * - 시스템 운영/관리 기능에 접근 가능
     * - 예: 사용자 차단, 게시글 삭제, 로그 모니터링 등
     */
    ADMIN("관리자");

    // ===================== 필드 =====================
    /**
     * 권한의 한글 라벨
     * - UI나 로그에서 사람이 이해하기 쉽게 표시
     * - 예: "일반 사용자", "관리자"
     */
    private final String label;

    // ===================== 생성자 =====================

    /**
     * Role 생성자
     *
     * @param label 한글 라벨
     */
    Role(String label) {
        this.label = label;
    }
}