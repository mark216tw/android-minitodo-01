package com.vibecode.minitodo.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vibecode.minitodo.BuildConfig
import com.vibecode.minitodo.R
import com.vibecode.minitodo.data.prefs.AppSettings
import com.vibecode.minitodo.domain.SortMode
import com.vibecode.minitodo.domain.ThemeMode
import com.vibecode.minitodo.domain.ThemePalette
import com.vibecode.minitodo.ui.theme.effectivePalette
import com.vibecode.minitodo.ui.theme.onThemePaletteColor
import com.vibecode.minitodo.ui.theme.previewColor
import com.vibecode.minitodo.ui.todo.labelRes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    events: Flow<SettingsEvent>,
    onBack: () -> Unit,
    onSetSortMode: (SortMode) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetThemePalette: (ThemePalette) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val effectivePalette = settings.themePalette.effectivePalette()
    var showSortDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }

    LaunchedEffect(events) {
        events.collect { event ->
            val message = when (event) {
                is SettingsEvent.ExportSuccess ->
                    context.getString(R.string.backup_export_success, event.count)

                SettingsEvent.ExportFailed -> context.getString(R.string.backup_export_failed)
                is SettingsEvent.ImportSuccess ->
                    context.getString(R.string.backup_import_success, event.count)

                SettingsEvent.ImportFailed -> context.getString(R.string.backup_import_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var exactAlarmEnabled by remember { mutableStateOf(canScheduleExactAlarms(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        exactAlarmEnabled = canScheduleExactAlarms(context)
    }

    val openNotificationSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
        )
    }
    val openExactAlarmSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri(),
                    ),
                )
            } catch (_: Exception) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_section_general))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_sort)) },
                supportingContent = { Text(stringResource(settings.sortMode.labelRes())) },
                modifier = Modifier.clickable { showSortDialog = true },
            )

            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_section_appearance))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(stringResource(settings.themeMode.labelRes())) },
                modifier = Modifier.clickable { showThemeDialog = true },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme_color)) },
                supportingContent = {
                    ThemePaletteRow(
                        selected = effectivePalette,
                        onSelect = onSetThemePalette,
                    )
                },
            )

            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_section_notifications))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (notificationsEnabled) {
                                R.string.settings_notifications_granted
                            } else {
                                R.string.settings_notifications_denied
                            },
                        ),
                        color = if (notificationsEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                },
                trailingContent = {
                    TextButton(onClick = openNotificationSettings) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                },
                modifier = Modifier.clickable { openNotificationSettings() },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_exact_alarm)) },
                    supportingContent = {
                        Text(
                            text = stringResource(
                                if (exactAlarmEnabled) {
                                    R.string.settings_exact_alarm_granted
                                } else {
                                    R.string.settings_exact_alarm_denied
                                },
                            ),
                            color = if (exactAlarmEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    },
                    trailingContent = {
                        TextButton(
                            onClick = openExactAlarmSettings,
                            enabled = !exactAlarmEnabled,
                        ) {
                            Text(stringResource(R.string.action_open_settings))
                        }
                    },
                    modifier = Modifier.clickable(enabled = !exactAlarmEnabled) {
                        openExactAlarmSettings()
                    },
                )
            }

            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_section_backup))

            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_export)) },
                supportingContent = { Text(stringResource(R.string.backup_export_summary)) },
                modifier = Modifier.clickable {
                    exportLauncher.launch("minitodo-backup-${LocalDate.now()}.json")
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_import)) },
                supportingContent = { Text(stringResource(R.string.backup_import_summary)) },
                modifier = Modifier.clickable {
                    importLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream"),
                    )
                },
            )

            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_section_about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_privacy)) },
                modifier = Modifier.clickable { showPrivacyDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_license)) },
                modifier = Modifier.clickable { showLicenseDialog = true },
            )
        }
    }

    if (showSortDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_sort),
            options = SortMode.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.sortMode,
            onSelect = onSetSortMode,
            onDismiss = { showSortDialog = false },
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.themeMode,
            onSelect = onSetThemeMode,
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.about_privacy)) },
            text = { Text(stringResource(R.string.about_privacy_body)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(stringResource(R.string.about_license)) },
            text = { Text(stringResource(R.string.about_license_body)) },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        onImport(uri)
                    },
                ) {
                    Text(stringResource(R.string.action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            },
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePaletteRow(
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
) {
    val options = ThemePalette.entries.filter { palette ->
        palette != ThemePalette.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    FlowRow(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { palette ->
            ThemePaletteSwatch(
                palette = palette,
                selected = palette == selected,
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun ThemePaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(palette.labelRes())
    Box(
        modifier = Modifier
            .size(48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (palette == ThemePalette.DYNAMIC) {
                        Modifier.background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    ThemePalette.PINK.previewColor,
                                    ThemePalette.ORANGE.previewColor,
                                    ThemePalette.GREEN.previewColor,
                                    ThemePalette.BLUE.previewColor,
                                    ThemePalette.PURPLE.previewColor,
                                    ThemePalette.PINK.previewColor,
                                ),
                            ),
                        )
                    } else {
                        Modifier.background(palette.previewColor)
                    },
                )
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                if (palette == ThemePalette.DYNAMIC) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = palette.onThemePaletteColor(),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.onThemePaletteColor(),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun canScheduleExactAlarms(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager.canScheduleExactAlarms()
}

@StringRes
private fun ThemePalette.labelRes(): Int = when (this) {
    ThemePalette.DYNAMIC -> R.string.theme_color_dynamic
    ThemePalette.BLUE -> R.string.theme_color_blue
    ThemePalette.GREEN -> R.string.theme_color_green
    ThemePalette.PURPLE -> R.string.theme_color_purple
    ThemePalette.ORANGE -> R.string.theme_color_orange
    ThemePalette.PINK -> R.string.theme_color_pink
    ThemePalette.TEAL -> R.string.theme_color_teal
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
