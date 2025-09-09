package org.example.util;

import java.util.Scanner;

/**
 * InputUtil
 * -------------------
 * 콘솔 입력을 다루는 유틸리티 클래스.
 * <p>
 * 특징 및 설계:
 * - final 클래스로 선언되어 상속 불가
 * - 인스턴스화를 막기 위해 private 생성자 사용 → 외부에서 new 불가
 * - 단일 Scanner 인스턴스(SCANNER)를 전역적으로 공유
 * (System.in은 애플리케이션 전체에서 공유되므로, Scanner를 여러 번 열고 닫으면 문제 발생)
 * - 모든 입력은 Enter(개행) 단위로 처리되며, 잘못된 값이 들어오면 유효할 때까지 반복 요청
 * <p>
 * 주의 사항:
 * - SCANNER는 close 하지 않는다 (System.in을 닫으면 전체 입력 스트림이 닫혀 더 이상 입력 불가)
 * - readPasswordTwice는 콘솔 입력이 그대로 화면에 출력(Echo ON)됨
 * → 운영 환경에서는 java.io.Console.readPassword 같은 no-echo 입력 방식 사용 권장
 * - EOF(입력 종료) 상황에서는 Scanner가 예외를 던질 수 있으므로,
 * 실제 서비스에서는 예외 처리 및 종료 절차를 별도로 마련해야 함
 */
public final class InputUtil {   // final: 상속 방지
    /**
     * 전역에서 공유하는 표준 입력 Scanner. close 금지
     */
    private static final Scanner SCANNER = new Scanner(System.in);

    // 🔒 인스턴스화를 막는 private 생성자
    private InputUtil() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    // ===================== 기본 입력 =====================

    /**
     * 단순히 한 줄 입력을 받아 반환
     *
     * @return 입력 문자열 (개행 제외, null 없음)
     */
    public static String readLine() {
        return SCANNER.nextLine();
    }

    /**
     * 공백 또는 빈 문자열을 허용하지 않는 입력
     * - null이거나 공백만 입력된 경우 다시 입력받음
     *
     * @return 공백이 아닌 문자열
     */
    public static String readNonEmptyLine() {
        String inputLine;
        while (true) {
            inputLine = SCANNER.nextLine();
            if (inputLine != null && !inputLine.trim().isEmpty()) return inputLine.trim();
            System.out.print("비어있을 수 없습니다. 다시 입력: ");
        }
    }

    /**
     * 프롬프트를 출력한 후, 공백이 아닌 문자열 입력을 받음
     *
     * @param prompt 출력 메시지
     * @return 공백이 아닌 문자열
     */
    public static String readNonEmptyLine(String prompt) {
        System.out.print(prompt);
        return readNonEmptyLine();
    }

    // ===================== 정수 입력 =====================

    /**
     * 정수를 입력받을 때까지 반복 요청
     * - 잘못된 입력이 들어오면 "숫자를 입력하세요." 출력 후 재입력
     *
     * @param prompt 안내 메시지
     * @return 입력된 정수
     */
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

    /**
     * 지정된 범위(min~max) 안의 정수만 허용
     * - 범위를 벗어나면 경고 메시지 출력 후 다시 입력
     *
     * @param prompt 안내 메시지
     * @param min    최소값
     * @param max    최대값
     * @return 범위 내 정수
     */
    public static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value < min || value > max) {
                System.out.println(min + "~" + max + " 범위의 숫자를 입력하세요.");
            } else return value;
        }
    }

    // ===================== 가격 입력 =====================

    /**
     * 가격 입력을 정수로 변환
     * - 허용 형식: "1000" 또는 "1,000"
     * - RegexUtil.isValidPriceWithCommaOrPlain() 으로 유효성 검증
     *
     * @param prompt 안내 메시지
     * @return 정상 입력 시 정수, 잘못된 형식이면 null
     */
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
     * 비밀번호를 2회 입력받아 일치 여부를 검증
     * - 두 입력이 다르면 재입력 요구
     * - 콘솔에서 입력이 그대로 보임(Echo ON)
     * → 실제 운영 환경에서는 echo 없는 입력 방식 사용 권장
     *
     * @param firstPrompt  1차 입력 프롬프트
     * @param secondPrompt 2차 입력 프롬프트
     * @return 두 입력이 일치하면 최종 비밀번호 문자열
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