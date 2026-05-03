## Context

`SiqFilePicker.kt` currently uses `java.awt.FileDialog` for all platforms. On macOS this delegates to the native Cocoa sheet and works perfectly. On Windows, AWT's `FileDialog` wraps the old Win32 `GetOpenFileName` API (COMDLG32), which renders as a legacy dialog rather than the modern Windows Explorer–style `IFileDialog` introduced in Vista. On Linux (tested: CachyOS + KDE Plasma), the AWT GTK integration layer crashes with a black/blank window — a known incompatibility between Java's AWT GTK backend and modern KDE/Wayland compositor stacks.

The caller signature is `pickSiqFile(): Path?` and nothing else needs to change; callers don't care how the path is obtained.

## Goals / Non-Goals

**Goals:**
- Show a native-looking file picker on macOS, Windows, and Linux
- Fix the black screen crash on KDE Plasma (and similar Linux desktop environments)
- Maintain the same `pickSiqFile(): Path?` API — zero caller changes
- No new library dependencies

**Non-Goals:**
- Multi-file selection (out of scope for this change)
- Remembering last-opened directory across sessions
- Supporting non-desktop (Android, iOS, web) targets

## Decisions

### Decision 1: Shell out to native OS tools on Windows and Linux instead of using AWT

**Chosen**: Spawn a subprocess to invoke the OS-native dialog.
- **Windows**: `powershell -command` running `System.Windows.Forms.OpenFileDialog`. PowerShell with WinForms is available on every modern Windows installation and produces the proper `IFileDialog` Explorer-style picker.
- **Linux**: Try `kdialog --getopenfilename` first (always present on KDE), then `zenity --file-selection` (GNOME/GTK environments), then fall back to `javax.swing.JFileChooser` as last resort.

**Alternatives considered**:
- *JNA/JNI to call `IFileDialog` on Windows*: Works but adds a native library dependency and significant complexity.
- *Continue using `FileDialog` on Windows*: The old Win32 dialog renders fine but looks dated (no left-side "Places" panel, no preview pane, old toolbar).
- *`JFileChooser` everywhere*: Pure Swing; looks like a generic Java app dialog on every platform — worse than the current state.
- *Fix AWT GTK on Linux*: Involves setting JVM flags (`-Dawt.useSystemAAFontSettings=off`, `-Dswing.aatext=false`, `GDK_BACKEND=x11`) at launch time, which is fragile and environment-specific.

### Decision 2: macOS stays on `java.awt.FileDialog`

`FileDialog` on macOS wraps the native `NSOpenPanel` and works flawlessly. No change needed.

### Decision 3: Subprocess approach — synchronous, blocking the calling thread

The existing `pickSiqFile()` call is already blocking (it blocks until the user dismisses the dialog). Subprocesses launched with `ProcessBuilder` can be waited on synchronously with the same semantics. The result is read from stdout.

### Decision 4: Platform detection via `System.getProperty("os.name")`

Standard, no-dependency approach used throughout the JVM ecosystem. Checked at call time (not at startup) since there is only one code path in the desktop target.

## Risks / Trade-offs

- **PowerShell execution policy**: Some corporate Windows environments block PowerShell scripts. The command uses `-NonInteractive` and runs inline (not a `.ps1` file), which bypasses most execution policy restrictions. If blocked, the picker returns `null` (no file selected).
- **`kdialog`/`zenity` absence on minimal Linux installs**: Handled by the `JFileChooser` fallback. Distros running neither KDE nor GNOME (e.g., Openbox, i3) will see the Swing picker, which is functional if not pretty.
- **Blocking the UI thread**: Same risk as current implementation — callers must ensure they invoke from a coroutine/thread that is allowed to block. No change in contract.
- **Subprocess PATH lookup**: `kdialog` and `zenity` must be on `PATH`. On CachyOS + KDE this is always the case.

## Migration Plan

1. Rewrite `SiqFilePicker.kt` with the platform-dispatching logic.
2. Manually test on macOS, Windows, and Linux (KDE Plasma).
3. No database migrations, no API changes, no feature flags needed.

**Rollback**: revert the single file — all other code is unchanged.

## Open Questions

_(none — approach is well-defined)_
