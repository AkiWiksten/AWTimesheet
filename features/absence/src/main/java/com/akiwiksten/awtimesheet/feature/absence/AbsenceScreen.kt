package com.akiwiksten.awtimesheet.feature.absence

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.android.tools.screenshot.PreviewTest

@Composable
fun AbsenceScreen(
    onNavigateToCreateAbsence: () -> Unit,
    viewModel: AbsenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AbsenceContent(
        uiState = uiState,
        actions = AbsenceActions(
            onSelectAbsence = viewModel::selectAbsence,
            onDeleteSelectedAbsence = viewModel::deleteSelectedAbsence,
            onNavigateToCreateAbsence = onNavigateToCreateAbsence
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(all = PADDING_SPACING)
            .padding(bottom = LocalContentBottomPadding.current),
    )
}

@Composable
private fun AbsenceContent(
    uiState: AbsenceUiState,
    actions: AbsenceActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
        horizontalAlignment = Alignment.Start
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = PADDING_SPACING_SMALL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(title = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.absence))
            }
        }
        AbsenceActionButtons(
            selectedAbsenceId = uiState.selectedAbsenceId,
            actions = actions
        )
        Header(
            title = stringResource(id = R.string.saved_absences_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp)
        )
        SavedAbsencesList(
            uiState = uiState,
            actions = actions
        )
    }
}

@Composable
private fun AbsenceActionButtons(
    selectedAbsenceId: Int?,
    actions: AbsenceActions,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL),
        modifier = modifier.fillMaxWidth(),
    ) {
        AwtButton(
            onClick = actions.onNavigateToCreateAbsence,
            modifier = Modifier.weight(weight = 1f)
        ) {
            Text(text = stringResource(id = R.string.new_absence))
        }
        AwtButton(
            onClick = actions.onDeleteSelectedAbsence,
            enabled = selectedAbsenceId != null,
            modifier = Modifier.weight(weight = 1f)
        ) {
            Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.delete))
        }
    }
}

@Composable
private fun SavedAbsencesList(
    uiState: AbsenceUiState,
    actions: AbsenceActions,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = modifier.fillMaxWidth()
    ) {
        if (uiState.savedAbsences.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.no_absences), fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                items(
                    items = uiState.savedAbsences,
                    key = { it.id }
                ) { savedAbsence ->
                    SavedAbsenceListItem(
                        savedAbsence = savedAbsence,
                        isSelected = savedAbsence.id == uiState.selectedAbsenceId,
                        onClick = {
                            if (uiState.selectedAbsenceId == savedAbsence.id) {
                                actions.onSelectAbsence(null)
                            } else {
                                actions.onSelectAbsence(savedAbsence.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedAbsenceListItem(
    savedAbsence: SavedAbsence,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING_SMALL),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(text = savedAbsence.absenceType, fontWeight = FontWeight.Bold)
            Text(text = "${savedAbsence.startDate} - ${savedAbsence.endDate}", fontWeight = FontWeight.Bold)
        }
    }
}

private val PreviewSavedAbsences = listOf(
    SavedAbsence(
        id = 1,
        absenceType = "Paid vacation",
        startDate = "2026-07-01",
        endDate = "2026-07-10",
        includeWeekends = false,
        isFlexDay = false
    ),
    SavedAbsence(
        id = 2,
        absenceType = "Sick leave",
        startDate = "2026-05-15",
        endDate = "2026-05-16",
        includeWeekends = true,
        isFlexDay = false
    )
)

@PreviewTest
@Preview(showBackground = true, name = "Absence - Empty")
@Composable
fun PreviewAbsenceEmpty() {
    AbsencePreviewContent(uiState = AbsenceUiState())
}

@PreviewTest
@Preview(showBackground = true, name = "Absence - With Selection")
@Composable
fun PreviewAbsenceWithSelection() {
    AbsencePreviewContent(
        uiState = AbsenceUiState(
            savedAbsences = PreviewSavedAbsences,
            selectedAbsenceId = 2
        )
    )
}

@Composable
private fun AbsencePreviewContent(uiState: AbsenceUiState) {
    AWTimesheetTheme(dynamicColor = false) {
        AbsenceContent(
            uiState = uiState,
            actions = AbsenceActions(
                onSelectAbsence = {},
                onDeleteSelectedAbsence = {},
                onNavigateToCreateAbsence = {}
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(all = PADDING_SPACING)
        )
    }
}
