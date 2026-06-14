package com.akiwiksten.awtimesheet.feature.singleproject

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
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectDropdownFieldsSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectHeaderSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTimeSelectionSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTopBar
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectUpperFieldsSection
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectConfiguration
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenState

@Composable
internal fun SingleProjectScreenContent(
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    hasUnsavedChanges: Boolean,
    config: SingleProjectConfiguration,
    onNavigateBack: () -> Unit,
    onDiscardAndNavigateBack: () -> Unit = onNavigateBack
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = screenState.uiState is SingleProjectUiState.Loading
    )
    val lastSuccessState = remember { mutableStateOf<SingleProjectUiState.Success?>(value = null) }
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

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
            screenState = screenState,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = lastSuccessState.value,
            config = config
        )
    }
}

@Composable
private fun SingleProjectContentByUiState(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?,
    config: SingleProjectConfiguration
) {
    when (screenState.uiState) {
        is SingleProjectUiState.Loading -> SingleProjectLoadingContent(
            padding = padding,
            screenState = screenState,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = cachedSuccessState,
            config = config
        )

        is SingleProjectUiState.Success -> SingleProjectSuccessContent(
            padding = padding,
            screenState = screenState,
            actions = actions,
            uiState = screenState.uiState,
            config = config
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
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?,
    config: SingleProjectConfiguration
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
        )
        return
    }

    SingleProjectSuccessContent(
        padding = padding,
        screenState = screenState,
        actions = actions,
        uiState = cachedSuccessState ?: SingleProjectUiState.Success(
            data = (screenState.uiState as? SingleProjectUiState.Success)?.data ?: screenState.state,
            workTimeByDate = ZERO_TIME,
            workTypes = (screenState.uiState as? SingleProjectUiState.Success)?.workTypes ?: emptyList()
        ),
        config = config
    )
}

@Composable
private fun SingleProjectSuccessContent(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    uiState: SingleProjectUiState,
    config: SingleProjectConfiguration
) {
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
        screenState = screenState.copy(uiState = uiState),
        actions = actions,
        config = config
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
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    config: SingleProjectConfiguration
) {
    val scrollState = rememberScrollState()
    val defaultWorkTypeText = stringResource(id = R.string.other)
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

        if (screenState.isAddMode) {
            NoteBanner(stringResource(id = R.string.project_name_note))
        }

        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
            modifier = Modifier.fillMaxWidth()
        ) {
            SingleProjectFormFields(
                screenState = screenState,
                workTypes = workTypes,
                actions = actions,
                config = config
            )
        }

        Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
    }
}

@Composable
private fun SingleProjectFormFields(
    screenState: SingleProjectScreenState,
    workTypes: List<String>,
    actions: SingleProjectActions,
    config: SingleProjectConfiguration
) {
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
        SingleProjectUpperFieldsSection(
            state = screenState.state,
            isProjectNameEditable = screenState.isProjectNameEditable,
            isDuplicateProjectName = screenState.isDuplicateProjectName,
            isAbsence = isAnyAbsence,
            onStateChange = actions.onStateChange
        )

        SingleProjectTimeSelectionSection(
            state = screenState.state,
            onOpenProjectDetails = actions.onOpenProjectDetails,
            onStateChange = actions.onStateChange
        )

        SingleProjectDropdownFieldsSection(
            state = screenState.state,
            workTypeDropDownList = workTypes,
            isAbsence = isAnyAbsence,
            onStateChange = actions.onStateChange
        )

        AwtButton(
            onClick = actions.onSave,
            enabled = screenState.isConfirmEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }
}
