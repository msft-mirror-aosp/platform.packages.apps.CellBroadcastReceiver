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

package com.android.cellbroadcastreceiver

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.text.TextUtils
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.preference.PreferenceManager
import com.android.cellbroadcastreceiver.CellBroadcastSettings.KEY_OVERRIDE_DND_SETTINGS_CHANGED

/** State for list type of preference. */
@Stable
interface ListWidgetState {
    fun onChange(newValue: String)

    val value: String
}

/** State for switch type of preference. */
@Stable
interface SwitchWidgetState {
    fun onChange(value: Boolean)

    val checked: Boolean
    val enabled: Boolean
}

/** Settings preference screen model. */
class SettingsViewModel(
    private val sharedPreferences: SharedPreferences,
    private val resources: Resources
) : ViewModel() {

    private val reminderIntervalActiveValues =
        resources.getStringArray(R.array.alert_reminder_interval_active_values).toList()

    val reminderInterval = ListPreferenceModel(
        "alert_reminder_interval",
        resources.getString(R.string.alert_reminder_interval_in_min_default),
        reminderIntervalActiveValues,
        getActiveIntervalEntries(
            reminderIntervalActiveValues,
            resources.getStringArray(R.array.alert_reminder_interval_values).toList(),
            resources.getStringArray(R.array.alert_reminder_interval_entries).toList()
        )
    )

    ////////////////////////////////////////////////////////////////////////////////////

    val masterToggle =
        SwitchPreferenceModel(
            "enable_alerts_master_toggle",
            resources.getBoolean(R.bool.master_toggle_enabled_default),
            onPreferenceChanged = this::masterUpdateSubAlerts
        )
    val emergencyAlertsCheckBox =
        SwitchPreferenceModel(
            "enable_emergency_alerts",
            resources.getBoolean(R.bool.emergency_alerts_enabled_default)
        )
    val amberCheckBox =
        SwitchPreferenceModel(
            "enable_cmas_amber_alerts",
            resources.getBoolean(R.bool.amber_alerts_enabled_default)
        )
    val extremeCheckBox =
        SwitchPreferenceModel(
            "enable_cmas_extreme_threat_alerts",
            resources.getBoolean(R.bool.extreme_threat_alerts_enabled_default),
            onPreferenceChanged = {
                if (disableSevereWhenExtremeDisabled) {
                    severeCheckBox.setEnabled(it)
                    severeCheckBox.setChecked(false)
                }
            }
        )
    val severeCheckBox =
        SwitchPreferenceModel(
            "enable_cmas_severe_threat_alerts",
            resources.getBoolean(R.bool.severe_threat_alerts_enabled_default)
        )
    val presidentialCheckBox = SwitchPreferenceModel("enable_cmas_presidential_alerts", true)
    val publicSafetyMessagesChannelCheckBox =
        SwitchPreferenceModel(
            "enable_public_safety_messages",
            resources.getBoolean(R.bool.public_safety_messages_enabled_default)
        )
    val publicSafetyMessagesChannelFullScreenCheckBox =
        SwitchPreferenceModel(
            "enable_public_safety_messages_full_screen",
            resources.getBoolean(R.bool.public_safety_messages_full_screen_enabled_default)
        )
    val testCheckBox = SwitchPreferenceModel("enable_test_alerts", false)
    val exerciseTestCheckBox =
        SwitchPreferenceModel(
            "enable_exercise_alerts",
            resources.getBoolean(R.bool.test_exercise_alerts_enabled_default)
        )
    val operatorDefinedCheckBox =
        SwitchPreferenceModel(
            "enable_operator_defined_alerts",
            resources.getBoolean(R.bool.test_operator_defined_alerts_enabled_default)
        )
    val stateLocalTestCheckBox =
        SwitchPreferenceModel(
            "enable_state_local_test_alerts",
            resources.getBoolean(R.bool.state_local_test_alerts_enabled_default)
        )
    val areaUpdateInfoCheckBox = SwitchPreferenceModel("enable_area_update_info_alerts", true)
    val enableVibrateCheckBox = SwitchPreferenceModel("enable_alert_vibrate", true)
    val receiveCmasInSecondLanguageCheckBox =
        SwitchPreferenceModel("receive_cmas_in_second_language", false)
    val overrideDndCheckBox =
        SwitchPreferenceModel(
            "override_dnd",
            resources.getBoolean(R.bool.override_dnd_default),
            onPreferenceChanged = {
                sharedPreferences.edit().putBoolean(KEY_OVERRIDE_DND_SETTINGS_CHANGED, true).apply()
                updateVibrationPreference(it)
            }
        )
    val speechCheckBox = SwitchPreferenceModel("enable_alert_speech", true)
    val showCmasOptOutDialog = SwitchPreferenceModel("show_cmas_opt_out_dialog", true)

    ////////////////////////////////////////////////////////////////////////////////////
    init {
        if (!masterToggle.checked) {
            masterUpdateSubAlerts(false)
        }
    }

    var preferenceChangedByUser by mutableStateOf(false)
        private set

    val disableSevereWhenExtremeDisabled =
        resources.getBoolean(R.bool.disable_severe_when_extreme_disabled)

    /**
     * Update the vibration preference based on override DND. If DND is overridden, then do not
     * allow users to turn off vibration.
     *
     * @param overrideDnd `true` if the alert will be played at full volume, regardless DND
     *   settings.
     */
    private fun updateVibrationPreference(overrideDnd: Boolean) {
        if (overrideDnd) {
            // If DND is enabled, always enable vibration.
            enableVibrateCheckBox.setChecked(true)
        }
        // Grey out the preference if DND is overridden.
        enableVibrateCheckBox.setEnabled(!overrideDnd)
    }

    /** Updates sub-alert preference widgets enable/disable based on master toggle. */
    private fun masterUpdateSubAlerts(alertsEnabled: Boolean) {
        emergencyAlertsCheckBox.setEnabled(alertsEnabled)
        emergencyAlertsCheckBox.setChecked(alertsEnabled)

        amberCheckBox.setEnabled(alertsEnabled)
        amberCheckBox.setChecked(alertsEnabled)

        if (!resources.getBoolean(R.bool.disable_extreme_alert_settings)) {
            extremeCheckBox.setEnabled(alertsEnabled)
            extremeCheckBox.setChecked(alertsEnabled)
        }

        severeCheckBox.setEnabled(alertsEnabled)
        severeCheckBox.setChecked(alertsEnabled)

        publicSafetyMessagesChannelCheckBox.setEnabled(alertsEnabled)
        publicSafetyMessagesChannelCheckBox.setChecked(alertsEnabled)

        testCheckBox.setEnabled(alertsEnabled)
        testCheckBox.setChecked(alertsEnabled)

        exerciseTestCheckBox.setEnabled(alertsEnabled)
        exerciseTestCheckBox.setChecked(alertsEnabled)

        operatorDefinedCheckBox.setEnabled(alertsEnabled)
        operatorDefinedCheckBox.setChecked(alertsEnabled)

        stateLocalTestCheckBox.setEnabled(alertsEnabled)
        stateLocalTestCheckBox.setChecked(alertsEnabled)

        areaUpdateInfoCheckBox.setEnabled(alertsEnabled)
        areaUpdateInfoCheckBox.setChecked(alertsEnabled)
    }

    private inner class PreferenceStore(val key: String) {
        fun getPersistedBoolean(defaultValue: Boolean) =
            sharedPreferences.getBoolean(key, defaultValue)

        fun getPersistedString(defaultValue: String) =
            sharedPreferences.getString(key, defaultValue) ?: defaultValue

        fun persistString(value: String) {
            sharedPreferences.edit().putString(key, value).apply()
        }

        fun persistBoolean(value: Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }
    }

    inner class SwitchPreferenceModel(
        key: String,
        defaultValue: Boolean,
        val onPreferenceChanged: (newValue: Boolean) -> Unit = {}
    ) : SwitchWidgetState {
        private val store = PreferenceStore(key)
        private val _state = mutableStateOf(store.getPersistedBoolean(defaultValue))
        private var _enabled = mutableStateOf(true)
        private var checkedSet = false // persist logic from TwoStatePreference

        override fun onChange(value: Boolean) {
            if (setChecked(value)) {
                // bubble up event only on user change
                onPreferenceChanged(value) // widget specific event action
                preferenceChangedByUser = true
            }
        }

        fun setChecked(value: Boolean): Boolean {
            val changed = _state.value != value
            _state.value = value
            if (changed || !checkedSet) {
                checkedSet = true
                store.persistBoolean(value)
            }
            return changed
        }

        fun setEnabled(value: Boolean) {
            _enabled.value = value
        }

        override val checked: Boolean by _state
        override val enabled: Boolean by _enabled
    }

    inner class ListPreferenceModel(
        key: String,
        defaultValue: String,
        val values: List<String>,
        val entries: List<String>
    ) : ListWidgetState {
        private val store = PreferenceStore(key)
        private val _state = mutableStateOf(store.getPersistedString(defaultValue))
        private var valueSet = false

        override fun onChange(newValue: String) {
            val changed = !TextUtils.equals(_state.value, newValue)
            if (changed || !valueSet) {
                _state.value = newValue
                valueSet = true
                store.persistString(newValue)
            }
        }

        override val value: String by _state
    }

    private fun getActiveIntervalEntries(
        activeValues: List<String>,
        allValues: List<String>,
        allEntries: List<String>
    ) =
        Array(activeValues.size) {
                val index = allValues.indexOf(activeValues[it])
                if (index != -1) {
                    allEntries.get(index)
                } else {
                    Log.e(TAG, "Can't find ${activeValues.get(it)}")
                    ""
                }
            }
            .toList()

    companion object {
        private val TAG = "SettingsViewModel"

        fun createSettingsModel(context: Context): SettingsViewModel {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val resources = CellBroadcastSettings.getResourcesForDefaultSubId(context)
            return SettingsViewModel(prefs, resources)
        }

        val Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    val application = checkNotNull(extras[APPLICATION_KEY])
                    @Suppress("UNCHECKED_CAST")
                    return createSettingsModel(application.applicationContext) as T
                }
            }
    }
}
