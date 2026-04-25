package com.akiwiksten.worktime30.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.FORM_GROUP_SPACING
import com.akiwiksten.worktime30.core.FORM_INLINE_SPACING
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.feature.workday.SingleProjectState

@Composable
internal fun WorkdayActionButtons(
    items: List<SingleProjectState>,
    selectedIndex: Int,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val selectedProject = items.getOrNull(index = selectedIndex)
    val deleteButtonText = if (selectedProject?.projectTime != ZERO_TIME) {
        stringResource(id = R.string.nullify)
    } else {
        stringResource(id = R.string.delete)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = FORM_INLINE_SPACING))
            Text(text = stringResource(id = R.string.add))
        }
        Button(
            onClick = onEditClick,
            enabled = selectedIndex != -1,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = FORM_INLINE_SPACING))
            Text(text = stringResource(id = R.string.edit))
        }
        Button(
            onClick = onDeleteClick,
            enabled = selectedIndex != -1,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = FORM_INLINE_SPACING))
            Text(text = deleteButtonText)
        }
    }
}
