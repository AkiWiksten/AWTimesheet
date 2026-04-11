package com.akiwiksten.worktime30.feature.projects.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.feature.projects.ProjectDialogState

@Composable
internal fun DialogDropdownFields(
    state: ProjectDialogState,
    workTypeDropDownList: List<String>,
    onStateChange: (ProjectDialogState) -> Unit
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
            items = listOf("No allowance", "Full allowance", "Half allowance"),
            selectedText = state.allowance,
            onItemSelected = { onStateChange(state.copy(allowance = it)) }
        )
    }
}
