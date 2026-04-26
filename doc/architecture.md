# Architecture

## Overview

Event-driven quiz game. Host runs desktop app (Compose), players connect via phone browser (web UI with buzzer button). All local network.

## Module Structure

```
composeApp/  → Compose Multiplatform desktop UI (host)
core/        → Game logic: models, engine, events, state
siq/         → SIQ file parsing → core domain models
siq/xml/     → Jackson-annotated Java POJOs for raw XML
```

Dependency chain: `composeApp` → `core` ← `siq` → `siq/xml`

## Core Module

### Data Flow

```
GameEvent (from UI / network)
    ↓
GameEngine.process(event)
    ├── validateEventForPhase()  — reject invalid events for current phase
    ├── applyEvent()             — pure state transformation
    ├── nextPhase()              — determine new GamePhase
    └── notifyListeners()        — broadcast (GameState, GamePhase)
         ↓
    UI update / WebSocket broadcast to players
```

### Game Phases & Transitions

```
Lobby ──[StartGame]──→ ChoosingPlayer
ChoosingPlayer ──[SelectActivePlayer]──→ ChoosingQuestion
ChoosingQuestion ──[QuestionSelected]──→ ShowingQuestion
ShowingQuestion ──[PlayerBuzzed]──→ PlayerAnswering
ShowingQuestion ──[TimerExpired]──→ ChoosingPlayer
PlayerAnswering ──[HostAccepted]──→ ChoosingQuestion | RoundEnd | GameOver
PlayerAnswering ──[HostRejected]──→ ShowingQuestion
RoundEnd ──[NextRound]──→ ChoosingPlayer | GameOver
```

### Key Files

| File | Purpose |
|------|---------|
| `model/game.kt` | `GameState` — immutable snapshot of entire game |
| `model/player.kt` | `Player` — immutable, identified by UUID |
| `model/question.kt` | `Question<T>` with type variants: Simple, Stake, NoRisk, Secret |
| `event/Events.kt` | Sealed `GameEvent` hierarchy (12 events) |
| `state/States.kt` | `GamePhase` sealed class (7 phases) |
| `engine/GameEngine.kt` | Central coordinator — processes events, manages state + phase |
| `engine/GameTimer.kt` | Coroutine-based countdown, fires TimerTick/TimerExpired |
| `service/ScoreCalculator.kt` | `Player.addScore()` / `Player.subtractScore()` extensions |
| `service/PlayerOps.kt` | `GameState.addPlayer()` / `.removePlayer()` / etc. extensions |

### Events

| Event | Source | Phase |
|-------|--------|-------|
| `PlayerJoined(name)` | Player phone | Lobby |
| `PlayerLeft(playerId)` | Player phone | Lobby |
| `PlayerRenamed(playerId, newName)` | Player phone | Lobby |
| `StartGame` | Host | Lobby |
| `SelectActivePlayer(playerId)` | Host | ChoosingPlayer |
| `QuestionSelected(questionId)` | Active player | ChoosingQuestion |
| `PlayerBuzzed(playerId)` | Player phone | ShowingQuestion |
| `TimerTick` | GameTimer | ShowingQuestion |
| `TimerExpired` | GameTimer | ShowingQuestion |
| `HostAccepted` | Host | PlayerAnswering |
| `HostRejected` | Host | PlayerAnswering |
| `NextRound` | Host | RoundEnd |

### Immutability

All models are immutable (`val` fields). State changes produce new `GameState` instances via `copy()`. `GameEngine` holds the mutable reference internally and exposes read-only `state` and `phase` properties.

### Networking (planned)

Host app runs embedded HTTP/WebSocket server (Ktor). Players connect via phone browser.
- Player → server: JSON event (buzz, join)
- Server → `GameEngine.process(event)`
- Engine listener → serialize `GameState` + `GamePhase` → broadcast to all clients
- Host UI is another client with elevated privileges (can send HostAccepted, HostRejected, SelectActivePlayer, StartGame)
