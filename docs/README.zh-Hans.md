# Aion 2 DPS 统计工具

> **AION 2 实时战斗分析工具**
>
> 重构版本，具有线程安全改进和现代化架构。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](../LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io)
[![Release](https://img.shields.io/github/v/release/nousx/aion2-dps-meter)](https://github.com/nousx/aion2-dps-meter/releases)

[English](../README.md) | [한국어](README.ko.md) | [繁體中文](README.zh-Hant.md) | [ไทย](README.th.md) | [Русский](README.ru.md)

---

## 📸 截图

### 主界面 DPS 统计
![DPS Meter](main_ui.png)

### 玩家详情
![Player Details](playerdetail_ui.png)

---

## ✨ 主要功能

- **实时 DPS 追踪** - 每 100ms 更新一次
- **技能分析** - 暴击率和特性槽位详细分析
- **队伍排名** - 与队友比较伤害
- **爆发 DPS** - 5 秒滑动窗口分析伤害峰值
- **自动职业检测** - 自动识别玩家职业
- **多语言支持** - 英语、韩语、中文（简体/繁体）
- **全局热键** - 无需聚焦窗口即可切换显示和重置

---

## 📦 快速开始

### 系统要求
- **Windows 10/11** (x64)
- **Java 21+** ([下载](https://adoptium.net/temurin/releases/?version=21))
- **Npcap** ([下载](https://npcap.com/#download))
  - ⚠️ **必须** 勾选 "Install Npcap in WinPcap API-compatible Mode"

### 安装步骤

1. **下载** 最新 [Release](https://github.com/nousx/aion2-dps-meter/releases)
2. **安装** Java 21+ 和 Npcap
3. **以管理员身份运行** MSI 安装程序
4. **以管理员身份启动** 应用程序

### 首次设置

1. 如果 AION 2 正在运行，进入角色选择界面
2. 以管理员身份启动 DPS 统计工具
3. 允许 Windows 防火墙提示
4. 进入游戏世界

**故障排除：** 如果统计工具未显示，请使用传送石/藏身处传送，或进出副本。

---

## 🎮 使用方法

### 快捷键
- **切换显示：** `Ctrl+Shift+H`（可自定义）
- **重置 DPS：** `Ctrl+Shift+R`（可自定义）

### 详情面板
点击任意玩家名称查看：
- 总伤害和 DPS
- 贡献百分比
- 暴击、完美、背击率
- 技能详细分析
- 特性槽位使用情况（1-5）

---

## ❓ 常见问题

**Q: 这个工具与其他 DPS 统计工具有什么不同？**

此版本增加了游戏数据包自动检测、VPN/延迟优化器支持，并为线程安全进行了完全重构。此外，还包含了技能和 UI 的英文翻译。

**Q: 为什么显示数字而不是名字？**

名字检测需要一些时间，因为游戏不经常发送名字。尝试使用传送卷轴或前往军团大厅加快速度。如果您使用 ExitLag，启用"重启所有连接的快捷方式"选项并使用它来更快地重新加载。

**Q: UI 显示了但没有伤害数据。**

- 确认 Npcap 已正确安装
- 退出应用，前往角色选择，然后重新启动
- 尝试传送以刷新数据包捕获

**Q: 我看到其他玩家的 DPS 但看不到自己的。**

DPS 是基于受到最多总伤害的怪物计算的。确保您正在攻击与统计工具上已显示玩家相同的训练假人。

**Q: 单人时贡献度不是 100%。**

这通常意味着名字捕获失败。尝试上述提到的传送方法。

**Q: 可以使用聊天命令或 Discord 集成吗？**

目前还不行，但未来可能会添加！

**Q: 命中次数高于技能施放次数。**

多段攻击技能会单独计算每次命中。

**Q: 某些技能显示为数字而不是名称。**

这些通常是神石（饰品效果）。如果您发现其他显示为数字的技能，请通过 [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues) 报告。

---

## 🛠️ 从源代码构建

```bash
# 克隆仓库
git clone https://github.com/nousx/aion2-dps-meter.git
cd aion2-dps-meter

# 构建
./gradlew build

# 创建 MSI 安装程序
./gradlew packageMsi
```

---

## 📖 文档

- **[架构](ARCHITECTURE.md)** - 技术深度剖析
- **[更新日志](../CHANGELOG.md)** - 版本历史
- **[贡献指南](CONTRIBUTING.md)** - 如何贡献 *（即将推出）*

---

## 🔧 技术亮点

此重构版本包含重大改进：

- ✅ **线程安全架构** - 修复了 3 个高严重性竞态条件
- ✅ **模块化代码结构** - 通过提取减少了约 1,100+ 行
- ✅ **外部化技能数据** - 可编辑 JSON 中的 391 个技能
- ✅ **统一日志记录** - 后台线程减少 67%（3→1）
- ✅ **性能优化** - 无锁原子操作

**影响：**
- 🔒 100% 的共享状态得到适当保护
- 📉 代码减少约 1,100+ 行
- 🏗️ StreamProcessor：1009→400 行（拆分为 4 个类）
- 📝 DpsCalculator：1100→280 行
- ⚡ 后台线程减少 67%

---

## 🤝 贡献

欢迎贡献！请随时：
- 通过 [Issues](https://github.com/nousx/aion2-dps-meter/issues) 报告错误
- 提交 Pull Request
- 提出功能建议或改进

---

## 📄 许可证

MIT 许可证 - 详情请参阅 [LICENSE](../LICENSE)。

**致谢：**
- 原作：[TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
- 持续开发：[taengu](https://github.com/taengu/Aion2-Dps-Meter)
- 重构版本：SpecTruM

---

## ⚠️ 免责声明

此工具仅供 **个人使用和教育目的**。
- 使用风险自负
- 开发者对任何后果概不负责
- 请尊重游戏服务条款

---

## 📞 支持

- **Issues：** [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues)
- **Discord：** https://discord.gg/Aion2Global
- **文档：** [docs/](.)

---

**为 AION 2 社区用 ❤️ 制作**
