package com.akiwiksten.worktime30.feature.projects.single.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.feature.workday.SingleProjectState

@Composable
internal fun DialogDropdownFields(
    state: SingleProjectState,
    workTypeDropDownList: List<String>,
    onStateChange: (SingleProjectState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        DropdownMenuBox(
            labelId = R.string.work_type,
            items = workTypeDropDownList,
            selectedText = state.workType,
            onItemSelected = { onStateChange(state.copy(workType = it)) }
        )

        DropdownMenuBox(
            labelId = R.string.allowance,
            items = listOf(
                stringResource(id = R.string.no_allowance),
                stringResource(id = R.string.full_allowance),
                stringResource(id = R.string.half_day_allowance)
            ),
            selectedText = state.allowance,
            onItemSelected = { onStateChange(state.copy(allowance = it)) }
        )
    }
}
