## 1. Investigate current behavior

- [x] 1.1 Read `SiqFilePicker.kt` and note all call sites
- [x] 1.2 Confirm `java.awt.FileDialog` is the only place a file dialog is opened in the project

## 2. Implement platform-dispatching file picker

- [x] 2.1 Rewrite `SiqFilePicker.kt` with an `os.name`-based platform dispatch (`mac`, `win`, everything else = Linux)
- [x] 2.2 Keep macOS path: `java.awt.FileDialog` filtered to `.siq` (no change from current)
- [x] 2.3 Add Windows path: spawn `powershell -NonInteractive -Command` running `System.Windows.Forms.OpenFileDialog`, read selected path from stdout, return `null` on empty/error output
- [x] 2.4 Add Linux primary path: attempt `kdialog --getopenfilename . "*.siq"` via `ProcessBuilder`; parse stdout for the returned path
- [x] 2.5 Add Linux secondary path: if `kdialog` is not found (IOException or exit code ≠ 0), attempt `zenity --file-selection --file-filter=*.siq`; parse stdout
- [x] 2.6 Add Linux fallback: if neither `kdialog` nor `zenity` works, use `javax.swing.JFileChooser` with a `.siq` `FileNameExtensionFilter`

## 3. Verify callers

- [x] 3.1 Confirm `pickSiqFile(): Path?` signature is unchanged
- [x] 3.2 Build the project (`./gradlew :composeApp:build`) and verify zero compilation errors

## 4. Manual testing

- [ ] 4.1 Test on macOS: open dialog, select a `.siq` file, confirm it loads; cancel and confirm `null` is returned
- [ ] 4.2 Test on Windows: confirm the modern Explorer-style picker appears (not the old COMDLG32 dialog)
- [ ] 4.3 Test on Linux KDE Plasma (CachyOS): confirm no black screen, `kdialog` picker opens, file loads correctly
- [ ] 4.4 (Optional) Test Linux fallback: temporarily rename `kdialog` and verify `zenity` path kicks in
