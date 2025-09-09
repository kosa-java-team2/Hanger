package org.example.util;

import java.util.Scanner;

/**
 * InputUtil
 * -------------------
 * 콘솔 입력을 다루는 유틸리티 클래스.
 * <p>
 * 특징/설계 노트:
 * - 클래스는 유틸리티 성격이므로 final 이며, 인스턴스화를 막기 위해 private 생성자를 가진다.
 * - 단일 Scanner 인스턴스(SC)를 사용한다. (System.in은 전역 표준 입력이므로 Scanner를 여러 번
 *   생성/close하는 것은 권장되지 않음. 이 클래스에서는 SC를 close하지 않는다.)
 * - 모든 입력은 기본적으로 개행(Enter) 기반이며, 공백/형식 검증 루프를 통해 올바른 값이 들어올 때까지 요청한다.
 * <p>
 * 주의:
 * - readPasswordTwice는 콘솔에서 입력이 그대로 echo(표시)된다.
 *   운영 환경에서는 java.io.Console#readPassword 같은 no-echo 입력으로 대체하는 것을 권장한다.
 * - EOF(입력 스트림 종료) 상황에서는 Scanner가 예외를 던질 수 있으니, 실제 서비스에서는 예외 처리/종료 절차를
 *   별도로 마련하는 것이 좋다.
 */
public final class InputUtil {   // final: 상속 방지
    /** 전역에서 공유하는 표준 입력 Scanner. 닫지 않는다(close 금지). */
    private static final Scanner SCANNER = new Scanner(System.in);

    // 🔒 인스턴스화를 막는 private 생성자
    private InputUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ===================== 기본 입력 =====================

    public static String readLine() {
        return SCANNER.nextLine();
    }

    public static String readNonEmptyLine() {
        String inputLine;
        while (true) {
            inputLine = SCANNER.nextLine();
            if (inputLine != null && !inputLine.trim().isEmpty()) return inputLine.trim();
            System.out.print("비어있을 수 없습니다. 다시 입력: ");
        }
    }

    public static String readNonEmptyLine(String prompt) {
        System.out.print(prompt);
        return readNonEmptyLine();
    }

    // ===================== 정수 입력 =====================

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (Exception e) {
                System.out.println("숫자를 입력하세요.");
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value < min || value > max) {
                System.out.println(min + "~" + max + " 범위의 숫자를 입력하세요.");
            } else return value;
        }
    }

    // ===================== 가격 입력 =====================

    public static Integer readPriceAsInt(String prompt) {
        System.out.print(prompt);
        String raw = readNonEmptyLine();
        if (!RegexUtil.isValidPriceWithCommaOrPlain(raw)) return null;
        String normalized = raw.replace(",", "");
        try {
            return Integer.parseInt(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    // ===================== 비밀번호 입력(2회 확인) =====================

    /**
     * @param firstPrompt  1차 입력 프롬프트
     * @param secondPrompt 2차 입력 프롬프트
     */
    public static String readPasswordTwice(String firstPrompt, String secondPrompt) {
        while (true) {
            System.out.print(firstPrompt);
            String firstPassword = readNonEmptyLine();
            System.out.print(secondPrompt);
            String secondPassword = readNonEmptyLine();
            if (!firstPassword.equals(secondPassword)) {
                System.out.println("비밀번호가 일치하지 않습니다.");
                continue;
            }
            return firstPassword;
        }
    }
}