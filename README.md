# Aion2 DPS Meter

> **A refactored and enhanced combat analysis tool for AION 2**
> Forked from [Aion2-Dps-Meter](https://github.com/taengu/Aion2-Dps-Meter) with major improvements in code quality, thread safety, and maintainability.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg)](https://kotlinlang.org)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io)

---

## 🎯 Description

A real-time DPS (Damage Per Second) meter and combat analyzer for AION 2 that captures network packets to display:
- **Real-time damage statistics** for all party members
- **Skill breakdowns** with crit rates, perfect hits, and specialty slots
- **DPS rankings** with contribution percentages
- **Burst DPS tracking** for analyzing damage windows
- **Combat time tracking** for accurate DPS calculations

This refactored version includes:
- ✅ **Thread-safe architecture** - Fixed 3 HIGH severity race conditions
- ✅ **Modular code structure** - Extracted parsers for better maintainability (~1100+ lines reduced)
- ✅ **Externalized skill data** - 391 skills in editable JSON configuration
- ✅ **Unified logging system** - 67% reduction in background threads (3→1)
- ✅ **AtomicInteger optimizations** - Zero boxing overhead for performance-critical paths

---

## 🚀 Features

### Core Features
- 📊 **Real-time DPS tracking** - Updates every 100ms for responsive feedback
- 🎮 **Skill analysis** - Detailed breakdown of each skill's performance
- 🏆 **Party rankings** - Compare your DPS with party members
- 📈 **Burst DPS calculation** - 5-second sliding window analysis
- ⚔️ **Specialty slot detection** - Automatically detects active specialty slots (1-5)
- 🎯 **Target switching** - Analyze DPS on different targets
- ⏱️ **Combat time tracking** - Accurate active time measurement

### Technical Features
- 🔒 **Thread-safe** - ConcurrentHashMap, synchronized blocks, atomic operations
- 🎨 **Modern UI** - Transparent JavaFX overlay with real-time updates
- 🌐 **Multi-language** - Support for EN, KO, ZH-Hans, ZH-Hant
- ⌨️ **Global hotkeys** - Toggle visibility and reset data without focusing window
- 📝 **Debug logging** - Optional packet logging for troubleshooting
- 🔧 **Configurable** - JSON-based skill configuration for easy updates

---

## 📦 Installation

### Requirements
- **Windows 10/11**
- **Java 21+** (JDK or JRE)
- **Npcap** (WinPcap-compatible mode)
- **Administrator privileges** (for packet capture)

### Setup Steps

1. **Install Npcap** (Required for packet capture)
   ```
   Download: https://npcap.com/#download
   ⚠️ MUST check "Install Npcap in WinPcap API-compatible Mode"
   ```

2. **Install Java 21+**
   ```
   Download: https://adoptium.net/temurin/releases/?version=21
   ```

3. **Download Release**
   - Go to [Releases](../../releases)
   - Download `aion2-dps-meter-{version}.msi`
   - Run installer as Administrator

4. **Launch Application**
   ```
   Run as Administrator (required for packet capture)
   ```

5. **First Time Setup**
   - If AION 2 is running, go to character selection screen
   - Launch DPS meter as Administrator
   - Allow Windows Firewall prompt (select Private + Public networks)
   - Enter game world - DPS meter should appear

6. **Troubleshooting**
   - If meter doesn't appear: Teleport using Kisk/Hideout or enter/exit dungeon
   - If meter stops working: Teleport again to refresh packet capture
   - Still not working: Restart from step 4

---

## 🛠️ Development

### Build from Source

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/aion2-dps-meter.git
cd aion2-dps-meter

# Build with Gradle
./gradlew build

# Run application
./gradlew run

# Create MSI installer
./gradlew packageMsi
```

### Project Structure

```
src/main/kotlin/
├── DataStorage.kt              # Thread-safe data storage layer
├── DpsCalculator.kt            # DPS calculation and skill inference
├── Main.kt                     # Application entry point
├── packet/
│   ├── PcapCapturer.kt         # Network packet capture
│   ├── StreamProcessor.kt      # Main packet processing coordinator
│   └── parser/
│       ├── DamagePacketParser.kt   # Damage packet parsing
│       ├── NameResolver.kt         # Nickname/entity name parsing
│       └── SummonTracker.kt        # Summon entity tracking
├── data/
│   └── SkillCodeLoader.kt      # JSON skill data loader
├── logging/
│   └── UnifiedLogger.kt        # Consolidated logging system
├── webview/
│   └── BrowserApp.kt           # JavaFX UI controller
└── entity/
    ├── ParsedDamagePacket.kt   # Damage packet data model
    └── PersonalData.kt         # Player statistics tracking

src/main/resources/
├── data/
│   └── skill_codes.json        # 391 skill definitions (editable!)
├── js/
│   ├── core.js                 # Main UI logic
│   ├── details.js              # Skill detail panel
│   └── meter.js                # DPS meter rendering
└── index.html                  # Main UI template
```

### Key Technologies

- **Kotlin 1.9+** - Modern JVM language
- **JavaFX 21** - UI framework with WebView
- **Pcap4J** - Network packet capture library
- **Kotlinx Serialization** - JSON handling for skill data
- **JNA** - Native Windows API integration (global hotkeys)

---

## 🎮 Usage

### UI Components

- **Blue Header** - Monster/target name (when available)
- **Brown Button** - Reset current combat data
- **Pink Button** - Expand/collapse DPS meter
- **Class Icons** - Automatically detected class for each player
- **DPS Bars** - Real-time damage visualization with percentages
- **Details Panel** - Click any player to see skill breakdown

### Global Hotkeys

- **Toggle Visibility** - Default: `Ctrl+Shift+H` (customizable in settings)
- **Reset DPS** - Default: `Ctrl+Shift+R` (customizable in settings)

### Details Panel

Click any player name to open detailed statistics:
- **Total Damage** - Cumulative damage dealt
- **DPS** - Damage per second (active combat time)
- **Contribution %** - Percentage of total party damage
- **Crit Rate** - Critical hit percentage
- **Perfect Rate** - Perfect hit percentage (class-specific)
- **Skill Breakdown** - Each skill's damage, hit count, average damage
- **Specialty Slots** - Active specialty slots (1-5) highlighted per skill

---

## 🔧 Configuration

### Skill Data Configuration

Edit `src/main/resources/data/skill_codes.json` to update skill names or add new skills:

```json
{
  "version": "1.0",
  "skills": [
    {
      "code": 13350000,
      "name": "Deadly Strike",
      "specialtySlots": [1, 2, 3]
    }
  ],
  "possibleOffsets": [1, 2, 3, ...],
  "skillInference": {
    "13350000": {
      "offset1": 13350001,
      "offset2": 13350002
    }
  }
}
```

### Settings

Settings are stored in `settings.properties`:
- Network interface selection
- Server IP/port (auto-detected)
- Character name filter
- Target selection mode
- Debug logging options
- Hotkey bindings

---

## 📖 Documentation

For detailed technical information and guides:

- **[Architecture Documentation](docs/ARCHITECTURE.md)** - Complete technical architecture analysis
  - System architecture overview with diagrams
  - Technology stack and dependencies
  - Core components deep dive
  - Data flow and packet processing pipeline
  - Thread safety and concurrency mechanisms
  - Performance optimizations
  - Security considerations

- **[All Documentation](docs/)** - Browse all available documentation

---

## 🤝 Contributing

Contributions are welcome! This is a community-driven project.

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test thoroughly
5. Commit with clear messages (`git commit -m 'feat: add amazing feature'`)
6. Push to your branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Development Guidelines

- Follow existing code style (Kotlin conventions)
- Add comments for complex logic
- Test thread safety for concurrent code
- Update skill_codes.json for new skills
- Update README for new features

---

## 📋 Refactoring Summary

This version includes major improvements over the original:

### Phase 1: Stability (Critical Fixes)
1. ✅ **DataStorage thread safety** - HashMap → ConcurrentHashMap (3 race conditions fixed)
2. ✅ **PersonalData rolling window** - Synchronized blocks with iterator pattern
3. ✅ **Current target tracking** - AtomicInteger for lock-free atomicity

### Phase 2: Maintainability (Code Quality)
4. ✅ **DamagePacketParser extracted** - 205 lines, reduced StreamProcessor by 134 lines
5. ✅ **NameResolver extracted** - 340 lines, reduced StreamProcessor by 290 lines
6. ✅ **SummonTracker extracted** - 150 lines, reduced StreamProcessor by 47 lines
7. ✅ **Skill codes externalized** - 391 skills in JSON, reduced DpsCalculator by 675 lines
8. ✅ **Logging consolidated** - UnifiedLogger, eliminated 250+ duplicate lines

### Impact
- 🔒 **Thread-safety**: 100% of shared state now properly protected
- 📉 **Code reduction**: ~1100+ lines removed through deduplication
- 🏗️ **Modularity**: StreamProcessor reduced from 1009→400 lines (split into 4 classes)
- 📝 **Maintainability**: DpsCalculator reduced from 1100→280 lines
- ⚡ **Performance**: 67% reduction in background threads (3→1)
- ✅ **All changes backward compatible**

---

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details.

Original work by [TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
Refactored version by SpecTruM

**Note:** While this license permits commercial use, we kindly request that users consider contributing improvements back to the community rather than selling this software commercially. This is a community-driven project built with ❤️ for AION 2 players.

---

## ⚠️ Disclaimer

This tool is for **personal use and educational purposes only**.

- Use at your own risk
- The developer is not responsible for any consequences from using this tool
- This project may be paused or made private if requested by game operators
- Respect the game's Terms of Service

---

## 🙏 Acknowledgments

- **TK-open-public** - Original Aion2-Dps-Meter project
- **taengu** - Continued development and improvements
- **AION 2 Community** - Testing and feedback
- **Pcap4J Contributors** - Network capture library
- **JavaFX Team** - UI framework

---

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Discord**: https://discord.gg/Aion2Global
- **Documentation**: [docs/](docs/) folder - Technical guides and architecture

---

**Made with ❤️ for the AION 2 community**
