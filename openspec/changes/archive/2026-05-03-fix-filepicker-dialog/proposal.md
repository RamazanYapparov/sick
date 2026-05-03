## Why

The current `SiqFilePicker` uses `java.awt.FileDialog` which shows an outdated AWT-wrapped dialog on Windows (not the modern Windows file picker), and causes a black screen crash on Linux KDE Plasma (CachyOS, latest KDE). Users get a broken or visually inconsistent experience depending on OS.

## What Changes

- Replace the single `java.awt.FileDialog`-based implementation with a platform-dispatching file picker
- macOS: keep existing `FileDialog` behavior (already works natively)
- Windows: invoke the modern native file dialog via PowerShell's `System.Windows.Forms.OpenFileDialog` (bypasses AWT entirely, renders as a proper Windows Explorer dialog)
- Linux: invoke `kdialog --getopenfilename` (KDE) with fallback to `zenity --file-selection` (GNOME/other), and final fallback to `javax.swing.JFileChooser` if neither tool is available

## Capabilities

### New Capabilities

- `native-file-picker`: Platform-aware file picker that invokes the OS-native file dialog on macOS, Windows, and Linux instead of routing through AWT's cross-platform dialog

### Modified Capabilities

_(none — no existing specs change requirements)_

## Impact

- `composeApp/src/desktopMain/kotlin/app/session/SiqFilePicker.kt` — full rewrite
- No new library dependencies; Windows path uses bundled JDK + PowerShell (always present on Windows 10/11); Linux path shells out to `kdialog`/`zenity` (standard on KDE/GNOME)
- No changes to callers — `pickSiqFile(): Path?` signature stays the same
