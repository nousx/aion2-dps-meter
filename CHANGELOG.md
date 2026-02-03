# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-03

### 🎉 First Stable Release

This is the first stable release of the refactored Aion 2 DPS Meter, forked from [taengu/Aion2-Dps-Meter](https://github.com/taengu/Aion2-Dps-Meter) with major improvements in code quality, thread safety, and maintainability.

### ✨ Added

#### Core Features
- Real-time DPS tracking with 100ms update cycle
- Skill analysis with detailed breakdowns
- Party member rankings with contribution percentages
- Burst DPS calculation using 5-second sliding window
- Specialty slot detection (1-5 slots)
- Target switching support (most damage, most recent, all targets)
- Combat time tracking for accurate DPS calculations

#### Technical Features
- Thread-safe architecture with ConcurrentHashMap
- Transparent JavaFX overlay with always-on-top window
- Multi-language support (EN, KO, ZH-Hans, ZH-Hant)
- Global hotkeys for toggle visibility and reset
- Debug logging and packet capture for troubleshooting
- JSON-based skill configuration (391 skills)

#### Documentation
- Comprehensive [Architecture Documentation](docs/ARCHITECTURE.md)
- Detailed README with installation and usage guides
- Contributing guidelines

### 🔧 Changed

#### Phase 1: Thread Safety & Stability
- **Fixed 3 HIGH severity race conditions**
  - DataStorage: HashMap → ConcurrentHashMap
  - PersonalData rolling window: Added synchronized blocks with iterator pattern
  - Current target tracking: Migrated to AtomicInteger for lock-free atomicity

#### Phase 2: Code Quality & Maintainability
- **Extracted specialized parsers** (~1100+ lines reduced)
  - DamagePacketParser: 205 lines (reduced StreamProcessor by 134 lines)
  - NameResolver: 340 lines (reduced StreamProcessor by 290 lines)
  - SummonTracker: 150 lines (reduced StreamProcessor by 47 lines)
  - StreamProcessor: Reduced from 1009 → 400 lines (60% reduction)

- **Externalized skill data**
  - 391 skills moved to `skill_codes.json`
  - DpsCalculator: Reduced from 1100 → 280 lines (75% reduction)
  - Editable skill database without recompilation

- **Unified logging system**
  - Consolidated 3 separate loggers into UnifiedLogger
  - 67% reduction in background threads (3 → 1)
  - Eliminated 250+ duplicate logging lines

### 🚀 Performance Improvements
- Zero boxing overhead for AtomicInteger operations
- Lock-free reads with ConcurrentHashMap (wait-free guarantees)
- Binary search O(log n) for skill inference (391 skills → 9 comparisons)
- BPF kernel-level packet filtering (80% reduction in userspace packets)
- Gradle build cache for faster compilation

### 📊 Impact Summary
- 🔒 **Thread-safety**: 100% of shared state now properly protected
- 📉 **Code reduction**: ~1100+ lines removed through deduplication
- 🏗️ **Modularity**: StreamProcessor split into 4 specialized classes
- 📝 **Maintainability**: DpsCalculator reduced by 75%
- ⚡ **Performance**: 67% reduction in background threads
- ✅ **Backward compatible**: All changes maintain compatibility

### 🛠️ Technical Stack
- Kotlin 1.9+
- JavaFX 21
- Pcap4J 1.8.2
- Kotlinx Coroutines 1.8.1
- Kotlinx Serialization 1.7.1
- JNA 5.14.0

### 📦 Installation Requirements
- Windows 10/11 (x64)
- Java 21+ (JDK or JRE)
- Npcap (WinPcap-compatible mode)
- Administrator privileges for packet capture

### 🙏 Credits
- **Original work**: [TK-open-public](https://github.com/TK-open-public/Aion2-Dps-Meter)
- **Continued development**: [taengu](https://github.com/taengu/Aion2-Dps-Meter)
- **Refactored version**: SpecTruM

---

## Version History

### [1.0.0] - 2026-02-03
- First stable release with major refactoring

### Previous Versions (Pre-fork)
- 0.1.5 and earlier: Original implementation by TK-open-public and taengu

[1.0.0]: https://github.com/nousx/aion2-dps-meter/releases/tag/v1.0.0
