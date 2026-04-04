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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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

@Suppress("LongMethod")
@Composable
fun ProjectsScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    editWorkDayViewModel: EditWorkDayViewModel = hiltViewModel(),
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
) {
    val date by calendarViewModel.date.collectAsState()
    var openProjectDialogAdd by remember { mutableStateOf(false) }
    var openProjectDialogEdit by remember { mutableStateOf(false) }
    var areItemsOverLapping by remember { mutableStateOf(false) }
    var additionFailed by remember { mutableStateOf(false) }
    val selectedIndex by projectsViewModel.selectedIndex.collectAsState()
    val dropDownWorkTypes by
        projectsViewModel.dropDownWorkTypes.collectAsState()
    val ctx = LocalContext.current
    projectsViewModel.setCtx(ctx)

    LaunchedEffect(Unit) {
        projectsViewModel.setDate(date)
        projectsViewModel.loadWorkTimeTodayFromDb(date)
        projectsViewModel.index0 = 0
        projectsViewModel.deletedProjects.clear()
        projectsViewModel.loadWorkTypes()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxSize()
    ) {
        val saveString = stringResource(R.string.saved)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(stringResource(R.string.projects_customers))
            Text(
                text = date,
                fontSize = 30.sp,
            )
            Text(
                text = stringResource(R.string.work_time_today) + ": " +
                    projectsViewModel.getWorkTimeToday(),
                fontSize = 20.sp,
            )
        }
        ProjectsList(
            selectedIndex = selectedIndex,
            items = projectsViewModel.items,
            setSelectedIndex = projectsViewModel::setSelectedIndex,
            modifier = Modifier.weight(2f)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                // Button to add items to the list
                Button(onClick = { openProjectDialogAdd = true }) {
                    Text(text = stringResource(R.string.add), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.padding(8.dp))
                // Button to add items to the list
                Button(
                    onClick = { openProjectDialogEdit = true },
                    enabled = selectedIndex != -1
                ) {
                    Text(text = stringResource(R.string.edit), fontSize = 20.sp)
                }
            }
            Row(modifier = Modifier.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 20.dp)) {
                Button(
                    onClick = {
                        projectsViewModel.deleteItem(index = selectedIndex)
                    },
                    enabled = (selectedIndex != -1)
                ) {
                    Text(text = stringResource(R.string.delete), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = {
                        projectsViewModel.saveProjects()
                        Toast.makeText(ctx, saveString, Toast.LENGTH_SHORT).show()
                    },
                    enabled = projectsViewModel.items.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.save), fontSize = 20.sp)
                }
            }
        }
        when {
            additionFailed -> {
                MyAlertDialog(
                    onDismissRequest = { additionFailed = false },
                    onConfirmation = {
                        additionFailed = false
                    },
                    dialogTitle = stringResource(R.string.add_project_item_failed_title),
                    dialogText = stringResource(R.string.add_project_item_failed_text),
                    icon = Icons.Default.Info
                )
            }
            areItemsOverLapping -> {
                MyAlertDialog(
                    onDismissRequest = { areItemsOverLapping = false },
                    onConfirmation = {
                        areItemsOverLapping = false
                    },
                    dialogTitle = stringResource(R.string.items_overlapping_title),
                    dialogText = stringResource(R.string.items_overlapping_text),
                    icon = Icons.Default.Info
                )
            }
            openProjectDialogAdd -> {
                ProjectDialog(
                    onDismissRequest = { openProjectDialogAdd = false },
                    onConfirmation = fun(uiState: ProjectListItemUiState) {
                        additionFailed = !projectsViewModel.addItem(
                            uiState = uiState
                        )
                        openProjectDialogAdd = false
                        areItemsOverLapping = projectsViewModel.areItemsOverlapping(
                            uiState.projectStartTime,
                            uiState.projectEndTime
                        )
                    },
                    workTypeDropDownList = dropDownWorkTypes,
                    uiState = ProjectListItemUiState(
                        projectName = "Project1",
                        titleId = R.string.add,
                        leftOvers = projectsViewModel.leftOvers(ZERO_TIME)
                    )
                )
            }
            openProjectDialogEdit -> {
                var projectName = "Project1"
                var projectStartTime = ZERO_TIME
                var projectEndTime = ZERO_TIME
                var projectTime = ZERO_TIME
                var kilometres = 0
                var allowance = ""
                var workType = ""
                if (selectedIndex != -1) {
                    projectName = projectsViewModel.selectedItem(selectedIndex).projectName
                    projectStartTime = projectsViewModel.selectedItem(selectedIndex).projectStartTime
                    projectEndTime = projectsViewModel.selectedItem(selectedIndex).projectEndTime
                    projectTime = projectsViewModel.selectedItem(selectedIndex).projectTime
                    kilometres = projectsViewModel.selectedItem(selectedIndex).kilometres
                    allowance = projectsViewModel.selectedItem(selectedIndex).allowance
                    workType = projectsViewModel.selectedItem(selectedIndex).workType
                }
                ProjectDialog(
                    onDismissRequest = { openProjectDialogEdit = false },
                    onConfirmation = fun(uiState: ProjectListItemUiState) {
                        uiState.index = selectedIndex
                        uiState.initBalance = "-" + editWorkDayViewModel.getWorkTimeToday()
                        projectsViewModel.editItem(uiState)
                        openProjectDialogEdit = false
                        areItemsOverLapping = projectsViewModel.areItemsOverlapping(
                            uiState.projectStartTime,
                            uiState.projectEndTime
                        )
                    },
                    uiState = ProjectListItemUiState(
                        projectName = projectName,
                        projectStartTime = projectStartTime,
                        projectEndTime = projectEndTime,
                        projectTime = projectTime,
                        kilometres = kilometres,
                        allowance = allowance,
                        workType = workType,
                        titleId = R.string.edit,
                        leftOvers = projectsViewModel.leftOvers(projectTime)
                    ),
                    workTypeDropDownList = dropDownWorkTypes
                )
            }
        }
    }
}

@Composable
fun ProjectsList(
    selectedIndex: Int,
    items: SnapshotStateList<ProjectListItemUiState>,
    setSelectedIndex: (Int) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        // LazyColumn to display the list
        LazyColumn(
            modifier = Modifier.height(400.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp), // Spacing between items

        ) {
            items(
                items = items
            ) { item ->
                ListItem(
                    item = item,
                    selectedIndex = selectedIndex,
                    setSelectedIndex = setSelectedIndex
                )
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun ListItem(item: ProjectListItemUiState, selectedIndex: Int, setSelectedIndex: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp) // Add horizontal padding
            .clip(RoundedCornerShape(8.dp)) // Rounded corners
            .background(Color.LightGray)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) // Simple border
            .selectable(
                selected = selectedIndex == item.index,
                onClick = {
                    setSelectedIndex(item.index)
                }
            )
            .background(
                if (selectedIndex == item.index) {
                    Color.Gray
                } else {
                    Color.Transparent
                }
            )
            .padding(16.dp) // Padding inside the box
    ) {
        Row {
            Text(
                text = "${item.index}  ${item.projectName}  ${item.projectStartTime}  " +
                    "${item.projectEndTime}  ${item.kilometres} km  ${item.allowance}  " +
                    item.workType,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
