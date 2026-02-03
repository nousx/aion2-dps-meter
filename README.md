# Aion 2 DPS Meter

> **Real-time combat analysis tool for AION 2**
>
> Refactored version with thread safety improvements and modern architecture.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io)
[![Release](https://img.shields.io/github/v/release/nousx/aion2-dps-meter)](https://github.com/nousx/aion2-dps-meter/releases)

[한국어](docs/README.ko.md) | [简体中文](docs/README.zh-Hans.md) | [繁體中文](docs/README.zh-Hant.md) | [ไทย](docs/README.th.md) | [Русский](docs/README.ru.md)

---

## 📸 Screenshots

### Main DPS Meter
![DPS Meter](docs/main_ui.png)

### Player Details
![Player Details](docs/playerdetail_ui.png)

---

## ✨ Features

- **Real-time DPS tracking** - Updates every 100ms
- **Skill breakdown** - Detailed analysis with crit rates and specialty slots
- **Party rankings** - Compare damage with party members
- **Burst DPS** - 5-second sliding window for damage spikes
- **Auto class detection** - Automatically identifies player classes
- **Multi-language** - English, Korean, Chinese (Simplified/Traditional)
- **Global hotkeys** - Toggle visibility and reset without focusing window

---

## 📦 Quick Start

### Requirements
- **Windows 10/11** (x64)
- **Java 21+** ([Download](https://adoptium.net/temurin/releases/?version=21))
- **Npcap** ([Download](https://npcap.com/#download))
  - ⚠️ **MUST** check "Install Npcap in WinPcap API-compatible Mode"

### Installation

1. **Download** the latest [Release](https://github.com/nousx/aion2-dps-meter/releases)
2. **Install** Java 21+ and Npcap
3. **Run** the MSI installer as Administrator
4. **Launch** the application as Administrator

### First Time Setup

1. Go to character selection screen (if AION 2 is running)
2. Launch DPS meter as Administrator
3. Allow Windows Firewall prompt
4. Enter game world

**Troubleshooting:** If the meter doesn't appear, teleport using Kisk/Hideout or enter/exit a dungeon.

---

## 🎮 Usage

### Hotkeys
- **Toggle Visibility:** `Ctrl+Shift+H` (customizable)
- **Reset DPS:** `Ctrl+Shift+R` (customizable)

### Details Panel
Click any player name to view:
- Total damage and DPS
- Contribution percentage
- Crit, Perfect, Back attack rates
- Skill-by-skill breakdown
- Specialty slot usage (1-5)

---

## ❓ FAQ

**Q: What makes this different from other DPS meters?**

This version adds auto-detection for game packets, VPN/ping reducer support, and has been fully refactored for thread safety. Plus, it's got English translations for skills and UI.

**Q: Why do I see numbers instead of names?**

Name detection takes a moment because the game doesn't send names very often. Try using a teleport scroll or go to your Legion Hall to speed it up. If you use ExitLag, enable "Shortcut to restart all connections" and use it to reload faster.

**Q: The UI shows up but no damage appears.**

- Double-check that Npcap is installed correctly
- Exit the app, go to character select, then relaunch
- Try teleporting to refresh packet capture

**Q: I see other players' DPS but not mine.**

DPS is calculated based on the monster taking the most total damage. Make sure you're hitting the same training dummy as everyone else on the meter.

**Q: Contribution doesn't add up to 100% when I'm solo.**

This usually means name capture failed. Try the teleport trick mentioned above.

**Q: Can I use chat commands or integrate with Discord?**

Not yet, but maybe in the future!

**Q: Why is hit count higher than my skill casts?**

Multi-hit skills count each individual hit separately.

**Q: Some skills show as numbers instead of names.**

These are usually Theostones (accessory procs). If you find other skills showing as numbers, please report them via [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues).

---

## 🛠️ Building from Source

```bash
# Clone repository
git clone https://github.com/nousx/aion2-dps-meter.git
cd aion2-dps-meter

# Build
./gradlew build

# Create MSI installer
./gradlew packageMsi
```

---

## 📖 Documentation

- **[Architecture](docs/ARCHITECTURE.md)** - Technical deep dive
- **[Changelog](CHANGELOG.md)** - Version history
- **[Contributing](docs/CONTRIBUTING.md)** - How to contribute *(coming soon)*

---

## 🔧 Technical Highlights

This refactored version includes major improvements:

- ✅ **Thread-safe architecture** - Fixed 3 HIGH severity race conditions
- ✅ **Modular code structure** - Reduced ~1,100+ lines through extraction
- ✅ **Externalized skill data** - 391 skills in editable JSON
- ✅ **Unified logging** - 67% reduction in background threads (3→1)
- ✅ **Performance optimized** - Lock-free atomic operations

**Impact:**
- 🔒 100% of shared state properly protected
- 📉 Code reduced by ~1,100+ lines
- 🏗️ StreamProcessor: 1009→400 lines (split into 4 classes)
- 📝 DpsCalculator: 1100→280 lines
- ⚡ 67% fewer background threads

---

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs via [Issues](https://github.com/nousx/aion2-dps-meter/issues)
- Submit pull requests
- Suggest features or improvements

---

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.

**Credits:**
- Original work: [TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
- Continued development: [taengu](https://github.com/taengu/Aion2-Dps-Meter)
- Refactored version: SpecTruM

---

## ⚠️ Disclaimer

This tool is for **personal use and educational purposes only.**
- Use at your own risk
- The developer is not responsible for any consequences
- Respect the game's Terms of Service

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/nousx/aion2-dps-meter/issues)
- **Discord:** https://discord.gg/Aion2Global
- **Documentation:** [docs/](docs/)

---

**Made with ❤️ for the AION 2 community**
