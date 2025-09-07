package org.example.util;

import java.util.Scanner;

public final class InputUtil {   // final: 상속 방지
    private static final Scanner SC = new Scanner(System.in);

    // 🔒 인스턴스화를 막는 private 생성자
    private InputUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    public static String readLine() {
        return SC.nextLine();
    }

    public static String readNonEmptyLine() {
        String s;
        while (true) {
            s = SC.nextLine();
            if (s != null && !s.trim().isEmpty()) return s.trim();
            System.out.print("비어있을 수 없습니다. 다시 입력: ");
        }
    }

    public static String readNonEmptyLine(String prompt) {
        System.out.print(prompt);
        return readNonEmptyLine();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine();
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception e) {
                System.out.println("숫자를 입력하세요.");
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int v = readInt(prompt);
            if (v < min || v > max) {
                System.out.println(min + "~" + max + " 범위의 숫자를 입력하세요.");
            } else return v;
        }
    }

    // 1,000 형식 또는 숫자 허용 → int
    public static Integer readPriceAsInt(String prompt) {
        System.out.print(prompt);
        String s = readNonEmptyLine();
        if (!RegexUtil.isValidPriceWithCommaOrPlain(s)) return null;
        String normalized = s.replace(",", "");
        try {
            return Integer.parseInt(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    // 비밀번호 2회 확인
    public static String readPasswordTwice(String p1, String p2) {
        while (true) {
            System.out.print(p1);
            String a = readNonEmptyLine();
            System.out.print(p2);
            String b = readNonEmptyLine();
            if (!a.equals(b)) {
                System.out.println("비밀번호가 일치하지 않습니다.");
                continue;
            }
            return a;
        }
    }
}