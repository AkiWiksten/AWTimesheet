package com.akiwiksten.awtimesheet.feature.absence

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateAbsence: () -> Unit,
    viewModel: AbsenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.absence)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.absence),
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        AbsenceContent(
            savedAbsences = uiState.savedAbsences,
            onNavigateToCreateAbsence = onNavigateToCreateAbsence,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(all = PADDING_SPACING)
                .padding(bottom = LocalContentBottomPadding.current),
        )
    }
}

@Composable
private fun AbsenceContent(
    savedAbsences: List<SavedAbsence>,
    onNavigateToCreateAbsence: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AwtButton(
                onClick = onNavigateToCreateAbsence,
                modifier = Modifier.weight(weight = 1f)
            ) {
                Text(text = stringResource(id = R.string.new_absence))
            }
            AwtButton(
                onClick = {},
                modifier = Modifier.weight(weight = 1f)
            ) {
                Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.delete))
            }
        }
        Text(text = stringResource(id = R.string.saved_absences_title), fontWeight = FontWeight.Bold)
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (savedAbsences.isEmpty()) {
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
                    items(items = savedAbsences) { savedAbsence ->
                        SavedAbsenceListItem(savedAbsence = savedAbsence)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAbsenceListItem(savedAbsence: SavedAbsence, isSelected: Boolean = false) {
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
        modifier = Modifier.fillMaxWidth(),
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
