package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ACTION_BUTTON_FONT_SIZE
import com.akiwiksten.awtimesheet.core.FORM_GROUP_SPACING
import com.akiwiksten.awtimesheet.core.FORM_MAX_WIDTH

@Composable
internal fun SettingsActionButtonsSection(
    onSave: () -> Unit,
    onGenerateXlsx: () -> Unit,
    isReportEnabled: Boolean,
    isSaveEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
    ) {
        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.save), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Button(
            onClick = onGenerateXlsx,
            enabled = isReportEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_xlsx), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        if (isReportEnabled) {
            Text(
                text = stringResource(id = R.string.monthly_help_xlsx),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
            )
        }
    }
}


