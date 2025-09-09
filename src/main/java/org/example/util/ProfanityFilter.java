package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ProfanityFilter {
    private static final Set<String> bannedWords = new HashSet<>();

    static {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(ProfanityFilter.class.getClassLoader().getResourceAsStream("fword_list.txt")), StandardCharsets.UTF_8)
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