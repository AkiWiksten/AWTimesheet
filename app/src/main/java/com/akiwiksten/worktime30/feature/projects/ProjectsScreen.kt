package com.akiwiksten.worktime30.feature.projects

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.MyAlertDialog
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel
import com.akiwiksten.worktime30.feature.editworkday.EditWorkDayViewModel

@Composable
fun ProjectsScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    editWorkDayViewModel: EditWorkDayViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val date = calendarUiState.date
    val selectedIndex by projectsViewModel.selectedIndex.collectAsState()
    val dropDownWorkTypes by projectsViewModel.dropDownWorkTypes.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showOverlapAlert by remember { mutableStateOf(false) }
    var showAddFailedAlert by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        if (date.isNotEmpty()) {
            projectsViewModel.setDate(date)
            projectsViewModel.loadWorkTimeTodayFromDb(date)
            projectsViewModel.index0 = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProjectsHeader(date = date, workTime = projectsViewModel.getWorkTimeToday())

        ProjectsListSection(
            items = projectsViewModel.items,
            selectedIndex = selectedIndex,
            onItemSelected = projectsViewModel::setSelectedIndex,
            modifier = Modifier.weight(1f)
        )

        ProjectsActionButtons(
            isItemSelected = selectedIndex != -1,
            onAddClick = { showAddDialog = true },
            onEditClick = { showEditDialog = true },
            onDeleteClick = { projectsViewModel.deleteItem(selectedIndex) },
            onSaveClick = { projectsViewModel.saveProjects() },
            isSaveEnabled = projectsViewModel.items.isNotEmpty()
        )

        DialogHandling(
            showAddDialog = showAddDialog,
            showEditDialog = showEditDialog,
            showOverlapAlert = showOverlapAlert,
            showAddFailedAlert = showAddFailedAlert,
            selectedIndex = selectedIndex,
            dropDownWorkTypes = dropDownWorkTypes,
            projectsViewModel = projectsViewModel,
            editWorkDayViewModel = editWorkDayViewModel,
            onAddDismiss = { showAddDialog = false },
            onEditDismiss = { showEditDialog = false },
            onOverlapDismiss = { showOverlapAlert = false },
            onAddFailedDismiss = { showAddFailedAlert = false },
            onItemAdded = { failed, overlapping ->
                showAddFailedAlert = failed
                showOverlapAlert = overlapping
            },
            onItemEdited = { overlapping ->
                showOverlapAlert = overlapping
            }
        )
    }
}

@Composable
private fun ProjectsHeader(date: String, workTime: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Header(stringResource(R.string.projects_customers))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${stringResource(R.string.work_time_today)}: $workTime",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ProjectsListSection(
    items: List<ProjectListItemUiState>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items) { item ->
            ProjectListItem(
                item = item,
                isSelected = selectedIndex == item.index,
                onClick = { onItemSelected(item.index) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun ProjectListItem(
    item: ProjectListItemUiState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.projectName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.projectStartTime} - ${item.projectEndTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.workType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.kilometres} km",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProjectsActionButtons(
    isItemSelected: Boolean,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaveEnabled: Boolean
) {
    val ctx = LocalContext.current
    val saveString = stringResource(R.string.saved)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = onEditClick,
                enabled = isItemSelected,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDeleteClick,
                enabled = isItemSelected,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete))
            }
            Button(
                onClick = {
                    onSaveClick()
                    Toast.makeText(ctx, saveString, Toast.LENGTH_SHORT).show()
                },
                enabled = isSaveEnabled,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun DialogHandling(
    showAddDialog: Boolean,
    showEditDialog: Boolean,
    showOverlapAlert: Boolean,
    showAddFailedAlert: Boolean,
    selectedIndex: Int,
    dropDownWorkTypes: List<String>,
    projectsViewModel: ProjectsViewModel,
    editWorkDayViewModel: EditWorkDayViewModel,
    onAddDismiss: () -> Unit,
    onEditDismiss: () -> Unit,
    onOverlapDismiss: () -> Unit,
    onAddFailedDismiss: () -> Unit,
    onItemAdded: (failed: Boolean, overlapping: Boolean) -> Unit,
    onItemEdited: (overlapping: Boolean) -> Unit
) {
    if (showAddFailedAlert) {
        MyAlertDialog(
            onDismissRequest = onAddFailedDismiss,
            onConfirmation = onAddFailedDismiss,
            dialogTitle = stringResource(R.string.add_project_item_failed_title),
            dialogText = stringResource(R.string.add_project_item_failed_text),
            icon = Icons.Default.Info
        )
    }

    if (showOverlapAlert) {
        MyAlertDialog(
            onDismissRequest = onOverlapDismiss,
            onConfirmation = onOverlapDismiss,
            dialogTitle = stringResource(R.string.items_overlapping_title),
            dialogText = stringResource(R.string.items_overlapping_text),
            icon = Icons.Default.Info
        )
    }

    if (showAddDialog) {
        ProjectDialog(
            onDismissRequest = onAddDismiss,
            onConfirmation = { uiState ->
                val failed = !projectsViewModel.addItem(uiState)
                val overlapping = projectsViewModel.areItemsOverlapping(uiState.projectStartTime, uiState.projectEndTime)
                onItemAdded(failed, overlapping)
                onAddDismiss()
            },
            workTypeDropDownList = dropDownWorkTypes,
            uiState = ProjectListItemUiState(
                projectName = "",
                titleId = R.string.add,
                leftOvers = projectsViewModel.leftOvers(ZERO_TIME)
            )
        )
    }

    if (showEditDialog && selectedIndex != -1) {
        val item = projectsViewModel.selectedItem(selectedIndex)
        ProjectDialog(
            onDismissRequest = onEditDismiss,
            onConfirmation = { uiState ->
                uiState.index = selectedIndex
                uiState.initBalance = "-" + editWorkDayViewModel.getWorkTimeToday()
                projectsViewModel.editItem(uiState)
                val overlapping = projectsViewModel.areItemsOverlapping(uiState.projectStartTime, uiState.projectEndTime)
                onItemEdited(overlapping)
                onEditDismiss()
            },
            uiState = item.copy(titleId = R.string.edit, leftOvers = projectsViewModel.leftOvers(item.projectTime)),
            workTypeDropDownList = dropDownWorkTypes
        )
    }
}
