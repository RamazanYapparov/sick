## MODIFIED Requirements

### Requirement: Linux uses kdialog or zenity for native dialog
On Linux the file picker SHALL invoke `kdialog --getopenfilename` (KDE) if available, falling back to `zenity --file-selection` (GNOME/other) when `kdialog` is not installed, and finally to `javax.swing.JFileChooser` when neither tool is installed. Fallthrough between tools SHALL be driven by tool **availability** only — a user cancellation in any picker SHALL terminate the chain immediately and return `null`. All paths are filtered to `.siq` files.

#### Scenario: User selects a file on KDE Plasma
- **WHEN** `pickSiqFile()` is called on a KDE Plasma desktop
- **THEN** the native KDE file picker opens (no black screen) and selecting a `.siq` file returns its `Path`

#### Scenario: User selects a file on GNOME or other GTK environment
- **WHEN** `pickSiqFile()` is called and `kdialog` is absent but `zenity` is present
- **THEN** the Zenity GTK file picker opens and selecting a `.siq` file returns its `Path`

#### Scenario: Neither kdialog nor zenity is available on Linux
- **WHEN** `pickSiqFile()` is called and neither `kdialog` nor `zenity` is found on PATH
- **THEN** a `JFileChooser` dialog opens and selecting a `.siq` file returns its `Path`

#### Scenario: User cancels kdialog
- **WHEN** `pickSiqFile()` is called on KDE Plasma and the user cancels the `kdialog` picker
- **THEN** `null` is returned and **no further picker** (zenity or Swing) is shown

#### Scenario: User cancels zenity
- **WHEN** `pickSiqFile()` is called, `kdialog` is unavailable, `zenity` opens, and the user cancels it
- **THEN** `null` is returned and **the Swing fallback is not shown**

#### Scenario: kdialog is unavailable, zenity is present
- **WHEN** `pickSiqFile()` is called and `kdialog` cannot be spawned (binary not on PATH)
- **THEN** the picker continues to `zenity` automatically

#### Scenario: User cancels Swing fallback
- **WHEN** `pickSiqFile()` is called, both `kdialog` and `zenity` are unavailable, the Swing dialog opens, and the user cancels it
- **THEN** `null` is returned
