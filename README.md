# 🖥️ 중고거래 콘솔 애플리케이션

> Java 기반 **콘솔 환경에서 동작하는 중고거래 플랫폼**  
> GUI 없이 **터미널 입력/출력**만으로 회원 관리, 상품 등록, 검색, 거래, 알림 기능을 제공합니다.

---

## ✨ 주요 기능
- **회원 관리**
    - 회원가입 / 로그인
    - 역할(Role: `MEMBER`, `ADMIN`) 기반 권한
    - 비밀번호 해시 + Salt 저장 (보안 강화)

- **게시글 관리**
    - 상품 등록 (카테고리, 가격, 설명, 상태)
    - 게시글 검색 및 정렬 (최신순, 가격순)
    - 금칙어 필터링 (`ProfanityFilter`)

- **거래 관리**
    - 거래 상태(`TradeStatus`: 대기중, 진행중, 완료) 변경
    - 카테고리별 필터링 지원

- **알림(Notification)**
    - 거래 요청/수락/거절 알림
    - 사용자 맞춤형 알림 리스트 출력

- **신뢰도 시스템**
    - 좋은 평가/나쁜 평가 기록
    - 신뢰도 점수 계산

- **데이터 저장**
    - `DataStore` 직렬화를 통한 **스냅샷 저장/복원**
    - 별도 DB 없이 파일 기반 저장

---

## 🛠 기술 스택
- **Language**: Java 21
- **Build Tool**: Maven
- **IDE**: IntelliJ IDEA
- **Library**: Lombok

---

## 📂 프로젝트 구조
src
└── main
└── java
└── org.example
├── model # 도메인 모델 (User, Post, Notification 등)
├── service # 비즈니스 로직
├── util # 유틸 클래스 (RegexUtil, PriceUtil, ProfanityFilter 등)
└── datastore # DataStore (스냅샷 저장/복원)

yaml
코드 복사

---

## 🚀 실행 방법
1. 저장소 클론
   ```bash
   git clone https://github.com/your-repo.git
   cd your-repo
Maven 빌드

bash
코드 복사
mvn clean install
실행 (콘솔 환경)

bash
코드 복사
java -jar target/UsedMarketApp.jar
📖 콘솔 사용 예시
메인 메뉴
text
코드 복사
=== 중고거래 시스템 ===
1. 로그인
2. 회원가입
3. 게시글 등록
4. 게시글 검색
5. 알림 확인
0. 종료
------------------------
메뉴를 선택하세요:
게시글 검색
text
코드 복사
[게시글 검색 결과]
페이지 1 / 3 (총 24건)
1. [상의] 나이키 반팔티 - 10,000원
2. [신발] 아디다스 운동화 - 30,000원
   ...
> n (다음 페이지) | p (이전 페이지) | 0 (메인 메뉴)
📸 실행 화면 (샘플)
메인 메뉴	게시글 검색

📌 docs/images/ 폴더에 실행 화면 캡처 이미지를 저장하고 위와 같이 참조하면 GitHub에서 표시됩니다.
(예: docs/images/console-main.png)

⚙️ 설계 포인트
순수 콘솔 기반: GUI 없이 System.in/out 기반 인터페이스

Enum 라벨 매핑: Role, TradeStatus, NotificationType → 한글 라벨 지원

보안성 강화: SHA-256 해시 + Salt 기반 비밀번호 관리

데이터 안정성: 직렬화 기반 스냅샷 저장/복원

UX 개선: 페이지네이션, 첫/마지막 페이지 안내 메시지

📌 향후 개선 계획
GUI 환경으로 확장 (JavaFX, Swing)

데이터베이스 연동 (현재는 직렬화 기반 저장)

OAuth2 로그인 (Google, Naver 등)