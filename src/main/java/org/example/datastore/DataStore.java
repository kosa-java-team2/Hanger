package org.example.datastore;

import org.example.model.Post;
import org.example.model.User;

import java.io.*;
import java.util.*;

public class DataStore {
    private static final String DATA_FILE = "store.dat";

    // 저장 대상 컬렉션
    private Map<String, User> users = new HashMap<>();   // id -> User
    private Map<Integer, Post> posts = new HashMap<>();  // postId -> Post
    private Set<String> rrnSet = new HashSet<>();        // 주민번호 중복 체크
    private int postSeq = 1000;                          // 게시글 번호 시퀀스

    // 직렬화 스냅샷
    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, User> users;
        Map<Integer, Post> posts;
        Set<String> rrnSet;
        int postSeq;
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
                this.rrnSet = s.rrnSet != null ? s.rrnSet : new HashSet<>();
                this.postSeq = s.postSeq != 0 ? s.postSeq : 1000;
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
        s.rrnSet = this.rrnSet;
        s.postSeq = this.postSeq;
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
    public Set<String> rrnSet() { return rrnSet; }

    public synchronized int nextPostId() {
        return ++postSeq;
    }
}
