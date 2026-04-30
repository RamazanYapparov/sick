## 1. Create Subdirectories

- [x] 1.1 Create `app/ui/window/`, `app/ui/media/`, `app/ui/components/`, `app/ui/theme/` under `composeApp/src/desktopMain/kotlin/`

## 2. Move theme/Palette.kt

- [x] 2.1 Update `package app.ui` → `package app.ui.theme` in `Palette.kt`
- [x] 2.2 Move file to `app/ui/theme/Palette.kt`

## 3. Move media files

- [x] 3.1 Update package + add `import app.ui.theme.Palette` in `AudioPlayer.kt`, move to `app/ui/media/`
- [x] 3.2 Update package + add `import app.ui.theme.Palette` in `VideoPlayer.kt`, move to `app/ui/media/`

## 4. Move components files

- [x] 4.1 Update package + add `import app.ui.theme.Palette` in `PlayerComponents.kt`, move to `app/ui/components/`
- [x] 4.2 Update package + add `import app.ui.theme.Palette` in `QuestionBoard.kt`, move to `app/ui/components/`
- [x] 4.3 Update package + add `import app.ui.theme.Palette` in `Scoreboard.kt`, move to `app/ui/components/`
- [x] 4.4 Update package in `SectionCard.kt` (no new imports needed), move to `app/ui/components/`
- [x] 4.5 Update package + add `import app.ui.theme.Palette` in `PhaseControls.kt` (SectionCard/PlayerChipRow/QuestionBoard stay in same package), move to `app/ui/components/`

## 5. Move window files

- [x] 5.1 Update package + add cross-package imports in `HostWindow.kt`:
  - `import app.ui.components.SectionCard`
  - `import app.ui.components.PlayerEditorRow`
  - `import app.ui.components.PhaseControls`
  - `import app.ui.components.HostAnswerCard`
  - `import app.ui.theme.Palette`
  - Move to `app/ui/window/`
- [x] 5.2 Update package + add cross-package imports in `SharedDisplay.kt`:
  - `import app.ui.components.QuestionBoard`
  - `import app.ui.components.Scoreboard`
  - `import app.ui.theme.Palette`
  - `import app.ui.media.AudioPlayer`
  - `import app.ui.media.VideoPlayer`
  - Move to `app/ui/window/`

## 6. Update external callers

- [x] 6.1 Update `App.kt` imports: replace `app.ui.HostWindowContent`, `app.ui.SharedDisplayScreen`, `app.ui.Palette` with `app.ui.window.*` and `app.ui.theme.Palette`

## 7. Verify

- [x] 7.1 Run `./gradlew :composeApp:build` — confirm zero compilation errors
