package org.example.datastore;

import org.example.model.*;

import java.io.*;
import java.util.*;

public class DataStore {
    private static final String DATA_FILE = "store.dat";

    // 기본 시퀀스 상수
    private static final int DEFAULT_POST_SEQ = 1000;
    private static final int DEFAULT_TRADE_SEQ = 2000;
    private static final int DEFAULT_NOTIFICATION_SEQ = 3000;
    private static final int DEFAULT_REPORT_SEQ = 4000;

    // 저장 대상 컬렉션
    private Map<String, User> users = new HashMap<>();        // id -> User
    private Map<Integer, Post> posts = new HashMap<>();        // postId -> Post
    private Map<Integer, Trade> trades = new HashMap<>();      // tradeId -> Trade
    private Map<Integer, Notification> notifications = new HashMap<>(); // notificationId -> Notification
    private Map<Integer, Report> reports = new HashMap<>();    // reportId -> Report
    private Set<String> rrnSet = new HashSet<>();              // 주민번호 중복 체크

    // 시퀀스
    private int postSeq = DEFAULT_POST_SEQ;
    private int tradeSeq = DEFAULT_TRADE_SEQ;
    private int notificationSeq = DEFAULT_NOTIFICATION_SEQ;
    private int reportSeq = DEFAULT_REPORT_SEQ;

    // 직렬화 스냅샷
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

    // ===================== 리팩터링된 loadAll =====================
    public void loadAll() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Snapshot s = readSnapshot(ois);
            applySnapshotOrDefaults(s);
            logLoadSummary();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("데이터 로드 실패: " + e.getMessage());
        }
    }
    // ============================================================

    public void saveAll() {
        Snapshot s = toSnapshot();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(s);
            System.out.println("💾 데이터 저장 완료.");
        } catch (IOException e) {
            System.out.println("데이터 저장 실패: " + e.getMessage());
        }
    }

    // --- 보조 메서드들 (복잡도 분산) ---
    private Snapshot readSnapshot(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Object obj = ois.readObject();
        return (obj instanceof Snapshot snapshot) ? snapshot : null;
    }

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

    private void logLoadSummary() {
        System.out.println("📦 데이터 로드 완료. (users=" + users.size() + ", posts=" + posts.size() + ")");
    }

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

    private <K, V> Map<K, V> nvlMap(Map<K, V> m) {
        return (m != null) ? m : new HashMap<>();
    }

    private <T> Set<T> nvlSet(Set<T> s) {
        return (s != null) ? s : new HashSet<>();
    }

    private int nzOrDefault(int val, int defVal) {
        return (val != 0) ? val : defVal;
    }

    // getters
    public Map<String, User> users() { return users; }
    public Map<Integer, Post> posts() { return posts; }
    public Map<Integer, Trade> trades() { return trades; }
    public Map<Integer, Notification> notifications() { return notifications; }
    public Map<Integer, Report> reports() { return reports; }
    public Set<String> rrnSet() { return rrnSet; }

    public synchronized int nextPostId() { return ++postSeq; }
    public synchronized int nextTradeId() { return ++tradeSeq; }
    public synchronized int nextNotificationId() { return ++notificationSeq; }
    public synchronized int nextReportId() { return ++reportSeq; }
}
