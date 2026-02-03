# Aion 2 DPS 미터

> **AION 2를 위한 실시간 전투 분석 도구**
>
> 스레드 안전성 개선 및 현대적인 아키텍처로 리팩토링된 버전입니다.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io)
[![Release](https://img.shields.io/github/v/release/nousx/aion2-dps-meter)](https://github.com/nousx/aion2-dps-meter/releases)

[English](../README.md) | [简体中文](README.zh-Hans.md) | [繁體中文](README.zh-Hant.md) | [ไทย](README.th.md) | [Русский](README.ru.md)

---

## 📸 스크린샷

### 메인 DPS 미터
![DPS Meter](main_ui.png)

### 플레이어 상세 정보
![Player Details](playerdetail_ui.png)

---

## ✨ 주요 기능

- **실시간 DPS 추적** - 100ms마다 업데이트
- **스킬 분석** - 크리티컬 확률 및 특성 슬롯 상세 분석
- **파티 순위** - 파티원들과 데미지 비교
- **버스트 DPS** - 5초 슬라이딩 윈도우로 데미지 스파이크 분석
- **자동 클래스 감지** - 플레이어 클래스 자동 식별
- **다국어 지원** - 영어, 한국어, 중국어 (간체/번체)
- **전역 단축키** - 창 포커스 없이 표시 전환 및 초기화

---

## 📦 빠른 시작

### 요구사항
- **Windows 10/11** (x64)
- **Java 21+** ([다운로드](https://adoptium.net/temurin/releases/?version=21))
- **Npcap** ([다운로드](https://npcap.com/#download))
  - ⚠️ **반드시** "Install Npcap in WinPcap API-compatible Mode" 체크

### 설치 방법

1. 최신 [Release](https://github.com/nousx/aion2-dps-meter/releases) **다운로드**
2. Java 21+ 및 Npcap **설치**
3. 관리자 권한으로 MSI 설치 파일 **실행**
4. 관리자 권한으로 애플리케이션 **시작**

### 첫 실행 설정

1. AION 2가 실행 중이면 캐릭터 선택 화면으로 이동
2. 관리자 권한으로 DPS 미터 실행
3. Windows 방화벽 프롬프트 허용
4. 게임 월드 입장

**문제 해결:** 미터가 표시되지 않으면 키스크/은신처로 순간이동하거나 던전 입장/퇴장을 시도하세요.

---

## 🎮 사용 방법

### 단축키
- **표시 전환:** `Ctrl+Shift+H` (사용자 지정 가능)
- **DPS 초기화:** `Ctrl+Shift+R` (사용자 지정 가능)

### 상세 패널
플레이어 이름을 클릭하여 확인:
- 총 데미지 및 DPS
- 기여도 비율
- 크리티컬, 퍼펙트, 백어택 확률
- 스킬별 상세 분석
- 특성 슬롯 사용 현황 (1-5)

---

## ❓ 자주 묻는 질문

**Q: 다른 DPS 미터와 무엇이 다른가요?**

이 버전은 게임 패킷 자동 감지, VPN/핑 리듀서 지원을 추가하고 스레드 안전성을 위해 완전히 리팩토링되었습니다. 또한 스킬과 UI에 대한 영어 번역이 포함되어 있습니다.

**Q: 이름 대신 숫자가 표시되는 이유는?**

게임에서 이름을 자주 전송하지 않기 때문에 이름 감지에 시간이 걸립니다. 순간이동 주문서를 사용하거나 군단 홀로 이동하여 속도를 높이세요. ExitLag을 사용하는 경우 "모든 연결 재시작 바로 가기" 옵션을 활성화하여 더 빠르게 다시 로드하세요.

**Q: UI는 표시되지만 데미지가 나타나지 않아요.**

- Npcap이 올바르게 설치되었는지 확인
- 앱을 종료하고 캐릭터 선택으로 이동한 후 다시 시작
- 순간이동으로 패킷 캡처 새로고침 시도

**Q: 다른 플레이어의 DPS는 보이지만 내 것은 안 보여요.**

DPS는 가장 많은 총 데미지를 받은 몬스터를 기준으로 계산됩니다. 미터에 이미 표시된 플레이어와 동일한 훈련 더미를 공격하고 있는지 확인하세요.

**Q: 솔로일 때 기여도가 100%가 아니에요.**

이것은 일반적으로 이름 캡처 실패를 의미합니다. 위에 언급된 순간이동 방법을 시도하세요.

**Q: 채팅 명령어나 Discord 연동을 사용할 수 있나요?**

아직은 아니지만 향후 추가될 수 있습니다!

**Q: 히트 카운트가 스킬 시전 횟수보다 높아요.**

다단 히트 스킬은 각 히트를 개별적으로 계산합니다.

**Q: 일부 스킬이 이름 대신 숫자로 표시돼요.**

이것들은 보통 테오스톤(장신구 효과)입니다. 숫자로 표시되는 다른 스킬을 발견하면 [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues)를 통해 신고해 주세요.

---

## 🛠️ 소스에서 빌드

```bash
# 저장소 복제
git clone https://github.com/nousx/aion2-dps-meter.git
cd aion2-dps-meter

# 빌드
./gradlew build

# MSI 설치 파일 생성
./gradlew packageMsi
```

---

## 📖 문서

- **[아키텍처](ARCHITECTURE.md)** - 기술 심층 분석
- **[변경 로그](../CHANGELOG.md)** - 버전 기록
- **[기여 가이드](CONTRIBUTING.md)** - 기여 방법 *(준비 중)*

---

## 🔧 기술적 하이라이트

이 리팩토링된 버전에는 주요 개선 사항이 포함됩니다:

- ✅ **스레드 안전 아키텍처** - 3개의 HIGH 심각도 경합 조건 수정
- ✅ **모듈식 코드 구조** - 추출을 통해 ~1,100+ 라인 감소
- ✅ **외부화된 스킬 데이터** - 편집 가능한 JSON의 391개 스킬
- ✅ **통합 로깅** - 백그라운드 스레드 67% 감소 (3→1)
- ✅ **성능 최적화** - 잠금 없는 원자 연산

**영향:**
- 🔒 공유 상태의 100%가 적절히 보호됨
- 📉 코드가 ~1,100+ 라인 감소
- 🏗️ StreamProcessor: 1009→400 라인 (4개 클래스로 분할)
- 📝 DpsCalculator: 1100→280 라인
- ⚡ 백그라운드 스레드 67% 감소

---

## 🤝 기여하기

기여를 환영합니다! 자유롭게:
- [Issues](https://github.com/nousx/aion2-dps-meter/issues)를 통해 버그 신고
- Pull request 제출
- 기능 제안 또는 개선 사항

---

## 📄 라이선스

MIT 라이선스 - 자세한 내용은 [LICENSE](../LICENSE)를 참조하세요.

**크레딧:**
- 원작: [TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
- 지속 개발: [taengu](https://github.com/taengu/Aion2-Dps-Meter)
- 리팩토링 버전: SpecTruM

---

## ⚠️ 면책 조항

이 도구는 **개인 사용 및 교육 목적으로만** 제공됩니다.
- 본인 책임하에 사용
- 개발자는 어떠한 결과에도 책임을 지지 않음
- 게임 이용 약관을 존중하세요

---

## 📞 지원

- **Issues:** [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues)
- **Discord:** https://discord.gg/Aion2Global
- **문서:** [docs/](.)

---

**AION 2 커뮤니티를 위해 ❤️로 제작**
