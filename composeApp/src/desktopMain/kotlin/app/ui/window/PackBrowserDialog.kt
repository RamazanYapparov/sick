@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.ui.window

import app.session.ScannedPackInfo
import app.session.ScannedRoundInfo
import app.session.ScannedThemeInfo
import app.ui.theme.Palette
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.awt.Toolkit
import java.nio.file.Path

@Composable
internal fun PackBrowserDialog(
    scannedPacks: List<ScannedPackInfo>,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onBrowseFiles: () -> Unit,
    onChoosePack: (Path) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.size(width = (Toolkit.getDefaultToolkit().screenSize.width * 0.8f).dp, height = 520.dp),
            backgroundColor = Palette.ThemeSurface,
            shape = RoundedCornerShape(20.dp),
            elevation = 12.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.AccentBlue, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Browse Local Packs",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                "~${System.getProperty("user.home")}/Downloads",
                                fontSize = 11.sp,
                                color = Palette.InfoText,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRefresh,
                                enabled = !isScanning,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Palette.ButtonAccent,
                                    contentColor = Color.White,
                                    disabledBackgroundColor = Palette.ButtonAccent.copy(alpha = 0.6f),
                                    disabledContentColor = Palette.ButtonDisabledContent,
                                ),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text("Refresh", fontSize = 13.sp)
                            }
                            Button(
                                onClick = onBrowseFiles,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Palette.ButtonAccent,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "Browse Files",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Browse Files...", fontSize = 13.sp)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }

                // Content
                if (scannedPacks.isEmpty() && !isScanning) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No packs found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Palette.ThemeOnSurface.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Place .siq files in ~/Downloads or use Browse Files to locate one.",
                                fontSize = 13.sp,
                                color = Palette.ThemeOnSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(scannedPacks, key = { it.path.toString() }) { pack ->
                            PackRow(
                                pack = pack,
                                onChoose = { onChoosePack(pack.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(
    pack: ScannedPackInfo,
    onChoose: () -> Unit,
) {
    var peekExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        backgroundColor = Palette.DarkSurface,
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pack.packName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.ThemeOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pack.author.isNotBlank()) {
                        Text(
                            "by ${pack.author}",
                            fontSize = 12.sp,
                            color = Palette.SecondaryText,
                        )
                    }
                    Text(
                        "${pack.rounds.size} round(s), ${pack.rounds.sumOf { r -> r.themes.sumOf { t -> t.questions.size } }} questions",
                        fontSize = 11.sp,
                        color = Palette.SecondaryText,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { peekExpanded = !peekExpanded },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Palette.AccentBlue,
                        ),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text(if (peekExpanded) "Hide" else "Peek", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onChoose,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Palette.AccentBlue,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("Choose", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Peek expanded content
            AnimatedVisibility(visible = peekExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pack.rounds.forEach { round ->
                        PeekRoundSection(round = round)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeekRoundSection(round: ScannedRoundInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Palette.NestedCardBg,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundTypeBadge(type = round.type)
                Spacer(Modifier.width(6.dp))
                Text(
                    round.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.ThemeOnSurface,
                )
            }
            Spacer(Modifier.height(6.dp))
            round.themes.forEach { theme ->
                PeekThemeSection(theme = theme)
            }
        }
    }
}

@Composable
private fun PeekThemeSection(theme: ScannedThemeInfo) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Palette.AccentGold, RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                theme.name,
                fontSize = 13.sp,
                color = Palette.SubtleText,
            )
        }
        FlowRow(
            modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            theme.questions.forEach { q ->
                QuestionPriceChip(price = q.price)
            }
        }
    }
}

@Composable
private fun QuestionPriceChip(price: Int) {
    Box(
        modifier = Modifier
            .background(
                color = Palette.ChipBg,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            "$price",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Palette.ChipText,
        )
    }
}

@Composable
private fun RoundTypeBadge(type: String) {
    val label = when (type.lowercase()) {
        "final" -> "Final"
        else -> "Simple"
    }
    val bgColor = when (type.lowercase()) {
        "final" -> Palette.FinalBadge.copy(alpha = 0.2f)
        else -> Palette.AccentBlue.copy(alpha = 0.15f)
    }
    val textColor = when (type.lowercase()) {
        "final" -> Palette.FinalBadge
        else -> Palette.AccentBlue
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
