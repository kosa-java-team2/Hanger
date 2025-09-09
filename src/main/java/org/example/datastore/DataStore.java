package org.example.datastore;

import org.example.model.*;

import java.io.*;
import java.util.*;

/**
 * DataStore 클래스
 * -----------------
 * 애플리케이션의 주요 도메인 객체(User, Post, Trade, Notification)를
 * 메모리에 보관하고, 이를 직렬화를 통해 디스크 파일(store.dat)에 저장/로드한다.
 * <p>
 * 주요 특징:
 * - 사용자(User), 게시글(Post), 거래(Trade), 알림(Notification) 데이터 관리
 * - ID 자동 증가 시퀀스(postSeq, tradeSeq, notificationSeq) 관리
 * - 주민등록번호(RRN) 중복 방지 (rrnSet 이용)
 * - Snapshot 내부 클래스로 전체 상태를 일괄 직렬화하여 저장/복원
 * - Null-safe 유틸리티 제공(Map/Set이 null일 경우 빈 객체 반환)
 */
public class DataStore {
    // ===================== 상수 정의 =====================
    /**
     * 데이터 직렬화가 저장될 파일 이름
     */
    private static final String DATA_FILE = "store.dat";

    /**
     * 게시글(Post) ID 시퀀스 기본 시작 값
     */
    private static final int DEFAULT_POST_SEQ = 1000;

    /**
     * 거래(Trade) ID 시퀀스 기본 시작 값
     */
    private static final int DEFAULT_TRADE_SEQ = 2000;

    /**
     * 알림(Notification) ID 시퀀스 기본 시작 값
     */
    private static final int DEFAULT_NOTIFICATION_SEQ = 3000;

    // ===================== 실제 데이터 저장 컬렉션 =====================
    /**
     * 사용자(User) 데이터 저장 (키: username 같은 고유 문자열, 값: User 객체)
     */
    private Map<String, User> users = new HashMap<>();

    /**
     * 게시글(Post) 데이터 저장 (키: postId, 값: Post 객체)
     */
    private Map<Integer, Post> posts = new HashMap<>();

    /**
     * 거래(Trade) 데이터 저장 (키: tradeId, 값: Trade 객체)
     */
    private Map<Integer, Trade> trades = new HashMap<>();

    /**
     * 알림(Notification) 데이터 저장 (키: notificationId, 값: Notification 객체)
     */
    private Map<Integer, Notification> notifications = new HashMap<>();

    /**
     * 주민등록번호 중복 체크용 Set (회원 가입 시 중복 방지)
     */
    private Set<String> rrnSet = new HashSet<>();

    // ===================== ID 시퀀스 관리 =====================
    /**
     * 게시글 ID 자동 증가 시퀀스
     */
    private int postSeq = DEFAULT_POST_SEQ;

    /**
     * 거래 ID 자동 증가 시퀀스
     */
    private int tradeSeq = DEFAULT_TRADE_SEQ;

    /**
     * 알림 ID 자동 증가 시퀀스
     */
    private int notificationSeq = DEFAULT_NOTIFICATION_SEQ;

    // ===================== Snapshot 내부 클래스 =====================

    /**
     * DataStore의 현재 상태를 직렬화하기 위한 내부 클래스.
     * - users, posts, trades, notifications, rrnSet, 시퀀스 값 등을 한 번에 저장한다.
     * - 직렬화 대상은 반드시 Serializable 구현 필요.
     */
    private static class Snapshot implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        Map<String, User> users;
        Map<Integer, Post> posts;
        Map<Integer, Trade> trades;
        Map<Integer, Notification> notifications;
        Set<String> rrnSet;
        int postSeq;
        int tradeSeq;
        int notificationSeq;
    }

    // ===================== 데이터 로드 =====================

    /**
     * store.dat 파일에서 Snapshot을 읽어와 메모리에 복원한다.
     * - 파일이 존재하지 않으면(최초 실행 시) 기본값으로 초기화한다.
     * - 복원된 Snapshot의 users, posts 등 컬렉션 및 시퀀스를 현재 DataStore에 적용한다.
     */
    public void loadFromDisk() {
        File dataFile = new File(DATA_FILE);
        if (!dataFile.exists()) return; // 데이터 파일이 없으면 초기 상태 유지

        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(dataFile))) {
            Snapshot snapshot = readSnapshotFromStream(input);
            applySnapshotOrInitializeDefaults(snapshot);
            printLoadSummary();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("데이터 로드 실패: " + e.getMessage());
        }
    }

    // ===================== 데이터 저장 =====================

    /**
     * 현재 메모리 데이터를 Snapshot 객체로 직렬화하여 store.dat 파일에 저장한다.
     * - saveToDisk 호출 시점의 데이터 상태가 그대로 보존된다.
     */
    public void saveToDisk() {
        Snapshot snapshot = createSnapshot();
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            output.writeObject(snapshot); // Snapshot 직렬화
            System.out.println("💾 데이터 저장 완료.");
        } catch (IOException e) {
            System.out.println("데이터 저장 실패: " + e.getMessage());
        }
    }

    // ===================== 보조 메서드 =====================

    /**
     * ObjectInputStream으로부터 Snapshot 객체를 읽는다.
     *
     * @return 읽은 객체가 Snapshot이면 반환, 아니면 null
     */
    private Snapshot readSnapshotFromStream(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        Object readObject = inputStream.readObject();
        return (readObject instanceof Snapshot snapshot) ? snapshot : null;
    }

    /**
     * 읽은 Snapshot 데이터를 현재 DataStore에 적용한다.
     * - snapshot이 null이면 초기 상태로 재설정한다.
     */
    private void applySnapshotOrInitializeDefaults(Snapshot snapshot) {
        if (snapshot == null) {
            initializeDefaults();
            return;
        }
        this.users = mapOrEmpty(snapshot.users);
        this.posts = mapOrEmpty(snapshot.posts);
        this.trades = mapOrEmpty(snapshot.trades);
        this.notifications = mapOrEmpty(snapshot.notifications);
        this.rrnSet = setOrEmpty(snapshot.rrnSet);

        // 시퀀스 값이 0일 경우 기본값 유지
        this.postSeq = valueOrDefaultIfZero(snapshot.postSeq, DEFAULT_POST_SEQ);
        this.tradeSeq = valueOrDefaultIfZero(snapshot.tradeSeq, DEFAULT_TRADE_SEQ);
        this.notificationSeq = valueOrDefaultIfZero(snapshot.notificationSeq, DEFAULT_NOTIFICATION_SEQ);
    }

    /**
     * 초기 상태로 데이터스토어를 설정한다.
     * (주로 store.dat이 없거나 Snapshot이 null일 경우)
     */
    private void initializeDefaults() {
        this.users = new HashMap<>();
        this.posts = new HashMap<>();
        this.trades = new HashMap<>();
        this.notifications = new HashMap<>();
        this.rrnSet = new HashSet<>();

        this.postSeq = DEFAULT_POST_SEQ;
        this.tradeSeq = DEFAULT_TRADE_SEQ;
        this.notificationSeq = DEFAULT_NOTIFICATION_SEQ;
    }

    /**
     * 데이터 로드 완료 후, 데이터 개수 요약 출력
     */
    private void printLoadSummary() {
        System.out.println("📦 데이터 로드 완료. (users=" + users.size() + ", posts=" + posts.size() + ")");
    }

    /**
     * 현재 메모리 상태를 Snapshot 객체로 변환한다.
     */
    private Snapshot createSnapshot() {
        Snapshot snapshot = new Snapshot();
        snapshot.users = this.users;
        snapshot.posts = this.posts;
        snapshot.trades = this.trades;
        snapshot.notifications = this.notifications;
        snapshot.rrnSet = this.rrnSet;
        snapshot.postSeq = this.postSeq;
        snapshot.tradeSeq = this.tradeSeq;
        snapshot.notificationSeq = this.notificationSeq;
        return snapshot;
    }

    // ===================== Null-safe Helper =====================

    /**
     * Map이 null이면 빈 HashMap을 반환한다.
     */
    private <K, V> Map<K, V> mapOrEmpty(Map<K, V> map) {
        return (map != null) ? map : new HashMap<>();
    }

    /**
     * Set이 null이면 빈 HashSet을 반환한다.
     */
    private <T> Set<T> setOrEmpty(Set<T> set) {
        return (set != null) ? set : new HashSet<>();
    }

    /**
     * 값이 0이면 기본값을 반환한다.
     */
    private int valueOrDefaultIfZero(int value, int defaultValue) {
        return (value != 0) ? value : defaultValue;
    }

    // ===================== Getter =====================

    /**
     * User Map 반환
     */
    public Map<String, User> users() {
        return users;
    }

    /**
     * Post Map 반환
     */
    public Map<Integer, Post> posts() {
        return posts;
    }

    /**
     * Trade Map 반환
     */
    public Map<Integer, Trade> trades() {
        return trades;
    }

    /**
     * Notification Map 반환
     */
    public Map<Integer, Notification> notifications() {
        return notifications;
    }

    /**
     * 주민등록번호 Set 반환
     */
    public Set<String> rrnSet() {
        return rrnSet;
    }

    // ===================== 시퀀스 메서드 =====================

    /**
     * 다음 게시글 ID 발급
     */
    public int nextPostId() {
        return ++postSeq;
    }

    /**
     * 다음 거래 ID 발급
     */
    public int nextTradeId() {
        return ++tradeSeq;
    }

    /**
     * 다음 알림 ID 발급
     */
    public int nextNotificationId() {
        return ++notificationSeq;
    }
}