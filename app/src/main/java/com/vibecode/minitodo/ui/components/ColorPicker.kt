package com.vibecode.minitodo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vibecode.minitodo.R
import com.vibecode.minitodo.domain.TodoColor
import com.vibecode.minitodo.ui.theme.onTodoColor
import com.vibecode.minitodo.ui.theme.todoColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selected: TodoColor,
    onSelect: (TodoColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TodoColor.entries.forEach { color ->
            val description = stringResource(color.labelRes())
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .selectable(
                        selected = selected == color,
                        role = Role.RadioButton,
                        onClick = { onSelect(color) },
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(todoColor(color))
                        .border(
                            width = if (selected == color) 3.dp else 1.dp,
                            color = if (selected == color) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == color) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = onTodoColor(color),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@StringRes
fun TodoColor.labelRes(): Int = when (this) {
    TodoColor.NONE -> R.string.color_none
    TodoColor.RED -> R.string.color_red
    TodoColor.ORANGE -> R.string.color_orange
    TodoColor.YELLOW -> R.string.color_yellow
    TodoColor.LIME -> R.string.color_lime
    TodoColor.GREEN -> R.string.color_green
    TodoColor.TEAL -> R.string.color_teal
    TodoColor.CYAN -> R.string.color_cyan
    TodoColor.BLUE -> R.string.color_blue
    TodoColor.INDIGO -> R.string.color_indigo
    TodoColor.PURPLE -> R.string.color_purple
    TodoColor.DEEP_PURPLE -> R.string.color_deep_purple
    TodoColor.PINK -> R.string.color_pink
    TodoColor.BROWN -> R.string.color_brown
    TodoColor.BLUE_GREY -> R.string.color_blue_grey
    TodoColor.DEEP_ORANGE -> R.string.color_deep_orange
}
