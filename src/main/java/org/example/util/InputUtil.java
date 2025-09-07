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
    private static final Scanner SC = new Scanner(System.in);

    // 🔒 인스턴스화를 막는 private 생성자
    private InputUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ===================== 기본 입력 =====================

    /**
     * 한 줄 입력(빈 문자열 허용).
     * @return 사용자가 입력한 원문(개행 제외). EOF 시 예외가 날 수 있음.
     */
    public static String readLine() {
        return SC.nextLine();
    }

    /**
     * 비어 있지 않은 한 줄 입력(트림 후 공백만 있는 입력은 거부).
     * 잘못된 입력 시 재입력을 요청한다.
     * @return 공백 제거(trim)된 비어 있지 않은 문자열
     */
    public static String readNonEmptyLine() {
        String s;
        while (true) {
            s = SC.nextLine(); // EOF 시 NoSuchElementException 가능
            if (s != null && !s.trim().isEmpty()) return s.trim();
            System.out.print("비어있을 수 없습니다. 다시 입력: ");
        }
    }

    /**
     * 프롬프트를 먼저 출력한 뒤, 비어 있지 않은 한 줄 입력을 받는다.
     * @param prompt 사용자에게 보여줄 안내 문구
     * @return 공백 제거(trim)된 비어 있지 않은 문자열
     */
    public static String readNonEmptyLine(String prompt) {
        System.out.print(prompt);
        return readNonEmptyLine();
    }

    // ===================== 정수 입력 =====================

    /**
     * 정수 입력을 받을 때까지 반복 요청한다.
     * (숫자 이외 입력 시 "숫자를 입력하세요." 안내 후 재시도)
     * @param prompt 안내 문구
     * @return 파싱된 int 값
     */
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

    /**
     * 지정한 범위 [min, max]에 속하는 정수를 입력받는다.
     * 범위를 벗어나면 재입력을 요청한다.
     * @param prompt 안내 문구
     * @param min 최소값(포함)
     * @param max 최대값(포함)
     * @return 범위 내 정수
     */
    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int v = readInt(prompt);
            if (v < min || v > max) {
                System.out.println(min + "~" + max + " 범위의 숫자를 입력하세요.");
            } else return v;
        }
    }

    // ===================== 가격 입력 =====================

    /**
     * 가격 입력을 정수로 받는다.
     * - "1000" 또는 "1,000" 형식 모두 허용.
     * - 유효성은 RegexUtil.isValidPriceWithCommaOrPlain 로 검사.
     * - 유효하지 않거나 파싱 실패 시 null 반환(호출부에서 오류 메시지 처리).
     * <p>
     * 주의:
     * - 금액 범위가 int를 넘어설 수 있다면 long/BigDecimal 사용을 고려한다.
     *
     * @param prompt 안내 문구
     * @return 파싱된 정수 가격 또는 null(형식/파싱 오류)
     */
    public static Integer readPriceAsInt(String prompt) {
        System.out.print(prompt);
        String s = readNonEmptyLine();
        if (!RegexUtil.isValidPriceWithCommaOrPlain(s)) return null;
        String normalized = s.replace(",", ""); // "1,000" → "1000"
        try {
            return Integer.parseInt(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    // ===================== 비밀번호 입력(2회 확인) =====================

    /**
     * 비밀번호를 2회 입력받아 일치할 때까지 반복한다.
     * <p>
     * 보안 노트:
     * - 이 메서드는 콘솔에 입력이 그대로 표시된다(에코 on). 운영 환경에서는 no-echo 입력(예: Console#readPassword)을 권장.
     * - 비밀번호 유효성(길이/문자 조합 등) 검사는 별도 정책에 따라 호출부나 Validator에서 수행할 것.
     * <p>
     * @param p1 1차 입력 프롬프트
     * @param p2 2차 입력 프롬프트
     * @return 최종 일치한 비밀번호 원문(호출부에서 즉시 해시/폐기 권장)
     */
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