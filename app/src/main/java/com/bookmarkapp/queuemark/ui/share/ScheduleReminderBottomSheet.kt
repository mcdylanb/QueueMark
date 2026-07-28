package com.bookmarkapp.queuemark.ui.share

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookmarkapp.queuemark.domain.ReminderPreset
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme
import java.util.Calendar

@Composable
fun ScheduleReminderContent(
    state: ShareUiState,
    onAction: (ShareUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(24.dp)) {
        if (!state.hasValidUrl) {
            Text(
                text = "No link found in the shared text",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onAction(ShareUiAction.OnDone) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Close") }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Saved to Bookmark!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        state.scrapedTitle?.let { title ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "When do you plan on reading this?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderOption(
                label = "Tonight at 8 PM",
                selected = state.selectedPreset == ReminderPreset.TONIGHT,
                onClick = { onAction(ShareUiAction.OnPresetSelected(ReminderPreset.TONIGHT)) },
                modifier = Modifier.weight(1f)
            )
            ReminderOption(
                label = "Tomorrow Morning",
                selected = state.selectedPreset == ReminderPreset.TOMORROW_MORNING,
                onClick = {
                    onAction(ShareUiAction.OnPresetSelected(ReminderPreset.TOMORROW_MORNING))
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderOption(
                label = "This Weekend",
                selected = state.selectedPreset == ReminderPreset.THIS_WEEKEND,
                onClick = { onAction(ShareUiAction.OnPresetSelected(ReminderPreset.THIS_WEEKEND)) },
                modifier = Modifier.weight(1f)
            )
            ReminderOption(
                label = "Custom Date & Time",
                selected = state.customPicked,
                onClick = { onAction(ShareUiAction.OnCustomClicked) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onAction(ShareUiAction.OnDone) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Done") }
    }

    if (state.showCustomPicker) {
        CustomDateTimePicker(
            onPicked = { onAction(ShareUiAction.OnCustomPicked(it)) },
            onDismiss = { onAction(ShareUiAction.OnCustomDismissed) }
        )
    }
}

@Composable
private fun ReminderOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier.height(56.dp)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Date first, then time; combined into one epoch.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateTimePicker(
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var pickedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    if (pickedDateMillis == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDateMillis = datePickerState.selectedDateMillis },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = pickedDateMillis!!
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(calendar.timeInMillis)
                }) { Text("Set reminder") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Preview(showBackground = true, name = "Reminder sheet — light")
@Composable
private fun ScheduleReminderPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        ScheduleReminderContent(
            state = ShareUiState(scrapedTitle = "Designing for Focus"),
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Reminder sheet — dark, preset picked",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ScheduleReminderPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        ScheduleReminderContent(
            state = ShareUiState(
                scrapedTitle = "Designing for Focus",
                selectedPreset = ReminderPreset.TONIGHT
            ),
            onAction = {}
        )
    }
}
