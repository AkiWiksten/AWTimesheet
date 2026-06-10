package com.akiwiksten.awtimesheet.feature.projectdetails.model

import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsViewModel

fun createProjectDetailsScreenActions(
    viewModel: ProjectDetailsViewModel,
    onConfirm: () -> Unit
): ProjectDetailsScreenActions {
    return ProjectDetailsScreenActions(
        onClearDetails = viewModel.clearDetails,
        onConfirm = onConfirm,
        fieldActions = ProjectDetailsFieldActions(
            startTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.START_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.START_TIME, it) }
            ),
            lunchTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_TIME, it) }
            ),
            endTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.END_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.END_TIME, it) }
            ),
            projectTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.PROJECT_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.PROJECT_TIME, it) }
            ),
            lunchStart = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_START) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_START, it) }
            ),
            lunchEnd = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_END) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_END, it) }
            ),
            breakStart = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.BREAK_START) },
                onSet = { viewModel.updateTime(ProjectDetailsField.BREAK_START, it) }
            ),
            breakEnd = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.BREAK_END) },
                onSet = { viewModel.updateTime(ProjectDetailsField.BREAK_END, it) }
            )
        )
    )
}
