package org.example.model;

/**
 * Role
 * -------------------
 * 사용자 권한(역할)을 표현하는 열거형.
 *
 * 사용처:
 * - {@link org.example.service.AuthService} : 관리자 전용 로그인(adminOnly) 검증
 * - {@link org.example.service.AdminService}: 관리자만 수행 가능한 사용자/게시글 관리
 *
 * 설계/호환성 노트:
 * - 직렬화/영속화(스냅샷)와 연동되므로 **상수명 변경/삭제 금지**.
 *   권한이 늘면 새 상수를 추가하는 방식으로 확장하세요(예: MODERATOR).
 * - enum의 **ordinal(순서) 의존 금지**. 권한 비교가 필요하면 명시적 매핑/체크 로직을 사용하세요.
 */
public enum Role {
    /** 일반 사용자(기본 권한) */
    MEMBER,

    /** 관리자(운영/관리 기능 접근) */
    ADMIN
}