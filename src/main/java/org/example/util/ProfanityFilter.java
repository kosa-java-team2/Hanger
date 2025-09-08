package org.example.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ProfanityFilter {
    private static final Set<String> bannedWords = new HashSet<>();

    // 금칙어 파일 로드
    static {
        try (Stream<String> lines = Files.lines(Paths.get("fword_list.txt"))) {
            lines.map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .forEach(bannedWords::add);
        } catch (IOException e) {
            System.err.println("금칙어 리스트 로드 실패: " + e.getMessage());
        }
    }

    /** 문자열에 금칙어 포함 여부 확인 */
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