# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all modules
./gradlew build

# Run the desktop application
./gradlew :composeApp:run

# Run tests for a specific module
./gradlew :core:test
./gradlew :siq:test

# Run a single test class
./gradlew :core:test --tests "fully.qualified.TestClassName"

# Package desktop app
./gradlew :composeApp:packageMsi        # Windows
./gradlew :composeApp:packageDmg        # macOS
./gradlew :composeApp:packageDeb        # Linux
```

## Architecture

This is a Kotlin Multiplatform desktop quiz game engine that reads SIQ packages (a ZIP-based XML format for quiz questions, popular in Russian quiz games).

### Module Structure

```
sick/
├── composeApp/    # Compose Multiplatform desktop UI
├── core/          # Domain models, services, state machine
├── siq/           # SIQ file reading and domain mapping
└── siq/xml/       # Jackson-annotated Java POJOs for raw XML deserialization
```

**Dependency chain**: `composeApp` → `core` ← `siq` → `siq/xml`

### Data Flow

SIQ file (ZIP archive) → `SiqExtractor` (unzip) → `Parser.java` (Jackson XML) → `siq/xml` POJOs → mappers in `siq/mapper/` → `core` domain models → game services & state machine → Compose UI

### Key Layers

**`siq/xml/`** — Java POJOs with Jackson annotations (`@JacksonXmlRootElement`, etc.) modeling the raw SIQ XML structure. Parsed by `Parser.java` using `XmlMapper`.

**`siq/`** — Kotlin layer that extracts SIQ archives (`SiqExtractor`), reads them (`SiqReader`), and maps XML models to domain models (`mapper/package.kt`, `mapper/question.kt`).

**`core/model/`** — Domain model: `Package`, `Round`, `Theme`, `Question` (variants: `Simple`, `Stake`, `NoRisk`, `Secret`), `Player`, `Game`.

**`core/service/`** — Pure functions/services for game operations: creating games, managing players (add/remove/rename), updating scores (addFull, addHalf, addDouble, subtractFull, subtractHalf, subtractDouble).

**`core/state/`** — Game state machine using `kstatemachine`. States: `ChoosePlayerToStart`, `ChooseQuestion`, `PlayingQuestion<T>`, `AnsweringQuestion<T>`.

**`composeApp/`** — Compose Multiplatform desktop UI. Entry point: `main.kt` → `App.kt`.

### Key Dependencies

- `arrow-kt` — Functional programming (Either, Option, etc.) used in services
- `kstatemachine` — State machine for game flow
- `jackson-dataformat-xml` + `jackson-module-kotlin` — SIQ XML parsing
- `compose-multiplatform` — Desktop UI
