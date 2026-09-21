# Coinsdance

Coinsdance는 AWS Lightsail에서 실행 중인 CoinSDance 자동매매 봇의 상태를 Android에서 확인하기 위한 대시보드 앱입니다.

현재 배포 버전은 **1.0.9 (versionCode 10)**이며, 배포별 변경사항은 [RELEASE_NOTES.md](RELEASE_NOTES.md)에 기록합니다. 사용자가 버전 유지를 요청한 수정은 같은 버전에 포함하고, 실제 신규 배포가 필요할 때만 `versionName`과 `versionCode`를 함께 올립니다.

## 현재 상태

Jetpack Compose 대시보드와 CoinSDance HTTPS API 클라이언트가 구현되어 있습니다. 앱은 Firebase Google 로그인으로 사용자를 인증하고 고정된 CoinSDance HTTPS 서버에 자동 연결합니다.

## 예정 기능

- CoinSDance 프로세스 생존 여부, 마지막 정상 동기화 시각과 장애 표시
- 전체 평가자본, 사용 가능 KRW와 보유 종목 수
- 신규 매수 활성 종목 수와 현재 배정액의 첫 단계(`1/45`)로 계산한 최소 1회 매수금액
- 매수·매도 목표가에 도달하여 실제 저점·고점을 추적 중인 종목 수. 괄호 안에는 보유 중인 전체 코인 가운데 현재가가 업비트 공식 매수평단가보다 낮은 종목 수(매수 카드)와 높은 종목 수(매도 카드)를 표시
- 미체결 매수·매도 주문 수
- 최근 매수·매도 체결 내역
- 유의종목, Coinbase 등록 해제, API 및 주문 오류 알림

현황 화면은 `총투자금액`, `보유현금`, `총매수원가`, `총보유자산`, `총코인평가액`을 5초마다 갱신합니다. 코인 화면은 엑셀 시트처럼 `코인명`, `총매수원가`, `현재평가액`, `등락률`, `보유유무`, `매수대상` 순서의 고정 타이틀 행과 데이터 행으로만 구성됩니다. 좌우로 스크롤해도 코인명 열은 화면 왼쪽에 고정됩니다. 숫자 열의 헤더와 값은 같은 오른쪽 정렬선과 오른쪽 여백을 사용합니다. 등록 코인 타이틀과 표 사이의 코인명 전용 검색창은 입력 문자열을 포함하는 코인을 대소문자 구분 없이 즉시 제안하고 표도 실시간으로 좁히며, 제안 항목을 선택할 수 있습니다. 최초 진입 시 보유유무는 `유`, 정렬은 총매수원가 내림차순이 기본값입니다. 숫자 정렬을 모두 해제하면 코인명 알파벳순으로 표시합니다. 사용자가 바꾼 검색어·필터·정렬은 다른 탭을 다녀오거나 앱을 background에서 복귀해도 유지되며, 로그아웃 또는 앱 프로세스 재시작 시 기본값으로 돌아갑니다. 보유유무와 매수대상은 각각 전체·유·무를 선택할 수 있고, 숫자 열은 여러 개를 동시에 선택해 선택 순서대로 다중 정렬할 수 있습니다. 거래내역도 모든 거래가 KRW 마켓이라는 전제에서 `KRW-` 접두사를 생략합니다. UTC 절대시각으로 최신순 정렬한 후 Android 기기의 현지 시간대로 표시하므로 한국 날짜와 미국 날짜가 달라도 최신 거래가 누락되지 않습니다. 매도 거래에는 서버가 체결 당시 공식 평단과 매수·매도 수수료를 반영해 영구 저장한 실현 순수익을 표시한다. 해당 기능 배포 전 거래와 외부·수동 매도는 정확한 당시 원가가 없으므로 `실수익 집계 전`으로 표시한다.

## 보안 원칙

업비트 API 키와 Secret Key를 Android 앱에 저장하거나 포함하지 않습니다. 키 갱신 화면은 사용자가 입력한 키를 HTTPS로 서버에 한 번 전달하고 입력값을 즉시 비웁니다. 앱은 Firebase ID 토큰을 Bearer 인증값으로 보내며 서버는 Google 서명, 이메일 인증 여부와 허용 계정 목록을 모두 검증합니다. 서버 주소와 공용 인증 토큰을 사용자에게 입력받거나 기기에 저장하지 않습니다. 실제 주문 권한과 Firebase Admin 서비스 계정 키는 서버에만 두고 앱 백업도 비활성화합니다. 비밀값은 Git에 커밋하지 않습니다.

## 서버 API 계약

- `GET /api/v1/dashboard`: 자산, 봇 상태, 추적 수, 등록 코인, 등록해제 이력과 최근 거래

인증 상태와 서버 연결 상태는 서로 독립적으로 관리한다. 로그인 세션이 없거나 HTTP 401/403 또는 유효한 Firebase 사용자 상실이 확인되면 연결 장애로 표시하지 않고 로그인 화면으로 이동한다. 기존 Firebase 로그인 세션이 있으면 즉시 대시보드 조회를 시작하며 로그아웃은 설정 화면에서 실행한다. 서버 연결 상태의 기본값은 항상 `LOADING`이며 앱이 foreground로 복귀할 때마다 `LOADING`, 실패 횟수 0으로 초기화한 뒤 즉시 새로 조회한다. resume·로그아웃·인증 전환으로 기존 요청을 정상 취소한 경우에는 연결 실패로 계산하지 않는다. 첫 응답 전과 통신 실패 후 자동 재시도 중에는 `봇 상태 확인 중`을 표시하고, 5초 간격의 실제 통신 실패가 3회 연속 발생한 경우에만 `봇 장애 또는 연결 끊김`을 표시한다. 정상 응답에 성공하면 연결 오류와 실패 횟수는 즉시 초기화된다. 서버가 정상 응답으로 `alive=false`를 반환한 경우에는 실제 봇 장애로 표시한다.

등록 코인 목록은 서버의 같은 조회 주기에서 얻은 업비트 실잔고·공식 평단·현재가로 총매수원가, 현재평가액과 등락률을 계산한다. 앱에서는 불필요한 `KRW-` 접두사를 숨기고 엑셀형 6열 표로 표시한다. `MainActivity`는 Activity 생명주기와 Compose 진입점만 담당하며 앱 셸, 공용 컴포넌트와 각 화면은 파일별로 분리한다. 모든 화면에는 Android Studio에서 바로 확인할 수 있는 샘플 데이터 기반 Compose Preview가 있다.
- `PUT /api/v1/credentials`: 새 업비트 Access/Secret Key 검증 및 교체
- 요청 헤더: `Authorization: Bearer <FIREBASE_ID_TOKEN>`

앱 설정에서 허용된 Google 계정으로 로그인합니다. 현재 서버 주소는 앱에 고정되어 있으며 CoinSDance의 내부 8080 포트는 인터넷에 직접 노출하지 않습니다. Firebase Console에는 배포 APK의 SHA-1/SHA-256이 등록되어야 합니다. 향후 Google Play 배포 시에는 Play App Signing이 발급한 앱 서명 인증서 지문도 Firebase Android 앱에 추가하고 새 `google-services.json`으로 교체합니다.

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

실제 설치 APK의 앱 서명 키는 Google Play App Signing이 생성·보관한다. Coinsdance 빌드 서버에는 별도의 **업로드 키**만 보관하며, 이 키로 서명한 AAB를 Play Console에 전달한다.

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
- Coinsdance: CoinSDance의 운영 상태를 표시하는 Android 클라이언트
