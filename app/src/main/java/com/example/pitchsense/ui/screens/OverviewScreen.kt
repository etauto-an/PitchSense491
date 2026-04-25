package com.example.pitchsense.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pitchsense.data.model.StatItem
import com.example.pitchsense.ui.components.HeaderBar
import com.example.pitchsense.ui.components.OfflineBanner
import com.example.pitchsense.ui.components.StatCard
import java.text.Normalizer

/**
 * Metadata attached to each batter entry for display purposes.
 *
 * @param order 1-based lineup position; used to prefix the player name in the dropdown
 *              (e.g., "3. Vladimir Guerrero Jr.") so batting order remains visible.
 */
private data class BatterLineupMeta(
    val order: Int
)

/**
 * Role bucket for grouping pitchers in the dropdown.
 *
 * Pitchers are split into three sections so scouts can quickly navigate a long roster
 * without scrolling past irrelevant role types.
 *
 * @param header Section heading rendered as a disabled, bold dropdown row.
 * @param order  Sort key that keeps Starters → Relievers → Closers regardless of roster order.
 */
private enum class PitcherRole(val header: String, val order: Int) {
    STARTER("Starters", 0),
    RELIEVER("Relievers", 1),
    CLOSER("Closers", 2)
}

/**
 * Primary dashboard screen: batter/pitcher selector, top-level stat cards, and navigation
 * entry points to the three analysis screens.
 *
 * Layout adapts based on pitcher selection — a single centered "General" column when no
 * pitcher is chosen, or a two-column General + Pitcher Specific split when one is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    selectedBatter: String,
    selectedPitcher: String,
    batterOptions: List<String>,
    pitcherOptions: List<String>,
    generalStats: List<StatItem>,
    pitcherSpecificStats: List<StatItem>,
    isError: Boolean,
    isOffline: Boolean,
    onBatterSelected: (String) -> Unit,
    onPitcherSelected: (String) -> Unit,
    onNavigateToAdvancedStats: () -> Unit,
    onNavigateToHeatMap: () -> Unit,
    onNavigateToPitchSequence: () -> Unit
) {
    // Assign lineup-position metadata from list index; wrapped in remember so it is only
    // recomputed when the roster list itself changes, not on every recomposition.
    val batterMeta = remember(batterOptions) {
        batterOptions.mapIndexed { index, name ->
            val order = index + 1
            name to BatterLineupMeta(order = order)
        }.toMap()
    }
    val orderedBatterOptions = remember(batterOptions, batterMeta) {
        batterOptions.sortedBy { batterMeta[it]?.order ?: Int.MAX_VALUE }
    }
    // Hardcoded role assignments reflect the 2025 Dodgers roster used in the demo data.
    // Closers/Relievers are listed explicitly; everyone else defaults to Starter.
    val pitcherRoleByName = remember(pitcherOptions) {
        pitcherOptions.associateWith { name ->
            when (name) {
                "Edwin Díaz" -> PitcherRole.CLOSER
                "Tanner Scott", "Blake Treinen", "Alex Vesia", "Brusdar Graterol", "Brock Stewart", "Ben Casparius", "Jack Dreyer", "Paul Gervase", "Edgardo Henriquez", "Kyle Hurt", "Will Klein", "Ronan Kopp" -> PitcherRole.RELIEVER
                else -> PitcherRole.STARTER
            }
        }
    }
    val orderedPitcherOptions = remember(pitcherOptions, pitcherRoleByName) {
        pitcherOptions.sortedWith(
            compareBy<String> { pitcherRoleByName[it]?.order ?: Int.MAX_VALUE }
                // Preserve original order within each role bucket.
                .thenBy { pitcherOptions.indexOf(it) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        HeaderBar(title = "PitchSense: Analyze Batter")
        // Amber banner shown whenever data is sourced from local demo fallback.
        if (isOffline) OfflineBanner()

        Column(modifier = Modifier.padding(24.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerHeadshot(playerName = selectedBatter, rolePrefix = "batter")
                    Spacer(modifier = Modifier.height(8.dp))
                    BatterDropdown(
                        label = "Choose Batter (Required)",
                        options = orderedBatterOptions,
                        meta = batterMeta,
                        selectedOption = selectedBatter,
                        onOptionSelected = onBatterSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    PlayerHeadshot(playerName = selectedPitcher, rolePrefix = "pitcher")
                    Spacer(modifier = Modifier.height(8.dp))
                    PitcherDropdown(
                        label = "Choose Pitcher (Optional)",
                        options = orderedPitcherOptions,
                        roleByName = pitcherRoleByName,
                        selectedOption = selectedPitcher,
                        onOptionSelected = onPitcherSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isError) {
                Text(
                    text = "Unable to load stats. Check that the backend is running.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (selectedPitcher.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    StatsDataColumn(
                        title = "General",
                        subtitle = "$selectedBatter — Season Overview",
                        stats = generalStats,
                        modifier = Modifier.fillMaxWidth(0.48f)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatsDataColumn(
                        title = "General",
                        subtitle = "$selectedBatter — Season Overview",
                        stats = generalStats,
                        modifier = Modifier.weight(1f)
                    )
                    StatsDataColumn(
                        title = "Pitcher Specific",
                        subtitle = "$selectedBatter vs. $selectedPitcher",
                        stats = pitcherSpecificStats,
                        emptyMessage = "No matchup data available for this batter and pitcher.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onNavigateToAdvancedStats, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Advanced Stats", style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = onNavigateToHeatMap, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Batter Heat Maps", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onNavigateToPitchSequence, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Recommend Pitch Sequence", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Pitcher selector with role-based section headers (Starters / Relievers / Closers).
 *
 * Section headers are rendered as disabled, non-clickable rows so they act as visual
 * separators without being selectable options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PitcherDropdown(
    label: String,
    options: List<String>,
    roleByName: Map<String, PitcherRole>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
            scrollState = rememberScrollState()
        ) {
            // Render grouped sections: starters, relievers, then closer.
            PitcherRole.entries.sortedBy { it.order }.forEach { role ->
                val pitchersInRole = options.filter { roleByName[it] == role }
                if (pitchersInRole.isNotEmpty()) {
                    DropdownMenuItem(
                        enabled = false,
                        text = {
                            Text(
                                text = role.header,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF233B90)
                            )
                        },
                        onClick = {}
                    )

                    pitchersInRole.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Batter selector that prefixes each name with its lineup position (e.g., "3. Vlad Guerrero Jr.").
 *
 * The position prefix is shown in both the collapsed field and the dropdown list so
 * the batting order stays visible without needing a separate column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatterDropdown(
    label: String,
    options: List<String>,
    meta: Map<String, BatterLineupMeta>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMeta = meta[selectedOption]
    // Include lineup number in visible value when metadata is present.
    val selectedDisplay = if (selectedMeta == null) {
        selectedOption
    } else {
        "${selectedMeta.order}. $selectedOption"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
            scrollState = rememberScrollState()
        ) {
            options.forEach { option ->
                val optionMeta = meta[option]
                val displayText = if (optionMeta == null) {
                    option
                } else {
                    "${optionMeta.order}. $option"
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Circular player headshot resolved from a drawable asset, with an initials avatar fallback.
 *
 * @param playerName  Full display name; converted to a resource slug for asset lookup.
 * @param rolePrefix  Either `"batter"` or `"pitcher"`, prepended to the slug so assets
 *                    for the same player name don't collide across roles.
 */
@Composable
private fun PlayerHeadshot(playerName: String, rolePrefix: String) {
    val context = LocalContext.current
    // Resolve drawable id from a normalized player-name slug.
    val resId = remember(playerName, rolePrefix) {
        context.playerHeadshotResId(rolePrefix = rolePrefix, playerName = playerName)
    }

    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "$playerName headshot",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback avatar if no matching headshot asset exists.
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFE7EDF9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playerName.take(2).uppercase().ifBlank { "--" },
                color = Color(0xFF233B90),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Resolves the drawable resource ID for a player headshot using a normalized name slug.
 *
 * NFD decomposition strips diacritics before slug generation (e.g., "Díaz" → "diaz")
 * because Android resource identifiers must be ASCII. Returns 0 if no matching asset exists,
 * which the caller treats as a signal to render the initials fallback.
 */
private fun Context.playerHeadshotResId(rolePrefix: String, playerName: String): Int {
    // Decompose Unicode then strip combining marks so accented characters become plain ASCII.
    val normalized = Normalizer.normalize(playerName, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    val slug = normalized
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return resources.getIdentifier("${rolePrefix}_$slug", "drawable", packageName)
}

/**
 * Generic unsectioned player dropdown for simple single-list selections.
 *
 * Kept as a public utility in case a future screen needs a flat selector without
 * the role-grouping or lineup-order metadata used by [BatterDropdown]/[PitcherDropdown].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
            scrollState = rememberScrollState()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Titled column of [StatCard] rows used for the General and Pitcher Specific stat groups. */
@Composable
private fun StatsDataColumn(
    title: String,
    subtitle: String,
    stats: List<StatItem>,
    emptyMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        if (stats.isEmpty() && emptyMessage != null) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            return
        }

        stats.forEach { stat ->
            StatCard(
                title = stat.title,
                value = stat.value,
                isPrimary = stat.isPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
