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

현황 화면은 `총투자금액`, `보유현금`, `총매수원가`, `총보유자산`, `총코인평가액`을 5초마다 갱신합니다. 코인 화면은 전체 등록 종목에서 현재 신규 매수 대상을 색상으로 구분합니다. 거래내역은 UTC 절대시각으로 최신순 정렬한 후 Android 기기의 현지 시간대로 표시하므로 한국 날짜와 미국 날짜가 달라도 최신 거래가 누락되지 않습니다. 매도 거래에는 서버가 체결 당시 공식 평단과 매수·매도 수수료를 반영해 영구 저장한 실현 순수익을 표시한다. 해당 기능 배포 전 거래와 외부·수동 매도는 정확한 당시 원가가 없으므로 `실수익 집계 전`으로 표시한다.

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
- Firebase 프로젝트 `coinsdance-72c2c` 연결

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

## Google Play 프로덕션 서명

실제 설치 APK의 앱 서명 키는 Google Play App Signing이 생성·보관한다. CoinSDash 빌드 서버에는 별도의 **업로드 키**만 보관하며, 이 키로 서명한 AAB를 Play Console에 전달한다.

빌드 장비의 서명 파일 배치:

```text
/etc/coinsdash/signing/upload.jks
/etc/coinsdash/signing/signing.properties
```

macOS 개발 장비에서는 다음 경로도 자동으로 인식한다.

```text
~/.config/coinsdash/signing/upload.jks
~/.config/coinsdash/signing/signing.properties
```

`signing.properties` 형식은 저장소의 `signing.properties.example`을 따른다. 두 실제 파일은 전용 빌드 사용자만 읽을 수 있도록 디렉터리는 0700, 파일은 0600으로 설정한다. 저장소, APK/AAB, 로그 또는 명령행 인수에 키와 암호를 넣지 않는다.

위 기본 경로에 키를 설치한 장비에서는 별도 옵션 없이 서명된 Production APK를 만든다.

```bash
./gradlew assembleRelease
```

결과 경로는 `app/build/outputs/apk/release/app-release.apk`다. Release 서명 파일이 없으면 unsigned APK를 만들지 않고 빌드를 실패시킨다.

Google Play 업로드용 AAB 빌드:

```bash
./scripts/build-release.sh
```

스크립트는 설정과 keystore의 권한이 정확히 0600인지 검사하고 Gradle configuration cache를 끈 뒤 서명된 `app-release.aab`을 만든다. 결과 경로는 `app/build/outputs/bundle/release/app-release.aab`이다. 최초 Play Console 릴리스에서는 Google이 앱 서명 키를 생성하도록 선택하고, 이 AAB의 인증서는 업로드 키로 등록한다.

업로드 키는 재발급할 수 있지만 빌드 연속성을 위해 암호화된 별도 백업을 보관한다. 실거래 CoinSDance 서버와 같은 장비를 사용해야 한다면 빌드 사용자와 디렉터리 권한을 반드시 분리한다.

## 연관 프로젝트

- CoinSDance: Upbit 실거래 전략과 주문 실행을 담당하는 Go 서버
- CoinSDash: CoinSDance의 운영 상태를 표시하는 Android 클라이언트
