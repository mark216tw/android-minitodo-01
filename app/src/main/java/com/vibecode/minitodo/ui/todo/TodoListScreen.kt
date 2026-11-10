package com.vibecode.minitodo.ui.todo

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vibecode.minitodo.R
import com.vibecode.minitodo.domain.SectionType
import com.vibecode.minitodo.domain.SortMode
import com.vibecode.minitodo.domain.Todo
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.domain.TodoItem
import com.vibecode.minitodo.domain.TodoSection
import com.vibecode.minitodo.ui.theme.MiniTodoTheme
import com.vibecode.minitodo.ui.theme.todoColor
import com.vibecode.minitodo.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    uiState: TodoListUiState,
    events: Flow<TodoListEvent>,
    onAdd: () -> Unit,
    onOpenTodo: (Long) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onToggleItem: (TodoItem) -> Unit,
    onArchiveTodo: (Todo) -> Unit,
    onRestoreTodo: (Long) -> Unit,
    onArchiveCompleted: () -> Unit,
    onSetSortMode: (SortMode) -> Unit,
    onReorderTodos: (List<Long>) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchActive by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = searchActive) {
        searchActive = false
        onSearchQueryChange("")
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is TodoListEvent.Archived -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.archived_message,
                            event.title.ifBlank { context.getString(R.string.untitled_todo) },
                        ),
                        actionLabel = context.getString(R.string.action_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onRestoreTodo(event.todoId)
                    }
                }

                is TodoListEvent.ArchivedCompleted -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.archive_completed_count, event.count),
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TodoListTopBar(
                sortMode = uiState.sortMode,
                completedCount = uiState.completedCount,
                searchActive = searchActive,
                searchQuery = uiState.searchQuery,
                onSetSortMode = onSetSortMode,
                onOpenArchive = onOpenArchive,
                onOpenSettings = onOpenSettings,
                onArchiveCompleted = onArchiveCompleted,
                onSearchActiveChange = { active ->
                    searchActive = active
                    if (!active) onSearchQueryChange("")
                },
                onSearchQueryChange = onSearchQueryChange,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.hasError -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.error_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.error_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                uiState.sections.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank()) {
                        SearchEmptyContent(modifier = Modifier.align(Alignment.Center))
                    } else {
                        EmptyTodoContent(
                            onAdd = onAdd,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                else -> {
                    TodoSectionList(
                        sections = uiState.sections,
                        sortMode = uiState.sortMode,
                        onReorderTodos = onReorderTodos,
                        onOpenTodo = onOpenTodo,
                        onToggleTodo = onToggleTodo,
                        onToggleItem = onToggleItem,
                        onArchiveTodo = onArchiveTodo,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoListTopBar(
    sortMode: SortMode,
    completedCount: Int,
    searchActive: Boolean,
    searchQuery: String,
    onSetSortMode: (SortMode) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onArchiveCompleted: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(100L)
            searchFocusRequester.requestFocus()
        }
    }

    TopAppBar(
        title = {
            if (searchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { onSearchActiveChange(false) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                            )
                        }
                    },
                )
            } else {
                Text(stringResource(R.string.app_title))
            }
        },
        actions = {
            if (!searchActive) {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                    )
                }
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.action_sort),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(stringResource(mode.labelRes())) },
                                onClick = {
                                    onSetSortMode(mode)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (sortMode == mode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.archive_screen_title)) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenArchive()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.archive_completed)) },
                            enabled = completedCount > 0,
                            onClick = {
                                moreMenuExpanded = false
                                onArchiveCompleted()
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            onClick = {
                                moreMenuExpanded = false
                                onOpenSettings()
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyTodoContent(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAdd) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.action_add))
        }
    }
}

@Composable
private fun SearchEmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.search_no_results_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodoSectionList(
    sections: List<TodoSection>,
    sortMode: SortMode,
    onReorderTodos: (List<Long>) -> Unit,
    onOpenTodo: (Long) -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onToggleItem: (TodoItem) -> Unit,
    onArchiveTodo: (Todo) -> Unit,
) {
    val listState = rememberLazyListState()
    var draggingTodoId by remember { mutableStateOf<Long?>(null) }
    var todoDragOffset by remember { mutableFloatStateOf(0f) }
    var autoScrollDirection by remember { mutableFloatStateOf(0f) }
    var manualOrderIds by remember { mutableStateOf<List<Long>?>(null) }
    var completedExpanded by rememberSaveable { mutableStateOf(true) }
    val dbManualTodos = sections.firstOrNull { it.type == SectionType.ALL }?.todos.orEmpty()

    LaunchedEffect(dbManualTodos.map { it.id }) {
        if (draggingTodoId == null) {
            manualOrderIds = dbManualTodos.map { it.id }
        }
    }

    val displayedSections = if (sortMode == SortMode.MANUAL) {
        val orderedIds = manualOrderIds ?: dbManualTodos.map { it.id }
        val byId = dbManualTodos.associateBy { it.id }
        val orderedTodos = orderedIds.mapNotNull { byId[it] } +
            dbManualTodos.filterNot { it.id in orderedIds }
        sections.map { section ->
            if (section.type == SectionType.ALL) {
                section.copy(todos = orderedTodos)
            } else {
                section
            }
        }
    } else {
        sections
    }
    val manualTodos = displayedSections
        .firstOrNull { it.type == SectionType.ALL }
        ?.todos
        .orEmpty()
    val currentManualTodos by rememberUpdatedState(manualTodos)
    val currentReorder by rememberUpdatedState(onReorderTodos)

    LaunchedEffect(draggingTodoId, autoScrollDirection) {
        if (draggingTodoId == null || autoScrollDirection == 0f) return@LaunchedEffect
        while (true) {
            val consumed = listState.scrollBy(autoScrollDirection)
            if (consumed == 0f) {
                autoScrollDirection = 0f
                break
            }
            todoDragOffset += consumed
            delay(16L)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        displayedSections.forEach { section ->
            val isCompletedSection = section.type == SectionType.COMPLETED
            item(key = "header-${section.type}") {
                SectionHeader(
                    section = section,
                    expanded = if (isCompletedSection) completedExpanded else null,
                    onToggle = if (isCompletedSection) {
                        { completedExpanded = !completedExpanded }
                    } else {
                        null
                    },
                )
            }
            if (!isCompletedSection || completedExpanded) {
                items(section.todos, key = { todo -> "todo-${todo.id}" }) { todo ->
                val manual = sortMode == SortMode.MANUAL && section.type == SectionType.ALL
                val moveUpLabel = stringResource(R.string.action_move_up)
                val moveDownLabel = stringResource(R.string.action_move_down)
                val moveBy: (Int) -> Unit = { delta ->
                    val from = currentManualTodos.indexOfFirst { it.id == todo.id }
                    val to = from + delta
                    if (from >= 0 && to in currentManualTodos.indices) {
                        val reordered = currentManualTodos
                            .toMutableList()
                            .apply { add(to, removeAt(from)) }
                        manualOrderIds = reordered.map { it.id }
                        currentReorder(reordered.map { it.id })
                    }
                }
                val dragHandleModifier = if (manual) {
                    Modifier
                        .pointerInput(todo.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingTodoId = todo.id
                                    autoScrollDirection = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingTodoId == todo.id) {
                                        todoDragOffset += dragAmount.y
                                        val key = "todo-${todo.id}"
                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == key }
                                        if (info != null) {
                                            val center = info.offset + info.size / 2f + todoDragOffset
                                            val target = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { other ->
                                                    val otherKey = other.key
                                                    otherKey is String &&
                                                        otherKey.startsWith("todo-") &&
                                                        currentManualTodos.any {
                                                            "todo-${it.id}" == otherKey
                                                        } &&
                                                        center.toInt() in
                                                        other.offset..(other.offset + other.size)
                                                }
                                            if (target != null) {
                                                val from = currentManualTodos
                                                    .indexOfFirst { "todo-${it.id}" == key }
                                                val to = currentManualTodos
                                                    .indexOfFirst { "todo-${it.id}" == target.key }
                                                if (from >= 0 && to >= 0 && from != to) {
                                                    val reordered = currentManualTodos
                                                        .toMutableList()
                                                        .apply { add(to, removeAt(from)) }
                                                    manualOrderIds = reordered.map { it.id }
                                                    todoDragOffset += info.offset - target.offset
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
                                    draggingTodoId = null
                                    todoDragOffset = 0f
                                    autoScrollDirection = 0f
                                    manualOrderIds?.let(currentReorder)
                                },
                                onDragCancel = {
                                    draggingTodoId = null
                                    todoDragOffset = 0f
                                    autoScrollDirection = 0f
                                    manualOrderIds?.let(currentReorder)
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
                } else {
                    null
                }
                val cardModifier = if (manual) {
                    Modifier
                        .zIndex(if (draggingTodoId == todo.id) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (draggingTodoId == todo.id) todoDragOffset else 0f
                            shadowElevation = if (draggingTodoId == todo.id) 8.dp.toPx() else 0f
                        }
                        .then(
                            if (draggingTodoId != todo.id) {
                                Modifier.animateItem()
                            } else {
                                Modifier
                            },
                        )
                } else {
                    Modifier.animateItem()
                }

                TodoCard(
                    todo = todo,
                    overdue = section.type == SectionType.OVERDUE,
                    modifier = cardModifier,
                    dragHandleModifier = dragHandleModifier,
                    onOpen = { onOpenTodo(todo.id) },
                    onToggleTodo = { onToggleTodo(todo) },
                    onToggleItem = onToggleItem,
                    onArchive = { onArchiveTodo(todo) },
                )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: TodoSection,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggle != null) {
                    Modifier.clickable(role = Role.Button, onClick = onToggle)
                } else {
                    Modifier
                },
            )
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(section.type.labelRes()),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = section.todos.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded != null) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = stringResource(
                    if (expanded) {
                        R.string.content_description_collapse
                    } else {
                        R.string.content_description_expand
                    },
                ),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@SuppressLint("ModifierParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoCard(
    todo: Todo,
    overdue: Boolean,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
    onOpen: () -> Unit,
    onToggleTodo: () -> Unit,
    onToggleItem: (TodoItem) -> Unit,
    onArchive: () -> Unit,
) {
    var expanded by rememberSaveable(todo.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val archiveLabel = stringResource(R.string.action_archive)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onToggleTodo()
                SwipeToDismissBoxValue.EndToStart -> onArchive()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    val direction = dismissState.dismissDirection
    val backgroundColors = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer

        SwipeToDismissBoxValue.EndToStart ->
            MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer

        SwipeToDismissBoxValue.Settled ->
            MaterialTheme.colorScheme.surfaceVariant to
                MaterialTheme.colorScheme.onSurfaceVariant
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColors.first)
                    .padding(horizontal = 24.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ) {
                if (direction == SwipeToDismissBoxValue.Settled) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SwipeActionLabel(
                            text = stringResource(R.string.action_complete),
                            color = backgroundColors.second,
                        )
                        SwipeActionLabel(
                            text = stringResource(R.string.action_archive),
                            color = backgroundColors.second,
                        )
                    }
                } else {
                    SwipeActionLabel(
                        text = stringResource(
                            when {
                                direction == SwipeToDismissBoxValue.EndToStart ->
                                    R.string.action_archive

                                todo.isCompleted -> R.string.action_mark_incomplete
                                else -> R.string.action_complete
                            },
                        ),
                        color = backgroundColors.second,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        Card(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(archiveLabel) {
                            onArchive()
                            true
                        },
                    )
                },
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(todoColor(todo.color)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                ) {
                    TodoHeader(
                        todo = todo,
                        overdue = overdue,
                        expanded = expanded,
                        dragHandleModifier = dragHandleModifier,
                        onToggleTodo = onToggleTodo,
                        onToggleExpanded = { expanded = !expanded },
                    )
                    if (expanded) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        TodoItems(
                            todo = todo,
                            onToggleItem = onToggleItem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeActionLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@SuppressLint("ModifierParameter")
@Composable
private fun TodoHeader(
    todo: Todo,
    overdue: Boolean,
    expanded: Boolean,
    dragHandleModifier: Modifier?,
    onToggleTodo: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val title = todo.title.ifBlank { stringResource(R.string.untitled_todo) }
    val progress = if (todo.items.isEmpty()) {
        null
    } else {
        stringResource(
            R.string.todo_progress,
            todo.completedItemCount,
            todo.items.size,
        )
    }
    val rangeSeparator = stringResource(R.string.separator_range)
    val timeText = todo.items
        .filterNot { it.isCompleted }
        .mapNotNull {
            DateTimeUtils.formatRange(
                it.startAt,
                it.startAllDay,
                it.endAt,
                it.endAllDay,
                rangeSeparator = rangeSeparator,
            )
        }
        .firstOrNull()
        ?: todo.items.mapNotNull {
            DateTimeUtils.formatRange(
                it.startAt,
                it.startAllDay,
                it.endAt,
                it.endAllDay,
                rangeSeparator = rangeSeparator,
            )
        }.firstOrNull()
    val toggleDescription = stringResource(R.string.content_description_toggle_complete)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggleTodo() },
            modifier = Modifier.semantics {
                contentDescription = "$toggleDescription: $title"
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress != null || timeText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress != null) {
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (progress != null && timeText != null) {
                        Text(
                            text = stringResource(R.string.separator_middle_dot),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (timeText != null) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (overdue) {
                Text(
                    text = stringResource(R.string.todo_overdue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = stringResource(
                    if (expanded) {
                        R.string.content_description_collapse
                    } else {
                        R.string.content_description_expand
                    },
                ),
            )
        }
        if (dragHandleModifier != null) {
            IconButton(
                onClick = {},
                modifier = dragHandleModifier,
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.drag_handle),
                )
            }
        }
    }
}

@Composable
private fun TodoItems(
    todo: Todo,
    onToggleItem: (TodoItem) -> Unit,
) {
    if (todo.items.isEmpty()) {
        Text(
            text = stringResource(R.string.todo_no_items),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val incomplete = todo.items.filterNot { it.isCompleted }
    val completed = todo.items.filter { it.isCompleted }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        incomplete.forEach { item ->
            TodoItemRow(item = item, onToggle = { onToggleItem(item) })
        }
        if (completed.isNotEmpty()) {
            Text(
                text = stringResource(R.string.section_completed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            completed.forEach { item ->
                TodoItemRow(item = item, onToggle = { onToggleItem(item) })
            }
        }
    }
}

@Composable
private fun TodoItemRow(
    item: TodoItem,
    onToggle: () -> Unit,
) {
    val title = item.title.ifBlank { stringResource(R.string.untitled_item) }
    val timeText = DateTimeUtils.formatRange(
        item.startAt,
        item.startAllDay,
        item.endAt,
        item.endAllDay,
        rangeSeparator = stringResource(R.string.separator_range),
    )
    val toggleDescription = stringResource(R.string.content_description_toggle_complete)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.semantics {
                contentDescription = "$toggleDescription: $title"
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (timeText != null) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@StringRes
fun SectionType.labelRes(): Int = when (this) {
    SectionType.OVERDUE -> R.string.section_overdue
    SectionType.TODAY -> R.string.section_today
    SectionType.UPCOMING -> R.string.section_upcoming
    SectionType.NO_DATE -> R.string.section_no_date
    SectionType.ALL -> R.string.section_all
    SectionType.COMPLETED -> R.string.section_completed
}

@StringRes
fun SortMode.labelRes(): Int = when (this) {
    SortMode.EARLIEST_START -> R.string.sort_earliest_start
    SortMode.CREATED -> R.string.sort_created
    SortMode.MANUAL -> R.string.sort_manual
}

@Preview(showBackground = true)
@Composable
private fun TodoCardPreview() {
    MiniTodoTheme {
        TodoCard(
            todo = Todo(
                id = 1L,
                title = "準備專案文件",
                color = TodoColor.BLUE,
                items = listOf(
                    TodoItem(
                        id = 1L,
                        title = "整理需求",
                        isCompleted = true,
                    ),
                    TodoItem(
                        id = 2L,
                        title = "撰寫初稿",
                        startAt = System.currentTimeMillis() + 3_600_000L,
                    ),
                ),
            ),
            overdue = false,
            onOpen = {},
            onToggleTodo = {},
            onToggleItem = {},
            onArchive = {},
        )
    }
}
