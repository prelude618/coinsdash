# CoinSDash

CoinSDash는 AWS Lightsail에서 실행 중인 CoinSDance 자동매매 봇의 상태를 Android에서 확인하기 위한 대시보드 앱입니다.

## 현재 상태

현재 저장소는 Android Studio의 기본 Jetpack Compose 앱을 생성한 초기 단계입니다. 서버 상태 API와 대시보드 화면은 아직 구현되지 않았습니다.

## 예정 기능

- CoinSDance 프로세스 생존 여부와 마지막 정상 동기화 시각
- 전체 평가자본, 사용 가능 KRW와 보유 종목 수
- 신규 매수 활성 종목과 종목별 배정액
- 매수 저점 추적 및 매도 고점 추적 종목 수
- 미체결 매수·매도 주문 수
- 최근 매수·매도 체결 내역
- 유의종목, Coinbase 등록 해제, API 및 주문 오류 알림

## 보안 원칙

업비트 API 키와 Secret Key를 Android 앱에 저장하거나 포함하지 않습니다. CoinSDash는 인증된 읽기 전용 CoinSDance 서버 API만 호출해야 합니다. 실제 주문 권한은 서버에만 두고, 앱과 저장소에는 비밀값을 커밋하지 않습니다.

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
