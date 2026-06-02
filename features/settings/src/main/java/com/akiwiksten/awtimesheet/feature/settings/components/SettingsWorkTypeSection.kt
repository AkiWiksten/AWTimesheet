package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.FORM_INLINE_SPACING
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.SettingsWorkTypeSectionState

@Composable
internal fun SettingsWorkTypeSection(state: SettingsWorkTypeSectionState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        DropdownMenuBox(
            items = state.workTypes,
            onItemSelected = state.settingsWorkTypeDialogState.onWorkTypeSelected,
            selectedText = state.settingsWorkTypeDialogState.selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)) {
            Button(
                onClick = state.settingsWorkTypeDialogState.onAddClick,
                modifier = Modifier.weight(weight = 1f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            Button(
                onClick = state.settingsWorkTypeDialogState.onDeleteClick,
                enabled = state.settingsWorkTypeDialogState.selectedWorkType.isNotEmpty() &&
                    state.settingsWorkTypeDialogState.selectedWorkType != state.protectedWorkType,
                modifier = Modifier.weight(weight = 1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}
