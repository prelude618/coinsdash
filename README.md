# CoinSDash

CoinSDash는 AWS Lightsail에서 실행 중인 CoinSDance 자동매매 봇의 상태를 Android에서 확인하기 위한 대시보드 앱입니다.

## 현재 상태

Jetpack Compose 대시보드와 CoinSDance HTTPS API 클라이언트가 구현되어 있습니다. 실제 사용 전 CoinSDance 서버에 대시보드 API와 HTTPS 진입점을 배포하고 앱 설정에 서버 주소와 인증 토큰을 입력해야 합니다.

## 예정 기능

- CoinSDance 프로세스 생존 여부, 마지막 정상 동기화 시각과 장애 표시
- 전체 평가자본, 사용 가능 KRW와 보유 종목 수
- 신규 매수 활성 종목과 종목별 배정액
- 매수 저점 추적 및 매도 고점 추적 종목 수
- 미체결 매수·매도 주문 수
- 최근 매수·매도 체결 내역
- 유의종목, Coinbase 등록 해제, API 및 주문 오류 알림

현황 화면은 `총투자금액`, `보유현금`, `총매수원가`, `총보유자산`, `총코인평가액`을 5초마다 갱신합니다. 코인 화면은 전체 등록 종목에서 현재 신규 매수 대상을 색상으로 구분합니다. 거래내역은 UTC 절대시각으로 최신순 정렬한 후 Android 기기의 현지 시간대로 표시하므로 한국 날짜와 미국 날짜가 달라도 최신 거래가 누락되지 않습니다.

## 보안 원칙

업비트 API 키와 Secret Key를 Android 앱에 저장하거나 포함하지 않습니다. 키 갱신 화면은 사용자가 입력한 키를 HTTPS로 서버에 한 번 전달하고 입력값을 즉시 비웁니다. CoinSDash는 Bearer 토큰으로 인증된 CoinSDance 서버 API만 호출하며 HTTP 주소를 거부합니다. 실제 주문 권한은 서버에만 두고 앱 백업도 비활성화합니다. 앱과 저장소에는 비밀값을 커밋하지 않습니다.

## 서버 API 계약

- `GET /api/v1/dashboard`: 자산, 봇 상태, 추적 수, 등록 코인, 등록해제 이력과 최근 거래
- `PUT /api/v1/credentials`: 새 업비트 Access/Secret Key 검증 및 교체
- 요청 헤더: `Authorization: Bearer <DASHBOARD_TOKEN>`

앱의 설정 화면에서 유효한 인증서가 적용된 `https://` 서버 주소와 대시보드 토큰을 입력합니다. CoinSDance의 내부 8080 포트를 인터넷에 직접 노출하지 않습니다.

## 기술 구성

- Kotlin 2.2.10
- Jetpack Compose 및 Material 3
- Android Gradle Plugin 9.3.2
- Android API 24 이상
- Java 11

## 개발 환경

1. Android Studio에서 이 저장소를 엽니다.
2. Android SDK 경로가 담긴 `local.properties`를 로컬에 준비합니다.
3. Gradle 동기화를 실행합니다.
4. API 24 이상의 에뮬레이터나 Android 기기에서 `app` 구성을 실행합니다.

`local.properties`, IDE 설정, Gradle 캐시, 빌드 산출물과 키스토어는 Git에서 제외됩니다.

## 명령행 빌드

```bash
./gradlew assembleDebug
```

단위 테스트:

```bash
./gradlew test
```

## 연관 프로젝트

- CoinSDance: Upbit 실거래 전략과 주문 실행을 담당하는 Go 서버
- CoinSDash: CoinSDance의 운영 상태를 표시하는 Android 클라이언트
