package org.example.service;

import org.example.model.Post;
import org.example.model.Role;
import org.example.model.User;
import org.example.util.InputUtil;
import org.example.datastore.DataStore;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminService
 * -----------------
 * 관리자 전용 기능을 제공하는 서비스 레이어.
 * 현재 제공 기능:
 *  - 사용자 목록 조회 및 삭제(관리자 계정은 삭제 불가)
 *  - 게시글 목록 조회 및 삭제(논리 삭제)
 *
 * 설계 노트:
 * - 영속 계층(DataStore)을 주입받아 사용한다(생성자 주입).
 * - 출력/입력(콘솔 I/O)은 간단한 CLI용 유틸(InputUtil)을 사용한다.
 * - 삭제 시, 사용자 삭제는 해당 사용자가 작성한 게시글도 함께 "삭제 마킹"한다(정합성 유지).
 * - 게시글 삭제는 물리 삭제가 아닌 논리 삭제(soft delete)를 사용한다.
 */
public class AdminService {
    private final DataStore store;

    /**
     * DataStore를 주입받는 생성자.
     * @param store 애플리케이션 전역 데이터 저장/로드를 담당하는 저장소
     */
    public AdminService(DataStore store) {
        this.store = store;
    }

    // ===================== 사용자 관리 =====================

    /**
     * 사용자 관리: 목록 조회 → 선택 삭제
     *
     * 흐름:
     *  1) 모든 사용자(users) 정렬(id 오름차순) 후 콘솔에 요약 정보 출력
     *  2) 삭제 대상 사용자 ID 입력받기(0 입력 시 취소)
     *  3) 유효성 검사:
     *     - 존재하지 않는 사용자 ID → 안내 후 종료
     *     - 관리자(ADMIN) 권한 계정 → 삭제 금지
     *  4) 부수 효과(정합성 유지):
     *     - 해당 사용자가 작성한 게시글 중, 아직 삭제되지 않은 글 모두 markDeleted()
     *     - users 맵에서 사용자 제거
     *     - rrnSet(주민번호 중복 체크)에서도 해당 사용자의 rrn 제거
     *  5) store.saveAll() 호출로 현재 상태 스냅샷 저장
     */
    public void manageUsers() {
        // 사용자 목록을 ID 기준 오름차순으로 정렬
        List<User> list = store.users().values().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();

        // 사용자 요약 정보 출력
        System.out.println("====== 사용자 목록 ======");
        for (User u : list) {
            System.out.printf(
                    "아이디: %s | 닉네임: %s | 나이: %d | 성별: %s | 권한: %s | 생성일: %s | 신뢰도: G%d/B%d%n",
                    u.getId(), u.getNickname(), u.getAge(), u.getGender(), u.getRole(),
                    u.getCreatedAt(), u.getTrustGood(), u.getTrustBad()
            );
        }
        System.out.println("========================");

        // 삭제 대상 사용자 ID 입력(0 = 취소)
        String id = InputUtil.readNonEmptyLine("삭제할 사용자 ID(0=취소): ");
        if ("0".equals(id)) return;

        // 존재 여부 확인
        User u = store.users().get(id);
        if (u == null) {
            System.out.println("해당 사용자가 없습니다.");
            return;
        }

        // 관리자 계정 보호
        if (u.getRole() == Role.ADMIN) {
            System.out.println("관리자 계정은 삭제할 수 없습니다.");
            return;
        }

        // 해당 사용자가 작성한 게시글도 함께 논리 삭제(삭제 마킹)
        store.posts().values().stream()
                .filter(p -> !p.isDeleted() && p.getSellerId().equals(id))
                .forEach(Post::markDeleted);

        // 사용자 삭제 및 주민번호 인덱스에서 제거
        store.users().remove(id);
        store.rrnSet().remove(u.getRrn());

        // 현재 상태 저장
        store.saveAll();
        System.out.println("삭제 완료");
    }

    // ===================== 게시글 관리 =====================

    /**
     * 게시글 관리: 목록 조회(삭제되지 않은 글) → 선택 삭제(논리 삭제)
     *
     * 흐름:
     *  1) posts에서 isDeleted() == false 인 글만 필터링하여 postId 오름차순으로 출력
     *  2) 삭제 대상 게시글 번호 입력받기(0 입력 시 취소)
     *  3) 유효성 검사:
     *     - 존재하지 않거나 이미 삭제된 게시글 → 안내 후 종료
     *  4) 게시글에 markDeleted() 호출하여 논리 삭제
     *  5) store.saveAll()로 저장
     */
    public void managePosts() {
        // 삭제되지 않은 게시글만 postId 오름차순으로 정렬
        List<Post> list = store.posts().values().stream()
                .filter(p -> !p.isDeleted())
                .sorted(Comparator.comparing(Post::getPostId))
                .toList();

        // 게시글 목록 출력 (Post.toString() 사용)
        System.out.println("====== 게시글 목록 ======");
        list.forEach(System.out::println);
        System.out.println("========================");

        // 삭제 대상 게시글 번호 입력(0 = 취소)
        int pid = InputUtil.readInt("삭제할 게시글 번호(0=취소): ");
        if (pid == 0) return;

        // 대상 게시글 조회
        Post p = store.posts().get(pid);
        if (p == null || p.isDeleted()) {
            System.out.println("해당 게시글이 없습니다.");
            return;
        }

        // 논리 삭제 후 저장
        p.markDeleted();
        store.saveAll();
        System.out.println("삭제 완료");
    }
}