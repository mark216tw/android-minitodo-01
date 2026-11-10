package com.vibecode.minitodo.ui.components

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibecode.minitodo.R
import com.vibecode.minitodo.util.DateTimeUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: Long?,
    allDay: Boolean,
    onDateChange: (Long) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackDate: Long? = null,
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    Text(
                        text = value?.let { DateTimeUtils.formatDate(it) }
                            ?: stringResource(R.string.select_date),
                    )
                },
            )
            if (!allDay) {
                AssistChip(
                    onClick = {
                        val current = value?.let { DateTimeUtils.localDateTime(it) }
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onTimeChange(hour, minute) },
                            current?.hour ?: DateTimeUtils.ALL_DAY_REMINDER_HOUR,
                            current?.minute ?: 0,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    enabled = value != null,
                    label = {
                        Text(
                            text = value?.let { DateTimeUtils.formatTime(it) }
                                ?: stringResource(R.string.select_time),
                        )
                    },
                )
            }
            FilterChip(
                selected = allDay,
                onClick = { onAllDayChange(!allDay) },
                enabled = value != null,
                label = { Text(stringResource(R.string.all_day_label)) },
            )
        }
    }

    if (showDatePicker) {
        val initialDate = value?.let { DateTimeUtils.localDate(it) }
            ?: fallbackDate?.let { DateTimeUtils.localDate(it) }
            ?: LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.datePickerMillis(initialDate),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateChange)
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val today = LocalDate.now()
                    listOf(
                        R.string.date_today to today,
                        R.string.date_tomorrow to today.plusDays(1),
                        R.string.date_next_week to today.plusWeeks(1),
                    ).forEach { (labelRes, date) ->
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis =
                                    DateTimeUtils.datePickerMillis(date)
                            },
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
                DatePicker(state = datePickerState)
            }
        }
    }
}
