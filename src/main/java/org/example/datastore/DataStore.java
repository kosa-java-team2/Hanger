package org.example.datastore;

import org.example.model.*;

import java.io.*;
import java.util.*;

public class DataStore {
    private static final String DATA_FILE = "store.dat";

    // 저장 대상 컬렉션
    private Map<String, User> users = new HashMap<>();   // id -> User
    private Map<Integer, Post> posts = new HashMap<>();  // postId -> Post
    private Map<Integer, Trade> trades = new HashMap<>(); // tradeId -> Trade
    private Map<Integer, Notification> notifications = new HashMap<>(); // notificationId -> Notification
    private Map<Integer, Report> reports = new HashMap<>(); // reportId -> Report
    private Set<String> rrnSet = new HashSet<>();        // 주민번호 중복 체크
    private int postSeq = 1000;                          // 게시글 번호 시퀀스

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

    public void loadAll() {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Snapshot s = (Snapshot) ois.readObject();
            if (s != null) {
                this.users = s.users != null ? s.users : new HashMap<>();
                this.posts = s.posts != null ? s.posts : new HashMap<>();
                this.trades = s.trades != null ? s.trades : new HashMap<>();
                this.notifications = s.notifications != null ? s.notifications : new HashMap<>();
                this.reports = s.reports != null ? s.reports : new HashMap<>();
                this.rrnSet = s.rrnSet != null ? s.rrnSet : new HashSet<>();
                this.postSeq = s.postSeq != 0 ? s.postSeq : 1000;
                this.tradeSeq = s.tradeSeq != 0 ? s.tradeSeq : 2000;
                this.notificationSeq = s.notificationSeq != 0 ? s.notificationSeq : 3000;
                this.reportSeq = s.reportSeq != 0 ? s.reportSeq : 4000;
            }
            System.out.println("📦 데이터 로드 완료. (users=" + users.size() + ", posts=" + posts.size() + ")");
        } catch (Exception e) {
            System.out.println("데이터 로드 실패: " + e.getMessage());
        }
    }

    public void saveAll() {
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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(s);
            System.out.println("💾 데이터 저장 완료.");
        } catch (IOException e) {
            System.out.println("데이터 저장 실패: " + e.getMessage());
        }
    }

    // getters
    public Map<String, User> users() { return users; }
    public Map<Integer, Post> posts() { return posts; }
    public Map<Integer, Trade> trades() { return trades; }
    public Map<Integer, Notification> notifications() { return notifications; }
    public Map<Integer, Report> reports() { return reports; }
    public Set<String> rrnSet() { return rrnSet; }

    public synchronized int nextPostId() {
        return ++postSeq;
    }

    private int tradeSeq = 2000;
    private int notificationSeq = 3000;
    private int reportSeq = 4000;

    public synchronized int nextTradeId() { return ++tradeSeq; }
    public synchronized int nextNotificationId() { return ++notificationSeq; }
    public synchronized int nextReportId() { return ++reportSeq; }
}
