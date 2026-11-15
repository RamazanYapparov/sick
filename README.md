# sick

Desktop quiz game host for SIQ question packs.

## Modules

- `composeApp` - desktop host UI and shared display UI
- `core` - game engine, models, scoring, timer
- `siq` - SIQ pack extraction and mapping into domain models
- `siq/xml` - raw XML parsing models
- `server` - embedded HTTP server for the player buzzer page

## Requirements

- JDK 21

## Run

Start the desktop app:

```bash
./gradlew :composeApp:run
```

On Windows:

```powershell
.\gradlew.bat :composeApp:run
```

The app starts:

- a `host` window for the operator
- a separate `display` window for the shared screen
- an embedded buzzer server at `http://localhost:8080`

## Basic Host Flow

1. Start the app.
2. In the host window, click `Load Pack` and choose a `.siq` file.
3. Add players in the `Players` section.
4. Open the buzzer page on player phones:
   `http://<host-machine-ip>:8080`
5. Click `Start Game`.
6. Choose the active player.
7. Let that player choose a question.
8. Use the host controls to:
   - choose the answering player manually if needed
   - mark answers `Correct` or `Wrong`
   - `Skip Question`
   - adjust scores manually
   - advance to the next round

## Notes

- v1 supports regular questions only.
- The host window shows the right answer for judging.
- The shared display currently does not have a dedicated answer-reveal phase yet.

## Build

Build all modules:

```bash
./gradlew build
```

Build only the desktop app:

```bash
./gradlew :composeApp:build
```

Run core tests:

```bash
./gradlew :core:test
```

## Portable Windows exe

```powershell
.\gradlew.bat :composeApp:packagePortableExe
```

This writes `composeApp\build\compose\binaries\main\portable\sick.exe`, one file with the app, its Java runtime and libVLC inside. It needs nothing installed. The first start of a build unpacks it to `%LOCALAPPDATA%\sick\<build id>`. Later starts reuse that folder, and folders left by older builds are deleted. Logs go to `%LOCALAPPDATA%\sick\logs`.

On Windows, Gradle downloads and checks a pinned libVLC build (`vlcVersion` in `composeApp/build.gradle.kts`). The app loads that libVLC in `run` and in packaged builds, not an installed VLC.

## License

sick is licensed under the GNU General Public License, version 3 (`LICENSE`). Packaged builds include libVLC, vlcj and other third-party components. Their licences and source code links are in `THIRD-PARTY-NOTICES.md`, which goes into every package together with `LICENSE` and VLC's `COPYING.txt`. A release that ships `sick.exe` must come with this repository's source code at the release tag. GitHub's release page attaches it automatically.
