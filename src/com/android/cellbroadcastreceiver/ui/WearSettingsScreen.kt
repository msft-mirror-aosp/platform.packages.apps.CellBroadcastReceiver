/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cellbroadcastreceiver.ui

import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import com.android.cellbroadcastreceiver.CellBroadcastChannelManager
import com.android.cellbroadcastreceiver.CellBroadcastReceiver
import com.android.cellbroadcastreceiver.CellBroadcastSettings
import com.android.cellbroadcastreceiver.R
import com.android.cellbroadcastreceiver.SettingsViewModel
import kotlinx.coroutines.launch

private const val TAG = "WearSettingsScreen"

/**
 *  Configuration of settings screen.
 *
 *  Can be used to control visibility of preferences depending on current device config.
 */
@Stable
data class SettingsScreenConfig(
    val showMasterToggle: Boolean,
    val showEmergencyAlertsCheckBox: Boolean,
    val showAmberCheckBox: Boolean,
    val showExtremeCheckBox: Boolean,
    val showSevereCheckBox: Boolean,
    val showPresidentialCheckBox: Boolean,
    val showPublicSafetyMessagesChannelCheckBox: Boolean,
    val showPublicSafetyMessagesChannelFullScreenCheckBox: Boolean,
    val showTestCheckBox: Boolean,
    val showExerciseTestCheckBox: Boolean,
    val showOperatorDefinedCheckBox: Boolean,
    val showStateLocalTestCheckBox: Boolean,
    val showEnableVibrateCheckBox: Boolean,
    val showReceiveCmasInSecondLanguageCheckBox: Boolean,
    val showOverrideDndCheckBox: Boolean
)

/** Return settings config based on rules that CellBroadcastReceiver must follow. */
fun defaultSettingsScreenConfig(context: Context): SettingsScreenConfig {
    val resources = CellBroadcastSettings.getResourcesForDefaultSubId(context)
    val channelManager =
        CellBroadcastChannelManager(context, SubscriptionManager.getDefaultSubscriptionId(), null)
    val isTestingMode = CellBroadcastReceiver.isTestingMode(context)
    return SettingsScreenConfig(
        showMasterToggle = resources.getBoolean(R.bool.show_main_switch_settings),
        showEmergencyAlertsCheckBox =
            !channelManager
                .getCellBroadcastChannelRanges(R.array.emergency_alerts_channels_range_strings)
                .isEmpty(),
        showAmberCheckBox =
            resources.getBoolean(R.bool.show_amber_alert_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(R.array.cmas_amber_alerts_channels_range_strings)
                    .isEmpty(),
        showExtremeCheckBox =
            resources.getBoolean(R.bool.show_extreme_alert_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(
                        R.array.cmas_alert_extreme_channels_range_strings
                    )
                    .isEmpty(),
        showSevereCheckBox =
            resources.getBoolean(R.bool.show_severe_alert_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(R.array.cmas_alerts_severe_range_strings)
                    .isEmpty(),
        showPresidentialCheckBox = resources.getBoolean(R.bool.show_presidential_alerts_settings),
        showPublicSafetyMessagesChannelCheckBox =
            resources.getBoolean(R.bool.show_public_safety_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(
                        R.array.public_safety_messages_channels_range_strings
                    )
                    .isEmpty(),
        showPublicSafetyMessagesChannelFullScreenCheckBox =
            resources.getBoolean(R.bool.show_public_safety_full_screen_settings) &&
                resources.getBoolean(R.bool.show_public_safety_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(
                        R.array.public_safety_messages_channels_range_strings
                    )
                    .isEmpty(),
        showTestCheckBox = CellBroadcastSettings.isTestAlertsToggleVisible(context),
        showExerciseTestCheckBox =
            resources.getBoolean(R.bool.show_separate_exercise_settings) &&
                (isTestingMode || resources.getBoolean(R.bool.show_exercise_settings)) &&
                !channelManager
                    .getCellBroadcastChannelRanges(R.array.exercise_alert_range_strings)
                    .isEmpty(),
        showOperatorDefinedCheckBox =
            resources.getBoolean(R.bool.show_separate_operator_defined_settings) &&
                (isTestingMode || resources.getBoolean(R.bool.show_operator_defined_settings)) &&
                !channelManager
                    .getCellBroadcastChannelRanges(R.array.operator_defined_alert_range_strings)
                    .isEmpty(),
        showStateLocalTestCheckBox =
            resources.getBoolean(R.bool.show_state_local_test_settings) &&
                !channelManager
                    .getCellBroadcastChannelRanges(R.array.state_local_test_alert_range_strings)
                    .isEmpty(),
        showEnableVibrateCheckBox =
            CellBroadcastSettings.isVibrationToggleVisible(context, resources),
        showReceiveCmasInSecondLanguageCheckBox =
            !resources.getString(R.string.emergency_alert_second_language_code).isEmpty(),
        showOverrideDndCheckBox = resources.getBoolean(R.bool.show_override_dnd_settings)
    )
}

/** Composable renders Wear material style CellBroadcast Settings screen. */
@Composable
fun WearSettingsScreen(
    model: SettingsViewModel,
    config: SettingsScreenConfig,
    onAlertHistoryClick: () -> Unit
) {
    MaterialTheme {
        val scrollState = rememberScalingLazyListState(0, 0)
        val focusRequester = remember { FocusRequester() } // rememberActiveFocusRequester() ??
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            modifier =
                Modifier.onRotaryScrollEvent {
                        Log.d(TAG, "onRotaryScrollEvent $it")
                        coroutineScope.launch {
                            scrollState.scrollBy(it.verticalScrollPixels)
                            scrollState.animateScrollBy(0f)
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
            timeText = { TimeText(modifier = Modifier.scrollAway(scrollState)) },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = scrollState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = scrollState
            ) {
                if (config.showMasterToggle) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_alerts_master_toggle_title),
                            state = model.masterToggle
                        )
                    }
                    item { Text(stringResource(R.string.enable_alerts_master_toggle_summary)) }
                }
                if (config.showEmergencyAlertsCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_emergency_alerts_message_title),
                            state = model.emergencyAlertsCheckBox
                        )
                    }
                }
                if (config.showAmberCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_cmas_amber_alerts_title),
                            state = model.amberCheckBox
                        )
                    }
                }
                if (config.showExtremeCheckBox) {
                    item {
                        WearSwitchWidget(
                            title =
                                stringResource(R.string.enable_cmas_extreme_threat_alerts_title),
                            state = model.extremeCheckBox
                        )
                    }
                }
                if (config.showSevereCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_cmas_severe_threat_alerts_title),
                            state = model.severeCheckBox
                        )
                    }
                }
                if (config.showPresidentialCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_cmas_presidential_alerts_title),
                            state = model.presidentialCheckBox
                        )
                    }
                }
                if (config.showPublicSafetyMessagesChannelCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_public_safety_messages_title),
                            state = model.publicSafetyMessagesChannelCheckBox
                        )
                    }
                    item { Text(stringResource(R.string.enable_public_safety_messages_summary)) }
                }
                if (config.showPublicSafetyMessagesChannelFullScreenCheckBox) {
                    item {
                        WearSwitchWidget(
                            title =
                                stringResource(
                                    R.string.enable_full_screen_public_safety_messages_title
                                ),
                            state = model.publicSafetyMessagesChannelFullScreenCheckBox
                        )
                    }
                    item {
                        Text(
                            stringResource(
                                R.string.enable_full_screen_public_safety_messages_summary
                            )
                        )
                    }
                }
                if (config.showTestCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_cmas_test_alerts_title),
                            state = model.testCheckBox
                        )
                    }
                }
                if (config.showExerciseTestCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_exercise_test_alerts_title),
                            state = model.exerciseTestCheckBox
                        )
                    }
                    item { Text(stringResource(R.string.enable_exercise_test_alerts_summary)) }
                }
                if (config.showOperatorDefinedCheckBox) {
                    item {
                        WearSwitchWidget(
                            title =
                                stringResource(R.string.enable_operator_defined_test_alerts_title),
                            state = model.operatorDefinedCheckBox
                        )
                    }
                    item {
                        Text(stringResource(R.string.enable_operator_defined_test_alerts_summary))
                    }
                }
                if (config.showStateLocalTestCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_state_local_test_alerts_title),
                            state = model.stateLocalTestCheckBox
                        )
                    }
                }
                item {
                    WearSwitchWidget(
                        title = stringResource(R.string.enable_area_update_info_alerts_title),
                        state = model.areaUpdateInfoCheckBox
                    )
                }
                item { Text(stringResource(R.string.enable_area_update_info_alerts_summary)) }
                item {
                    WearActionWidget(
                        title = stringResource(R.string.emergency_alert_history_title),
                        onClick = onAlertHistoryClick
                    )
                }
                if (config.showEnableVibrateCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.enable_alert_vibrate_title),
                            state = model.enableVibrateCheckBox
                        )
                    }
                }
                if (config.showReceiveCmasInSecondLanguageCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.receive_cmas_in_second_language_title),
                            state = model.receiveCmasInSecondLanguageCheckBox
                        )
                    }
                }
                if (config.showOverrideDndCheckBox) {
                    item {
                        WearSwitchWidget(
                            title = stringResource(R.string.override_dnd_title),
                            state = model.overrideDndCheckBox
                        )
                    }
                    item { Text(stringResource(R.string.override_dnd_summary)) }
                }
                item {
                    WearSwitchWidget(
                        title = stringResource(R.string.enable_alert_speech_title),
                        state = model.speechCheckBox
                    )
                }
                item {
                    WearListWidget(
                        title = stringResource(R.string.alert_reminder_interval_title),
                        values = model.reminderInterval.values,
                        entries = model.reminderInterval.entries,
                        state = model.reminderInterval
                    )
                }
                item {
                    WearSwitchWidget(
                        title = stringResource(R.string.show_cmas_opt_out_title),
                        state = model.showCmasOptOutDialog
                    )
                }
                item { Text(stringResource(R.string.show_cmas_opt_out_summary)) }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}
