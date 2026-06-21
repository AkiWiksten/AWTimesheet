package com.akiwiksten.awtimesheet.feature.singleproject

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.NoteBanner
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectCommentField
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectDownSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectHeaderSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectProjectNameField
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTimeSelectionSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTopBar
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenParams
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
internal fun SingleProjectScreenContent(
    params: SingleProjectScreenParams,
    hasUnsavedChanges: Boolean,
    onNavigateBack: () -> Unit,
    onDiscardAndNavigateBack: () -> Unit = onNavigateBack
) {
    val screenState = params.screenState
    val actions = params.actions
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = screenState.uiState is SingleProjectUiState.Loading
    )
    val lastSuccessState = remember { mutableStateOf<SingleProjectUiState.Success?>(value = null) }
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = CoreR.string.unsaved_data_message)

    LaunchedEffect(screenState.uiState) {
        if (screenState.uiState is SingleProjectUiState.Success) {
            lastSuccessState.value = screenState.uiState
        }
    }

    val guardedNavigateBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialogState.value = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = guardedNavigateBack)

    if (showUnsavedDialogState.value) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedDialogState.value = false },
            onDiscard = onDiscardAndNavigateBack,
            onSave = actions.onSave,
            dialogText = unsavedMessage,
            isSaveEnabled = screenState.isConfirmEnabled
        )
    }

    Scaffold(
        topBar = {
            SingleProjectTopSection(onNavigateBack = guardedNavigateBack)
        }
    ) { padding ->
        SingleProjectContentByUiState(
            padding = padding,
            params = params,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = lastSuccessState.value,
            hasUnsavedChanges = hasUnsavedChanges
        )
    }
}

@Composable
private fun SingleProjectContentByUiState(
    padding: PaddingValues,
    params: SingleProjectScreenParams,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?,
    hasUnsavedChanges: Boolean
) {
    val screenState = params.screenState
    when (screenState.uiState) {
        is SingleProjectUiState.Loading -> SingleProjectLoadingContent(
            padding = padding,
            params = params,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = cachedSuccessState,
            hasUnsavedChanges = hasUnsavedChanges
        )

        is SingleProjectUiState.Success -> SingleProjectSuccessContent(
            padding = padding,
            params = params,
            uiState = screenState.uiState,
            hasUnsavedChanges = hasUnsavedChanges
        )

        is SingleProjectUiState.Error -> CenteredErrorBox(
            errorMessage = screenState.uiState.message,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
        )
    }
}

@Composable
private fun SingleProjectLoadingContent(
    padding: PaddingValues,
    params: SingleProjectScreenParams,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?,
    hasUnsavedChanges: Boolean
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
        )
        return
    }

    val screenState = params.screenState
    SingleProjectSuccessContent(
        padding = padding,
        params = params,
        uiState = cachedSuccessState ?: SingleProjectUiState.Success(
            data = (screenState.uiState as? SingleProjectUiState.Success)?.data ?: screenState.state,
            workTimeByDate = ZERO_TIME,
            workTypes = (screenState.uiState as? SingleProjectUiState.Success)?.workTypes ?: emptyList()
        ),
        hasUnsavedChanges = hasUnsavedChanges
    )
}

@Composable
private fun SingleProjectSuccessContent(
    padding: PaddingValues,
    params: SingleProjectScreenParams,
    uiState: SingleProjectUiState,
    hasUnsavedChanges: Boolean
) {
    val screenState = params.screenState
    val successState = uiState as? SingleProjectUiState.Success
    val originalProjectTime = successState?.data?.projectTime ?: ""
    val baseWithoutCurrent = WorkTimeCalculator.calculateFlexTime(
        initialTime = successState?.workTimeByDate ?: ZERO_TIME,
        addedTime = "-$originalProjectTime"
    )
    val workTimeByDate = WorkTimeCalculator.calculateFlexTime(
        initialTime = baseWithoutCurrent,
        addedTime = screenState.state.projectTime
    )

    SingleProjectContent(
        padding = padding,
        workTimeByDate = workTimeByDate,
        params = params.copy(screenState = screenState.copy(uiState = uiState)),
        hasUnsavedChanges = hasUnsavedChanges
    )
}

@Composable
private fun SingleProjectTopSection(
    onNavigateBack: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            SingleProjectTopBar(onNavigateBack = onNavigateBack)
        }
    }
}

@Composable
private fun SingleProjectContent(
    padding: PaddingValues,
    workTimeByDate: String,
    params: SingleProjectScreenParams,
    hasUnsavedChanges: Boolean
) {
    val screenState = params.screenState
    val scrollState = rememberScrollState()
    val defaultWorkTypeText = stringResource(id = CoreR.string.other)
    val workTypes =
        (
            ((screenState.uiState as? SingleProjectUiState.Success)?.workTypes ?: emptyList()) +
                defaultWorkTypeText
            )
            .distinct()
            .sorted()

    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            columnModifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
        )
    ) {
        SingleProjectHeaderSection(
            date = screenState.date,
            workTimeByDate = workTimeByDate
        )

        if (!screenState.isAddMode && hasUnsavedChanges) {
            NoteBanner(text = stringResource(id = CoreR.string.edit_mode_modified_note))
        }

        if (screenState.isTimePickerDisabled) {
            NoteBanner(text = stringResource(id = R.string.pick_disabled_note))
        }

        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
            modifier = Modifier.fillMaxWidth()
        ) {
            SingleProjectFormFields(
                workTypes = workTypes,
                params = params
            )
        }

        Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
    }
}

@Composable
private fun SingleProjectFormFields(
    workTypes: List<String>,
    params: SingleProjectScreenParams
) {
    val screenState = params.screenState
    val actions = params.actions
    val config = params.config
    val isFlexDay = screenState.state.workType.equals(config.flexDayWorkType, ignoreCase = true)
    val isAbsence = config.absencePrefix.isNotEmpty() &&
        screenState.state.workType.startsWith(prefix = config.absencePrefix, ignoreCase = true)
    val isAnyAbsence = isFlexDay || isAbsence

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = PADDING_SPACING),
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SingleProjectProjectNameField(
            projectName = screenState.state.projectName,
            onProjectNameChange = { actions.onStateChange(screenState.state.copy(projectName = it)) },
            isEditable = screenState.isProjectNameEditable,
            isError = screenState.isDuplicateProjectName
        )

        SingleProjectTimeSelectionSection(
            state = screenState.state,
            onOpenProjectDetails = actions.onOpenProjectDetails,
            onStateChange = actions.onStateChange,
            isTimePickerDisabled = screenState.isTimePickerDisabled
        )

        SingleProjectDownSection(
            state = screenState.state,
            workTypeDropDownList = workTypes,
            isAbsence = isAnyAbsence,
            onStateChange = actions.onStateChange,
            onNavigateToLocationPicker = actions.onNavigateToLocationPicker
        )

        SingleProjectCommentField(
            comment = screenState.state.comment,
            onCommentChange = { actions.onStateChange(screenState.state.copy(comment = it)) }
        )

        AwtButton(
            onClick = actions.onSave,
            enabled = screenState.isConfirmEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = CoreR.string.save),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
