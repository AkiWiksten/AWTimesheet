package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_MAX_WIDTH
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.Header

@Composable
internal fun SettingsHeaderSection(date: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
        ) {
            Header(title = stringResource(id = R.string.settings))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
