package com.vibecode.minitodo.ui.edit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.vibecode.minitodo.R
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.ui.components.ColorPicker
import com.vibecode.minitodo.ui.components.DateTimeField
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditScreen(
    uiState: TodoEditUiState,
    events: Flow<TodoEditEvent>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onColorChange: (TodoColor) -> Unit,
    onTodoCompletedChange: (Boolean) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onRestoreItem: (ItemDraft, Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onItemTitleChange: (String, String) -> Unit,
    onItemCompletedChange: (String, Boolean) -> Unit,
    onStartDateChange: (String, Long) -> Unit,
    onStartTimeChange: (String, Int, Int) -> Unit,
    onStartAllDayChange: (String, Boolean) -> Unit,
    onClearStart: (String) -> Unit,
    onEndDateChange: (String, Long) -> Unit,
    onEndTimeChange: (String, Int, Int) -> Unit,
    onEndAllDayChange: (String, Boolean) -> Unit,
    onClearEnd: (String) -> Unit,
    onNotificationPermissionAsked: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDiscardDialog by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val hasTimedItem = uiState.items.any { it.startAt != null || it.endAt != null }

    LaunchedEffect(hasTimedItem, uiState.notificationPermissionAsked) {
        if (!hasTimedItem || uiState.notificationPermissionAsked ||
            notificationPermissionRequested || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            return@LaunchedEffect
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return@LaunchedEffect
        }
        notificationPermissionRequested = true
        onNotificationPermissionAsked()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(uiState.isNew, uiState.isLoading) {
        if (uiState.isNew && !uiState.isLoading) {
            delay(120L)
            titleFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                TodoEditEvent.Saved, TodoEditEvent.NotFound -> onBack()
                TodoEditEvent.SaveFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.save_failed),
                )
                is TodoEditEvent.Validation -> {
                    val messageRes = when (event.error) {
                        ValidationError.EMPTY_TITLE -> R.string.validation_title_empty
                        ValidationError.END_BEFORE_START -> R.string.validation_end_before_start
                    }
                    snackbarHostState.showSnackbar(context.getString(messageRes))
                }

                is TodoEditEvent.ItemRemoved -> {
                    val label = event.item.title.ifBlank {
                        context.getString(R.string.untitled_item)
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.deleted_message, label),
                        actionLabel = context.getString(R.string.action_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onRestoreItem(event.item, event.index)
                    }
                }
            }
        }
    }

    BackHandler(enabled = uiState.isDirty || uiState.isSaving) {
        if (!uiState.isSaving) showDiscardDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                uiState.isSaving -> Unit
                                uiState.isDirty -> showDiscardDialog = true
                                else -> onBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) {
                                R.string.new_todo_title
                            } else {
                                R.string.edit_todo_title
                            },
                        ),
                    )
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !uiState.isSaving && !uiState.isLoading,
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isSaving) {
                                    R.string.action_saving
                                } else {
                                    R.string.action_save
                                },
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            EditContent(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
                onTitleChange = onTitleChange,
                onColorChange = onColorChange,
                onTodoCompletedChange = onTodoCompletedChange,
                onAddItem = onAddItem,
                onRemoveItem = onRemoveItem,
                onMoveItem = onMoveItem,
                titleFocusRequester = titleFocusRequester,
                onItemTitleChange = onItemTitleChange,
                onItemCompletedChange = onItemCompletedChange,
                onStartDateChange = onStartDateChange,
                onStartTimeChange = onStartTimeChange,
                onStartAllDayChange = onStartAllDayChange,
                onClearStart = onClearStart,
                onEndDateChange = onEndDateChange,
                onEndTimeChange = onEndTimeChange,
                onEndAllDayChange = onEndAllDayChange,
                onClearEnd = onClearEnd,
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.unsaved_title)) },
            text = { Text(stringResource(R.string.unsaved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.action_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditContent(
    uiState: TodoEditUiState,
    modifier: Modifier,
    onTitleChange: (String) -> Unit,
    onColorChange: (TodoColor) -> Unit,
    onTodoCompletedChange: (Boolean) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    titleFocusRequester: FocusRequester,
    onItemTitleChange: (String, String) -> Unit,
    onItemCompletedChange: (String, Boolean) -> Unit,
    onStartDateChange: (String, Long) -> Unit,
    onStartTimeChange: (String, Int, Int) -> Unit,
    onStartAllDayChange: (String, Boolean) -> Unit,
    onClearStart: (String) -> Unit,
    onEndDateChange: (String, Long) -> Unit,
    onEndTimeChange: (String, Int, Int) -> Unit,
    onEndAllDayChange: (String, Boolean) -> Unit,
    onClearEnd: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var autoScrollDirection by remember { mutableFloatStateOf(0f) }
    val currentItems by rememberUpdatedState(uiState.items)
    val currentOnMove by rememberUpdatedState(onMoveItem)

    LaunchedEffect(draggingKey, autoScrollDirection) {
        if (draggingKey == null || autoScrollDirection == 0f) return@LaunchedEffect
        while (true) {
            val consumed = listState.scrollBy(autoScrollDirection)
            if (consumed == 0f) {
                autoScrollDirection = 0f
                break
            }
            dragOffset += consumed
            delay(16L)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.todo_title_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
        }

        item(key = "master-complete") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.mark_todo_complete),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.todoCompleted,
                    onCheckedChange = onTodoCompletedChange,
                )
            }
        }

        item(key = "colors") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.color_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                ColorPicker(
                    selected = uiState.color,
                    onSelect = onColorChange,
                )
            }
        }

        item(key = "items-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.items_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAddItem) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(R.string.add_item))
                }
            }
        }

        itemsIndexed(uiState.items, key = { _, item -> item.key }) { _, item ->
            val moveUpLabel = stringResource(R.string.action_move_up)
            val moveDownLabel = stringResource(R.string.action_move_down)
            val moveBy: (Int) -> Unit = { delta ->
                val from = currentItems.indexOfFirst { it.key == item.key }
                val to = from + delta
                if (from >= 0 && to in currentItems.indices) {
                    currentOnMove(from, to)
                }
            }
            val dragHandleModifier = Modifier
                .pointerInput(item.key) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingKey = item.key
                            autoScrollDirection = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (draggingKey == item.key) {
                                dragOffset += dragAmount.y
                                val info = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == item.key }
                                if (info != null) {
                                    val center = info.offset + info.size / 2f + dragOffset
                                    val target = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { other ->
                                            other.key != item.key &&
                                                other.key is String &&
                                                currentItems.any { it.key == other.key } &&
                                                center.toInt() in other.offset..(other.offset + other.size)
                                        }
                                    if (target != null) {
                                        val from = currentItems.indexOfFirst { it.key == item.key }
                                        val to = currentItems.indexOfFirst { it.key == target.key }
                                        if (from >= 0 && to >= 0 && from != to) {
                                            currentOnMove(from, to)
                                            dragOffset += info.offset - target.offset
                                        }
                                    }
                                    val edge = 72.dp.toPx()
                                    val step = 12.dp.toPx()
                                    val viewportStart = listState.layoutInfo.viewportStartOffset
                                    val viewportEnd = listState.layoutInfo.viewportEndOffset
                                    autoScrollDirection = when {
                                        center < viewportStart + edge -> -step
                                        center > viewportEnd - edge -> step
                                        else -> 0f
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggingKey = null
                            dragOffset = 0f
                            autoScrollDirection = 0f
                        },
                        onDragCancel = {
                            draggingKey = null
                            dragOffset = 0f
                            autoScrollDirection = 0f
                        },
                    )
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(moveUpLabel) {
                            moveBy(-1)
                            true
                        },
                        CustomAccessibilityAction(moveDownLabel) {
                            moveBy(1)
                            true
                        },
                    )
                }

            val cardModifier = Modifier
                .zIndex(if (draggingKey == item.key) 1f else 0f)
                .graphicsLayer {
                    translationY = if (draggingKey == item.key) dragOffset else 0f
                    shadowElevation = if (draggingKey == item.key) 8.dp.toPx() else 0f
                }
                .then(if (draggingKey != item.key) Modifier.animateItem() else Modifier)

            ItemEditorCard(
                item = item,
                modifier = cardModifier,
                dragHandleModifier = dragHandleModifier,
                onTitleChange = { onItemTitleChange(item.key, it) },
                onCompletedChange = { onItemCompletedChange(item.key, it) },
                onDelete = { onRemoveItem(item.key) },
                onStartDateChange = { onStartDateChange(item.key, it) },
                onStartTimeChange = { hour, minute ->
                    onStartTimeChange(item.key, hour, minute)
                },
                onStartAllDayChange = { onStartAllDayChange(item.key, it) },
                onClearStart = { onClearStart(item.key) },
                onEndDateChange = { onEndDateChange(item.key, it) },
                onEndTimeChange = { hour, minute ->
                    onEndTimeChange(item.key, hour, minute)
                },
                onEndAllDayChange = { onEndAllDayChange(item.key, it) },
                onClearEnd = { onClearEnd(item.key) },
            )
        }

        item(key = "add-item") {
            OutlinedButton(
                onClick = onAddItem,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(R.string.add_item))
            }
        }
    }
}

@SuppressLint("ModifierParameter")
@Composable
private fun ItemEditorCard(
    item: ItemDraft,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier,
    onTitleChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onStartDateChange: (Long) -> Unit,
    onStartTimeChange: (Int, Int) -> Unit,
    onStartAllDayChange: (Boolean) -> Unit,
    onClearStart: () -> Unit,
    onEndDateChange: (Long) -> Unit,
    onEndTimeChange: (Int, Int) -> Unit,
    onEndAllDayChange: (Boolean) -> Unit,
    onClearEnd: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {},
                    modifier = dragHandleModifier,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.drag_handle),
                    )
                }
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = onCompletedChange,
                    modifier = Modifier.semantics {
                        contentDescription = item.title
                    },
                )
                Text(
                    text = stringResource(R.string.completed_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_item),
                    )
                }
            }

            OutlinedTextField(
                value = item.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.item_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            DateTimeField(
                label = stringResource(R.string.start_label),
                value = item.startAt,
                allDay = item.startAllDay,
                onDateChange = onStartDateChange,
                onTimeChange = onStartTimeChange,
                onAllDayChange = onStartAllDayChange,
                onClear = onClearStart,
            )

            DateTimeField(
                label = stringResource(R.string.end_label),
                value = item.endAt,
                allDay = item.endAllDay,
                onDateChange = onEndDateChange,
                onTimeChange = onEndTimeChange,
                onAllDayChange = onEndAllDayChange,
                onClear = onClearEnd,
                fallbackDate = item.startAt,
            )
        }
    }
}
