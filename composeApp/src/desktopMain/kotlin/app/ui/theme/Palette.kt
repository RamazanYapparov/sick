package app.ui.theme

import androidx.compose.ui.graphics.Color

object Palette {
    val AccentBlue = Color(0xFF235A73)
    val AccentGold = Color(0xFFE7C98B)
    val AccentYellow = Color(0xFFE8B23A)
    val DarkSurface = Color(0xFF18313C)
    val Success = Color(0xFF2E7D32)
    val Error = Color(0xFFB3261E)

    // Dark theme Material colors (used via MaterialTheme.colors)
    val ThemeBackground = Color(0xFF0E1A21)
    val ThemeSurface = Color(0xFF162C36)
    val ThemeOnSurface = Color(0xFFF7F4ED)
    val ThemeOnBackground = Color(0xFFF7F4ED)

    // Shared display accent colors (different from host accent)
    val SharedPrimary = Color(0xFF184A45)
    val SharedSecondary = Color(0xFFE5B14C)

    // Text colors
    val SecondaryText = Color(0xFF8899AA)
    val InfoText = Color(0xFFB0C4DE)
    val ConfirmedText = Color(0xFFAAAAAA)
    val TimerColor = Color(0xFFF36C5B)
    val MutedText = Color(0xFF7A9BAA)
    val ActivePlayerText = Color(0xFF1B1B1B)

    // Divider & border
    val DividerColor = Color(0x335F7D8D)
    val BorderColor = Color(0xFFB7AA93)

    // Backgrounds
    val PanelBackground = Color(0xFF1A2B35)
    val ButtonAccent = Color(0xFF3A8AAA)

    // Question board
    val ThemeCardBg = Color(0xFFE9DDBE)
    val ThemeNameColor = Color(0xFF2B2B2B)
    val QuestionButtonColor = Color(0xFFB3AA9E)
    val QuestionButtonDisabled = Color(0xFF8B8378)

    // Select answer colors
    val SelectRowBg = Color(0x225F7D8D)
    val SelectLetterBg = Color(0x335F7D8D)

    // Round badge
    val FinalBadge = Color(0xFFFF6B35)

    // Media player
    val MediaBg = Color(0xFF1A2A35)
    val MediaTrack = Color(0xFF0D1C24)
    val VideoOverlay = Color(0xAA000000)
    val ProgressTrack = Color(0x44FFFFFF)

    // Button states
    val ButtonDisabledContent = Color(0xFFA0C0D0)

    // Nested card & chip backgrounds
    val NestedCardBg = Color(0xFF1E2D3A)
    val SubtleText = Color(0xFFC8D0DC)
    val ChipBg = Color(0xFF2A3A47)
    val ChipText = Color(0xFF7AB4D0)

    // Player state background colors
    val PlayerFailed = Color(0xFF4A1C24)
    val PlayerSkipped = Color(0xFF555555)
    val PlayerAnswering = Color(0xFF1E4D2B)

    // Player state text colors
    val PlayerFailedText = Color(0xFFCC6666)
    val PlayerAnsweringText = Color(0xFF5CCD8F)
}
