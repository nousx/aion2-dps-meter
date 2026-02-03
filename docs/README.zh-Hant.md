# Aion 2 DPS 統計工具

> **AION 2 即時戰鬥分析工具**
>
> 重構版本，具有執行緒安全改進和現代化架構。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io)
[![Release](https://img.shields.io/github/v/release/nousx/aion2-dps-meter)](https://github.com/nousx/aion2-dps-meter/releases)

[English](../README.md) | [한국어](README.ko.md) | [简体中文](README.zh-Hans.md) | [ไทย](README.th.md) | [Русский](README.ru.md)

---

## 📸 截圖

### 主介面 DPS 統計
![DPS Meter](main_ui.png)

### 玩家詳情
![Player Details](playerdetail_ui.png)

---

## ✨ 主要功能

- **即時 DPS 追蹤** - 每 100ms 更新一次
- **技能分析** - 爆擊率和特性槽位詳細分析
- **隊伍排名** - 與隊友比較傷害
- **爆發 DPS** - 5 秒滑動視窗分析傷害峰值
- **自動職業偵測** - 自動識別玩家職業
- **多語言支援** - 英語、韓語、中文（簡體/繁體）
- **全域熱鍵** - 無需聚焦視窗即可切換顯示和重置

---

## 📦 快速開始

### 系統需求
- **Windows 10/11** (x64)
- **Java 21+** ([下載](https://adoptium.net/temurin/releases/?version=21))
- **Npcap** ([下載](https://npcap.com/#download))
  - ⚠️ **必須** 勾選 "Install Npcap in WinPcap API-compatible Mode"

### 安裝步驟

1. **下載** 最新 [Release](https://github.com/nousx/aion2-dps-meter/releases)
2. **安裝** Java 21+ 和 Npcap
3. **以系統管理員身分執行** MSI 安裝程式
4. **以系統管理員身分啟動** 應用程式

### 首次設定

1. 如果 AION 2 正在執行，進入角色選擇介面
2. 以系統管理員身分啟動 DPS 統計工具
3. 允許 Windows 防火牆提示
4. 進入遊戲世界

**故障排除：** 如果統計工具未顯示，請使用傳送石/藏身處傳送，或進出副本。

---

## 🎮 使用方法

### 快速鍵
- **切換顯示：** `Ctrl+Shift+H`（可自訂）
- **重置 DPS：** `Ctrl+Shift+R`（可自訂）

### 詳情面板
點擊任意玩家名稱查看：
- 總傷害和 DPS
- 貢獻百分比
- 爆擊、完美、背擊率
- 技能詳細分析
- 特性槽位使用情況（1-5）

---

## ❓ 常見問題

**Q: 這個工具與其他 DPS 統計工具有什麼不同？**

此版本增加了遊戲封包自動偵測、VPN/延遲優化器支援，並為執行緒安全進行了完全重構。此外，還包含了技能和 UI 的英文翻譯。

**Q: 為什麼顯示數字而不是名字？**

名字偵測需要一些時間，因為遊戲不經常傳送名字。嘗試使用傳送卷軸或前往軍團大廳加快速度。如果您使用 ExitLag，啟用「重啟所有連線的快速鍵」選項並使用它來更快地重新載入。

**Q: UI 顯示了但沒有傷害資料。**

- 確認 Npcap 已正確安裝
- 結束應用程式，前往角色選擇，然後重新啟動
- 嘗試傳送以重新整理封包擷取

**Q: 我看到其他玩家的 DPS 但看不到自己的。**

DPS 是基於受到最多總傷害的怪物計算的。確保您正在攻擊與統計工具上已顯示玩家相同的訓練假人。

**Q: 單人時貢獻度不是 100%。**

這通常意味著名字擷取失敗。嘗試上述提到的傳送方法。

**Q: 可以使用聊天指令或 Discord 整合嗎？**

目前還不行，但未來可能會新增！

**Q: 命中次數高於技能施放次數。**

多段攻擊技能會單獨計算每次命中。

**Q: 某些技能顯示為數字而不是名稱。**

這些通常是神石（飾品效果）。如果您發現其他顯示為數字的技能，請透過 [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues) 回報。

---

## 🛠️ 從原始碼建置

```bash
# 複製儲存庫
git clone https://github.com/nousx/aion2-dps-meter.git
cd aion2-dps-meter

# 建置
./gradlew build

# 建立 MSI 安裝程式
./gradlew packageMsi
```

---

## 📖 文件

- **[架構](ARCHITECTURE.md)** - 技術深度剖析
- **[更新日誌](../CHANGELOG.md)** - 版本歷史
- **[貢獻指南](CONTRIBUTING.md)** - 如何貢獻 *（即將推出）*

---

## 🔧 技術亮點

此重構版本包含重大改進：

- ✅ **執行緒安全架構** - 修復了 3 個高嚴重性競態條件
- ✅ **模組化程式碼結構** - 透過提取減少了約 1,100+ 行
- ✅ **外部化技能資料** - 可編輯 JSON 中的 391 個技能
- ✅ **統一日誌記錄** - 背景執行緒減少 67%（3→1）
- ✅ **效能優化** - 無鎖原子操作

**影響：**
- 🔒 100% 的共享狀態得到適當保護
- 📉 程式碼減少約 1,100+ 行
- 🏗️ StreamProcessor：1009→400 行（拆分為 4 個類別）
- 📝 DpsCalculator：1100→280 行
- ⚡ 背景執行緒減少 67%

---

## 🤝 貢獻

歡迎貢獻！請隨時：
- 透過 [Issues](https://github.com/nousx/aion2-dps-meter/issues) 回報錯誤
- 提交 Pull Request
- 提出功能建議或改進

---

## 📄 授權

MIT 授權 - 詳情請參閱 [LICENSE](../LICENSE)。

**致謝：**
- 原作：[TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
- 持續開發：[taengu](https://github.com/taengu/Aion2-Dps-Meter)
- 重構版本：SpecTruM

---

## ⚠️ 免責聲明

此工具僅供 **個人使用和教育目的**。
- 使用風險自負
- 開發者對任何後果概不負責
- 請尊重遊戲服務條款

---

## 📞 支援

- **Issues：** [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues)
- **Discord：** https://discord.gg/Aion2Global
- **文件：** [docs/](.)

---

**為 AION 2 社群用 ❤️ 製作**
