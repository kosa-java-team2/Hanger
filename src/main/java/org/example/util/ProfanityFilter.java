package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ProfanityFilter
 * --------------------------
 * 금칙어(비속어, 욕설 등) 감지를 위한 유틸리티 클래스.
 * <p>
 * 주요 기능:
 * - 리소스 파일("fword_list.txt")에서 금칙어 목록을 로드
 * - 입력 문자열이 금칙어를 포함하는지 검사
 * <p>
 * 설계 특징:
 * - 금칙어는 HashSet<String>에 보관 → 중복 제거 + 빠른 탐색
 * - 클래스 로딩 시(static block) 자동으로 금칙어 파일을 읽어 메모리에 적재
 * - 대소문자 구분 없이 검사 수행
 */
public class ProfanityFilter {
    /**
     * 금칙어 집합(Set)
     * - HashSet 사용: O(1)에 가까운 탐색 속도
     * - 중복 단어 자동 제거
     */
    private static final Set<String> bannedWords = new HashSet<>();

    /*
      정적 초기화 블록
      - 클래스가 JVM에 로드될 때 한 번 실행

      처리 과정:
      1) classpath에서 "fword_list.txt" 파일을 UTF-8 인코딩으로 읽음
      2) 각 줄을 trim() 처리 후, 비어있지 않으면 bannedWords에 추가
      3) 파일 없음/읽기 실패 시 → 오류 메시지 출력 (System.err)

      예외 처리:
      - IOException: 파일 읽기 오류
      - NullPointerException: 리소스 파일 자체가 존재하지 않는 경우
     */
    static {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                ProfanityFilter.class.getClassLoader().getResourceAsStream("fword_list.txt")
                        ),
                        StandardCharsets.UTF_8
                )
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    bannedWords.add(line);
                }
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("금칙어 리스트 로드 실패: " + e.getMessage());
        }
    }

    /**
     * 입력 문자열이 금칙어를 포함하는지 확인
     * <p>
     * 처리 로직:
     * - null 입력 시 → false 반환 (안전 처리)
     * - 입력 문자열과 금칙어 모두 소문자로 변환 후 부분 문자열 비교
     * - 하나라도 포함되어 있으면 즉시 true 반환
     * - 끝까지 검사했는데 없으면 false
     *
     * @param text 검사할 문자열
     * @return 금칙어 포함 시 true, 없으면 false
     */
    public static boolean containsBannedWord(String text) {
        if (text == null) return false;

        String lower = text.toLowerCase();
        for (String banned : bannedWords) {
            if (lower.contains(banned.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}