package com.akiwiksten.awtimesheet.feature.absence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    val savedAbsences = uiState.savedAbsences

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(all = PADDING_SPACING)
                .padding(bottom = LocalContentBottomPadding.current),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start
        ) {
            AwtButton(
                onClick = onNavigateToCreateAbsence,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.new_absence))
            }
            Text(text = stringResource(id = R.string.saved_absences_title))
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
                shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
            ) {
                if (savedAbsences.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(id = R.string.no_absences))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = PADDING_SPACING_SMALL),
                        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
                    ) {
                        items(items = savedAbsences) { savedAbsence ->
                            SavedAbsenceListItem(savedAbsence = savedAbsence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAbsenceListItem(savedAbsence: SavedAbsence) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(text = savedAbsence.workType)
            Text(text = "${stringResource(id = R.string.start_date)}: ${savedAbsence.startDate}")
            Text(text = "${stringResource(id = R.string.end_date)}: ${savedAbsence.endDate}")
        }
    }
}
