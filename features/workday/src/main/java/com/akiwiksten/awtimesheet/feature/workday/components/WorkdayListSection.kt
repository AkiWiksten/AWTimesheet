package com.akiwiksten.awtimesheet.feature.workday.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.feature.workday.R

@Composable
internal fun WorkdayListSection(
    items: List<WorkdayListItemUiModel>,
    selectedItemKey: String?,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
                )
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.no_projects_available),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(all = 32.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(space = 2.dp)
                ) {
                    items.forEach { item ->
                        ProjectListItem(
                            item = item,
                            isSelected = selectedItemKey == item.stableKey,
                            onClick = { onItemSelected(item.index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectListItem(
    item: WorkdayListItemUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            if (item.isProjectNameOnlyPlaceholder) {
                ProjectNameOnlyContent(projectName = item.projectName)
            } else {
                ProjectSummaryContent(
                    projectName = item.projectName,
                    projectTime = item.projectTime,
                    kilometresLabel = item.kilometresLabel,
                    allowance = item.allowance,
                    workType = item.workType
                )
            }
        }
    }
}

@Composable
private fun ProjectNameOnlyContent(projectName: String) {
    Text(
        text = projectName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProjectSummaryContent(
    projectName: String,
    projectTime: String,
    kilometresLabel: String,
    allowance: String,
    workType: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = projectTime,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProjectDetails(
            workType = workType,
            allowance = allowance,
            modifier = Modifier.weight(weight = 1f)
        )
        Text(
            text = kilometresLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Immutable
internal data class WorkdayListItemUiModel(
    val index: Int,
    val projectName: String,
    val projectTime: String,
    val kilometres: String,
    val allowance: String,
    val workType: String,
    val kilometresLabel: String,
    val isProjectNameOnlyPlaceholder: Boolean,
    val stableKey: String
)

@Composable
private fun ProjectDetails(workType: String, allowance: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (workType.isNotEmpty()) {
            Text(
                text = workType,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (allowance.isNotEmpty()) {
            Text(
                text = allowance,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
