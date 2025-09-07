package org.example.datastore;

import org.example.model.*;

import java.io.*;
import java.util.*;

/**
 * DataStore 클래스
 * -----------------
 * 애플리케이션 내 주요 도메인 객체(User, Post, Trade, Notification, Report 등)를
 * 메모리에 저장하고, 직렬화를 통해 디스크에 저장/로드하는 역할을 담당한다.
 * <p>
 * 주요 특징:
 * - 고유 ID 생성을 위한 시퀀스 관리
 * - 중복 방지를 위한 주민번호(rrn) Set 관리
 * - 저장/로드 시 Snapshot 내부 클래스 사용 (직렬화 대상 일원화)
 * - Null-safe 유틸리티 메서드 제공
 */
public class DataStore {
    private static final String DATA_FILE = "store.dat"; // 데이터 저장 파일명

    // ===================== 기본 시퀀스 상수 =====================
    // 각 도메인 엔티티(Post, Trade, Notification, Report)의 고유 ID 생성을 위한 시작값
    private static final int DEFAULT_POST_SEQ = 1000;
    private static final int DEFAULT_TRADE_SEQ = 2000;
    private static final int DEFAULT_NOTIFICATION_SEQ = 3000;
    private static final int DEFAULT_REPORT_SEQ = 4000;

    // ===================== 저장 대상 컬렉션 =====================
    // 사용자 ID(User.id) -> User 객체
    private Map<String, User> users = new HashMap<>();

    // 게시글 번호(postId) -> Post 객체
    private Map<Integer, Post> posts = new HashMap<>();

    // 거래 번호(tradeId) -> Trade 객체
    private Map<Integer, Trade> trades = new HashMap<>();

    // 알림 번호(notificationId) -> Notification 객체
    private Map<Integer, Notification> notifications = new HashMap<>();

    // 신고 번호(reportId) -> Report 객체
    private Map<Integer, Report> reports = new HashMap<>();

    // 주민등록번호 중복 체크용 (회원가입 시 동일 RRN 방지)
    private Set<String> rrnSet = new HashSet<>();

    // ===================== 시퀀스 관리 변수 =====================
    // 다음에 생성될 ID를 위한 시퀀스 (저장/로드 시 지속적으로 이어져야 함)
    private int postSeq = DEFAULT_POST_SEQ;
    private int tradeSeq = DEFAULT_TRADE_SEQ;
    private int notificationSeq = DEFAULT_NOTIFICATION_SEQ;
    private int reportSeq = DEFAULT_REPORT_SEQ;

    // ===================== 내부 스냅샷 클래스 =====================
    /**
     * Snapshot 클래스
     * -----------------
     * DataStore의 현재 상태를 직렬화 가능한 형태로 묶어 저장하기 위한 내부 클래스.
     * (직렬화 대상이 많을 경우, 각 Map/Set을 개별적으로 저장하는 것보다 관리 용이)
     */
    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, User> users;
        Map<Integer, Post> posts;
        Map<Integer, Trade> trades;
        Map<Integer, Notification> notifications;
        Map<Integer, Report> reports;
        Set<String> rrnSet;
        int postSeq;
        int tradeSeq;
        int notificationSeq;
        int reportSeq;
    }

    // ===================== 데이터 로드 =====================
    /**
     * store.dat 파일에서 저장된 데이터를 읽어와 메모리로 적재한다.
     * - 파일이 없으면 아무 작업도 하지 않는다.
     * - Snapshot 객체를 역직렬화하여 현재 DataStore에 적용한다.
     */
    public void loadAll() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return; // 첫 실행 등 저장 파일이 없을 때

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Snapshot s = readSnapshot(ois);       // Snapshot 객체 읽기
            applySnapshotOrDefaults(s);           // Snapshot이 없으면 기본값으로 초기화
            logLoadSummary();                     // 요약 로그 출력
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("데이터 로드 실패: " + e.getMessage());
        }
    }

    // ===================== 데이터 저장 =====================
    /**
     * 현재 메모리에 적재된 데이터를 Snapshot 형태로 직렬화하여 store.dat 파일에 저장한다.
     */
    public void saveAll() {
        Snapshot s = toSnapshot(); // 현재 상태를 Snapshot 객체로 변환
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(s);
            System.out.println("💾 데이터 저장 완료.");
        } catch (IOException e) {
            System.out.println("데이터 저장 실패: " + e.getMessage());
        }
    }

    // ===================== 보조 메서드 =====================

    /**
     * ObjectInputStream에서 Snapshot 객체를 안전하게 읽어오기
     */
    private Snapshot readSnapshot(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Object obj = ois.readObject();
        return (obj instanceof Snapshot snapshot) ? snapshot : null;
    }

    /**
     * Snapshot이 null일 경우 기본값으로 초기화,
     * null이 아닌 경우는 Snapshot 데이터를 현재 객체에 적용
     */
    private void applySnapshotOrDefaults(Snapshot s) {
        if (s == null) {
            resetToDefaults();
            return;
        }
        this.users = nvlMap(s.users);
        this.posts = nvlMap(s.posts);
        this.trades = nvlMap(s.trades);
        this.notifications = nvlMap(s.notifications);
        this.reports = nvlMap(s.reports);
        this.rrnSet = nvlSet(s.rrnSet);

        this.postSeq = nzOrDefault(s.postSeq, DEFAULT_POST_SEQ);
        this.tradeSeq = nzOrDefault(s.tradeSeq, DEFAULT_TRADE_SEQ);
        this.notificationSeq = nzOrDefault(s.notificationSeq, DEFAULT_NOTIFICATION_SEQ);
        this.reportSeq = nzOrDefault(s.reportSeq, DEFAULT_REPORT_SEQ);
    }

    /**
     * Snapshot이 없을 경우(즉, 최초 실행) 빈 Map/Set과 기본 시퀀스 값으로 초기화
     */
    private void resetToDefaults() {
        this.users = new HashMap<>();
        this.posts = new HashMap<>();
        this.trades = new HashMap<>();
        this.notifications = new HashMap<>();
        this.reports = new HashMap<>();
        this.rrnSet = new HashSet<>();

        this.postSeq = DEFAULT_POST_SEQ;
        this.tradeSeq = DEFAULT_TRADE_SEQ;
        this.notificationSeq = DEFAULT_NOTIFICATION_SEQ;
        this.reportSeq = DEFAULT_REPORT_SEQ;
    }

    /**
     * 로드 완료 후 데이터 건수를 간략하게 로그 출력
     */
    private void logLoadSummary() {
        System.out.println("📦 데이터 로드 완료. (users=" + users.size() + ", posts=" + posts.size() + ")");
    }

    /**
     * 현재 DataStore 상태를 Snapshot 객체로 변환 (저장용)
     */
    private Snapshot toSnapshot() {
        Snapshot s = new Snapshot();
        s.users = this.users;
        s.posts = this.posts;
        s.trades = this.trades;
        s.notifications = this.notifications;
        s.reports = this.reports;
        s.rrnSet = this.rrnSet;
        s.postSeq = this.postSeq;
        s.tradeSeq = this.tradeSeq;
        s.notificationSeq = this.notificationSeq;
        s.reportSeq = this.reportSeq;
        return s;
    }

    // ===================== Null-safe Helper =====================
    private <K, V> Map<K, V> nvlMap(Map<K, V> m) {
        return (m != null) ? m : new HashMap<>();
    }

    private <T> Set<T> nvlSet(Set<T> s) {
        return (s != null) ? s : new HashSet<>();
    }

    private int nzOrDefault(int val, int defVal) {
        return (val != 0) ? val : defVal;
    }

    // ===================== Getter =====================
    public Map<String, User> users() { return users; }
    public Map<Integer, Post> posts() { return posts; }
    public Map<Integer, Trade> trades() { return trades; }
    public Map<Integer, Notification> notifications() { return notifications; }
    public Map<Integer, Report> reports() { return reports; }
    public Set<String> rrnSet() { return rrnSet; }

    // ===================== ID 시퀀스 메서드 =====================
    // synchronized → 멀티스레드 환경에서 동시 접근 시 ID 중복 방지
    public synchronized int nextPostId() { return ++postSeq; }
    public synchronized int nextTradeId() { return ++tradeSeq; }
    public synchronized int nextNotificationId() { return ++notificationSeq; }
    public synchronized int nextReportId() { return ++reportSeq; }
}