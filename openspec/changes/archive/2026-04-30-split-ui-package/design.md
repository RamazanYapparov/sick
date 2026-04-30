## Context

`app.ui` flat package has 10 files: 2 window entry-points (HostWindow, SharedDisplay), 2 media players (AudioPlayer, VideoPlayer), 5 game components (PhaseControls, PlayerComponents, QuestionBoard, Scoreboard, SectionCard), and 1 theme object (Palette). All currently share `package app.ui` with no import statements between them.

## Goals / Non-Goals

**Goals:**
- Organize into 4 subpackages: `window`, `media`, `components`, `theme`
- Zero behavior or API change
- All existing call-sites continue to compile

**Non-Goals:**
- Renaming composables or functions
- Moving files outside `composeApp`
- Splitting files further

## Decisions

**Grouping strategy**: `window` (entry-points) / `media` (players) / `components` (game UI) / `theme` (Palette) — chosen over a finer-grained split (game/player/controls) to avoid over-engineering at this scale.

**Palette placement**: `theme` subpackage rather than root, since it's a pure constant object with no composables and is referenced by every other subpackage. Keeping it separate avoids circular dependency ambiguity.

## Risks / Trade-offs

- Cross-package import count increases: ~10 new import lines total — low risk, mechanical change.
- External callers (App.kt, display window) need import updates — must grep for all `app.ui` usages first.

## Migration Plan

1. Create 4 subdirectories under `app/ui/`
2. Move each file, update `package` declaration
3. Add cross-package `import` lines in each affected file
4. Find and update all external callers
5. Build to verify — no tests needed (pure rename)
