## ADDED Requirements

### Requirement: macOS uses native file dialog
On macOS the file picker SHALL use `java.awt.FileDialog` backed by `NSOpenPanel`, filtered to `.siq` files.

#### Scenario: User selects a file on macOS
- **WHEN** `pickSiqFile()` is called on macOS
- **THEN** the native macOS file dialog opens, and selecting a `.siq` file returns its `Path`

#### Scenario: User cancels on macOS
- **WHEN** `pickSiqFile()` is called on macOS and the user closes the dialog without selecting
- **THEN** `null` is returned

### Requirement: Windows uses modern native file dialog
On Windows the file picker SHALL invoke `System.Windows.Forms.OpenFileDialog` via PowerShell, producing the modern Windows Explorer–style dialog filtered to `.siq` files.

#### Scenario: User selects a file on Windows
- **WHEN** `pickSiqFile()` is called on Windows
- **THEN** the modern Windows file picker opens and selecting a `.siq` file returns its `Path`

#### Scenario: User cancels on Windows
- **WHEN** `pickSiqFile()` is called on Windows and the user closes the dialog without selecting
- **THEN** `null` is returned

#### Scenario: PowerShell is unavailable or blocked on Windows
- **WHEN** the PowerShell subprocess exits with a non-zero code or produces no output
- **THEN** `null` is returned and no exception propagates to the caller

### Requirement: Linux uses kdialog or zenity for native dialog
On Linux the file picker SHALL invoke `kdialog --getopenfilename` (KDE) if available, falling back to `zenity --file-selection` (GNOME/other), and finally to `javax.swing.JFileChooser` if neither tool is found. All paths are filtered to `.siq` files.

#### Scenario: User selects a file on KDE Plasma
- **WHEN** `pickSiqFile()` is called on a KDE Plasma desktop
- **THEN** the native KDE file picker opens (no black screen) and selecting a `.siq` file returns its `Path`

#### Scenario: User selects a file on GNOME or other GTK environment
- **WHEN** `pickSiqFile()` is called and `kdialog` is absent but `zenity` is present
- **THEN** the Zenity GTK file picker opens and selecting a `.siq` file returns its `Path`

#### Scenario: Neither kdialog nor zenity is available on Linux
- **WHEN** `pickSiqFile()` is called and neither `kdialog` nor `zenity` is found on PATH
- **THEN** a `JFileChooser` dialog opens and selecting a `.siq` file returns its `Path`

#### Scenario: User cancels on Linux
- **WHEN** `pickSiqFile()` is called on Linux and the user closes the dialog without selecting
- **THEN** `null` is returned

### Requirement: Caller API is unchanged
The function signature `pickSiqFile(): Path?` SHALL remain identical so that no call sites require modification.

#### Scenario: Existing callers compile without change
- **WHEN** `pickSiqFile()` is rewritten with platform dispatch
- **THEN** all existing call sites continue to compile and behave identically
