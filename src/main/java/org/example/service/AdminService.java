package org.example.service;

import org.example.model.Post;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.datastore.DataStore;
import org.example.util.SortUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminService
 * -----------------
 * 관리자 전용 기능을 제공하는 서비스 레이어.
 * <p>
 * 제공 기능:
 * - 사용자 목록 조회 및 선택 삭제
 * (단, 관리자 계정은 삭제할 수 없음)
 * - 게시글 목록 조회 및 선택 삭제 (논리 삭제)
 * <p>
 * 설계 노트:
 * - 영속 계층(DataStore)을 주입받아 관리 작업 수행
 * - 콘솔 기반 CLI 환경을 가정하므로 InputUtil을 통한 입력 처리
 * - 삭제 정책:
 * - 사용자 삭제 시: 해당 사용자가 작성한 게시글도 모두 삭제 마킹(논리 삭제) 처리
 * - 게시글 삭제 시: 실제 제거가 아니라 `deleted` 플래그만 true로 마킹 (Soft Delete)
 * - 데이터 변경 후에는 항상 saveToDisk() 호출로 스냅샷 저장
 *
 * @param store 데이터 저장/로드를 담당하는 전역 저장소
 */
public record AdminService(DataStore store) {
    /**
     * AdminService 생성자
     *
     * @param store 전역 DataStore 인스턴스 (싱글톤처럼 공유됨)
     */
    public AdminService {
    }

    // ===================== 사용자 관리 =====================

    /**
     * 사용자 관리 (목록 조회 + 선택 삭제)
     * <p>
     * 동작 흐름:
     * 1) DataStore의 모든 사용자(users)를 불러와 ID 오름차순 정렬
     * 2) 사용자 정보를 콘솔에 출력 (id, 닉네임, 나이, 성별, 권한, 생성일, 신뢰도)
     * 3) 삭제할 사용자 ID 입력 (0 입력 시 취소)
     * 4) 검증 로직:
     * - 해당 ID의 사용자가 없으면 "존재하지 않음" 메시지 출력 후 종료
     * - 관리자 계정(Role.ADMIN)은 삭제 금지
     * 5) 정합성 유지:
     * - 해당 사용자가 작성한 게시글(Post)들을 모두 `markAsDeleted()` 처리
     * - DataStore.users 맵에서 제거
     * - 주민등록번호 중복 체크(rrnSet)에서도 제거
     * 6) DataStore.saveToDisk() 호출로 변경사항 영구 저장
     */
    public void manageUsers() {
        // 사용자 목록을 ID 기준 오름차순으로 정렬
        List<User> userList = new ArrayList<>(store.users().values());
        SortUtil.sortUsersById(userList);

        // 사용자 요약 정보 출력
        System.out.println("====== 사용자 목록 ======");
        for (User user : userList) {
            System.out.printf(
                    "아이디: %s | 닉네임: %s | 나이: %d | 성별: %s | 권한: %s | 생성일: %s | 신뢰도: G%d/B%d%n",
                    user.getId(), user.getNickname(), user.getAge(), user.getGender(),
                    user.getRole().getLabel(), user.getCreatedAt(),
                    user.getTrustGood(), user.getTrustBad()
            );
        }
        System.out.println("========================");

        // 삭제 대상 사용자 ID 입력
        String targetUserId = InputUtil.readNonEmptyLine("삭제할 사용자 ID(0=취소): ");
        if ("0".equals(targetUserId)) return;

        // 존재 여부 확인
        User targetUser = store.users().get(targetUserId);
        if (targetUser == null) {
            System.out.println("해당 사용자가 없습니다.");
            return;
        }

        // 관리자 계정 보호
        if (targetUser.getRole() == Role.ADMIN) {
            System.out.println("관리자 계정은 삭제할 수 없습니다.");
            return;
        }

        // 해당 사용자가 작성한 게시글도 논리 삭제
        for (Post post : store.posts().values()) {
            if (!post.isDeleted() && post.getSellerId().equals(targetUserId)) {
                post.markAsDeleted();
            }
        }

        // 사용자 삭제 및 주민번호 인덱스에서 제거
        store.users().remove(targetUserId);
        store.rrnSet().remove(targetUser.getRrn());

        // 변경사항 저장
        store.saveToDisk();
        System.out.println("삭제 완료");
    }

    // ===================== 게시글 관리 =====================

    /**
     * 게시글 관리 (목록 조회 + 선택 삭제)
     * <p>
     * 동작 흐름:
     * 1) DataStore의 posts 중 deleted=false 인 글만 모아 ID 오름차순 정렬
     * 2) 게시글 목록 출력 (Post.toString() 사용)
     * 3) 삭제할 게시글 ID 입력 (0 입력 시 취소)
     * 4) 검증 로직:
     * - 게시글이 존재하지 않거나 이미 삭제 상태이면 안내 후 종료
     * 5) 게시글의 markAsDeleted() 호출 (Soft Delete)
     * 6) DataStore.saveToDisk() 호출로 변경사항 저장
     */
    public void managePosts() {
        // 삭제되지 않은 게시글만 수집
        List<Post> postList = new ArrayList<>();
        for (Post post : store.posts().values()) {
            if (!post.isDeleted()) {
                postList.add(post);
            }
        }
        SortUtil.sortPostsById(postList);

        // 게시글 목록 출력
        System.out.println("====== 게시글 목록 ======");
        for (Post post : postList) {
            System.out.println(post);
        }
        System.out.println("========================");

        // 삭제 대상 게시글 입력
        int targetPostId = InputUtil.readInt("삭제할 게시글 번호(0=취소): ");
        if (targetPostId == 0) return;

        // 대상 게시글 조회
        Post targetPost = store.posts().get(targetPostId);
        if (targetPost == null || targetPost.isDeleted()) {
            System.out.println("해당 게시글이 없습니다.");
            return;
        }

        // 논리 삭제 후 저장
        targetPost.markAsDeleted();
        store.saveToDisk();
        System.out.println("삭제 완료");
    }
}