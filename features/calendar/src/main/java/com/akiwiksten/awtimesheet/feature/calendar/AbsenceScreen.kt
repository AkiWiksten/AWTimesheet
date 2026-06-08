package com.akiwiksten.awtimesheet.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateAbsence: () -> Unit,
) {
    val savedAbsences = remember { mutableStateListOf<SavedAbsence>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.absence)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.absence)
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
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNavigateToCreateAbsence,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = PADDING_SPACING)
                ) {
                    Text(text = stringResource(id = R.string.new_absence))
                }
            }

            if (savedAbsences.isNotEmpty()) {
                Text(text = stringResource(id = R.string.saved_absences_title))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f),
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

private data class SavedAbsence(
    val workType: String,
    val startDate: String,
    val endDate: String,
)

@Composable
private fun SavedAbsenceListItem(savedAbsence: SavedAbsence) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
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





