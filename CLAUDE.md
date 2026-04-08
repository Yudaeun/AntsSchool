# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요
일반인 개미 투자자를 위한 경제 학습 안드로이드 앱 — **AntsSchool (개미의 경제학교)**.
듀오링고처럼 쉽고 재미있게 경제를 배울 수 있도록 설계.

## 빌드 명령어

```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리즈 APK 빌드
./gradlew assembleRelease

# 단위 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.day.antsschool.ExampleUnitTest"

# 기기/에뮬레이터 계측 테스트
./gradlew connectedAndroidTest

# 디버그 빌드 기기에 설치
./gradlew installDebug

# 클린 빌드
./gradlew clean
```

## 기술 스택
- **언어**: Kotlin (Java 혼용 금지)
- **UI**: Jetpack Compose + Material3
- **로컬 캐시**: Room DB (오프라인 대비용)
- **인증**: Firebase Authentication (Google 로그인)
- **서버 통신**: Retrofit2
- **백엔드**: Kotlin + Spring Boot
- **서버 DB**: PostgreSQL on Railway
- **AI 퀴즈 생성**: Claude Haiku API
- **뉴스**: RSS 파싱
- **광고**: Google AdMob
- **빌드**: Gradle Kotlin DSL, 의존성 버전은 `gradle/libs.versions.toml`로 관리

## 데이터 저장 원칙
- 학습 진도, 퀴즈 결과, 레벨/배지/스트릭, 북마크 → 서버 저장 (Google 계정 연동)
- Room DB는 오프라인 캐시 용도로만 사용
- 기기가 바뀌어도 Google 로그인하면 모든 데이터 복원

## 앱 구조 — 하단 탭 5개
1. **홈**: 오늘의 경제 한 줄, 학습 스트릭, 오늘 할 일 카드, 뉴스 미리보기, 레벨/포인트, 광고 배너
2. **학습**: 입문/초급/중급 챕터, 카드 슬라이드, 진도율, 챕터 완료 시 배지, 잠금/해금 구조
3. **뉴스**: 오늘의 경제 뉴스, 쉬운 말 해설, 관련 개념 연결, 뉴스 기반 퀴즈, 북마크
4. **퀴즈**: 데일리/복습/AI 생성 퀴즈, 오답노트, 연속 정답 콤보, 랭킹(프리미엄)
5. **내 기록**: 레벨/경험치, 학습 캘린더(스트릭), 배지, 퀴즈 정답률, 프리미엄 업그레이드

## 학습 커리큘럼 (총 20챕터)

### 입문 단계 (6챕터)
입문01 돈이란 무엇인가 / 입문02 금리 기초 / 입문03 인플레이션 / 입문04 환율 기초 / 입문05 주식이란 / 입문06 은행 vs 투자

### 초급 단계 (8챕터)
초급01 ETF 기초 / 초급02 배당주 / 초급03 채권 기초 / 초급04 PER·PBR / 초급05 경제지표 읽기 / 초급06 분산투자 / 초급07 세금과 투자 / 초급08 ISA·IRP

### 중급 단계 (6챕터)
중급01 경기 사이클 / 중급02 연준(Fed) 이해 / 중급03 섹터 분석 / 중급04 채권 금리 역전 / 중급05 포트폴리오 / 중급06 환율과 수출

**챕터 구성**: 카드 5~7장 + 퀴즈 3문제, 5분 이내 완료, 카드 한 장에 개념 하나

## 게임화 요소
- **레벨/XP**: 학습·퀴즈 완료 시 경험치 적립
- **스트릭**: 연속 출석일 수 (끊기면 리셋)
- **배지**: 챕터 완료, 퀴즈 전체 정답, 7일 스트릭 등
- **콤보**: 퀴즈 연속 정답 시 보너스 XP
- **오답노트**: 틀린 문제 자동 저장 → 복습 유도

## 수익 모델
- **1단계**: Google AdMob 광고
- **2단계**: 프리미엄 구독 (광고 제거, AI 퀴즈 무제한, 랭킹, 상세 통계)

## 개발 단계 로드맵
- 1~2주차: 하단 탭 내비게이션, 홈/학습 화면 UI
- 3주차: Firebase 프로젝트 연결, Google 로그인 구현
- 4~6주차: 학습 카드, 퀴즈 화면, Spring Boot 백엔드, Retrofit 서버 연동
- 7~9주차: 레벨/배지/스트릭 게임화, 내 기록 탭
- 10~11주차: 뉴스 RSS, Claude API AI 퀴즈, AdMob 광고
- 12주차: Play Store 출시

## 현재 진행 상태
- [x] Java 21, Android Studio 설치
- [x] AntsSchool 프로젝트 생성 (Kotlin + Jetpack Compose + API 24)
- [x] Claude Code 연결
- [x] 하단 탭 내비게이션 구현
- [x] 홈 화면 구현
- [x] 학습 탭 구현
- [x] Firebase 인증 코드 구현 (google-services.json 추가 필요)
- [x] Spring Boot 백엔드, Retrofit 서버 연동
- [x] 뉴스 RSS 파싱 및 북마크
- [x] 레벨/배지/스트릭 게임화, 내 기록 탭
- [x] AdMob 배너 광고 (홈·학습·뉴스 상단 / 퀴즈 하단, 현재 테스트 ID 사용)
- [ ] AI 퀴즈 (Claude Haiku API 연동) — **현재 준비 중, 퀴즈 탭에 "준비중" 안내 카드로 표시**
- [ ] Play Store 출시

## 미구현 기능 메모
- **AI 퀴즈**: Claude Haiku API를 사용한 즉석 퀴즈 생성 기능은 미구현 상태.
  퀴즈 탭에서 AI 퀴즈 카드를 "준비중" UI로만 표시하고 있음.
  구현 시 `QuizViewModel.generateAiQuiz()` 함수와 `AiQuizComingSoonCard()`를 실제 `AiQuizCard()`로 교체할 것.

## 코딩 규칙
- Kotlin만 사용, Jetpack Compose로 모든 UI 구현
- 함수명/변수명은 영어, 주석은 한국어
- 하나의 파일에 하나의 화면(Screen) 원칙
- 색상/폰트는 `MaterialTheme`으로 통일
