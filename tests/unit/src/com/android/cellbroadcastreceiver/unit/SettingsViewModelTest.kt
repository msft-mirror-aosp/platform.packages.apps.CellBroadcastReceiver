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

package com.android.cellbroadcastreceiver.unit

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.cellbroadcastreceiver.CellBroadcastSettings.KEY_OVERRIDE_DND_SETTINGS_CHANGED
import com.android.cellbroadcastreceiver.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var sharedPreferences: SharedPreferences
    lateinit var resources: Resources

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        resources = context.resources
    }

    @Test
    fun testSetChecked_writesPreferences() {
        sharedPreferences.edit().putBoolean("enable_alert_vibrate", true).commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        model.enableVibrateCheckBox.setChecked(false)

        assertFalse(model.enableVibrateCheckBox.checked)
        assertFalse(sharedPreferences.getBoolean("enable_alert_vibrate", true))
    }

    @Test
    fun testSwitchPreferenceModel_readSharedPreferences() {
        sharedPreferences.edit().putBoolean("enable_alert_vibrate", false).commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        assertFalse(model.enableVibrateCheckBox.checked)
    }

    @Test
    fun testListPreferenceModelOnChange_writesPreferences() {
        sharedPreferences.edit().putString("alert_reminder_interval", "0").commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        model.reminderInterval.onChange("1")

        assertEquals("1", model.reminderInterval.value)
        assertEquals("1", sharedPreferences.getString("alert_reminder_interval", ""))
    }

    @Test
    fun testListPreferenceModel_readSharedPreferences() {
        sharedPreferences.edit().putString("alert_reminder_interval", "2").commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        assertEquals("2", model.reminderInterval.value)
    }

    @Test
    fun testChecked_defaultReturned_whenMissing() {
        sharedPreferences.edit().remove("enable_alert_vibrate").commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        assertFalse(model.testCheckBox.checked)
    }

    @Test
    fun testListPreferenceModelValue_defaultReturned_whenMissing() {
        sharedPreferences.edit().remove("alert_reminder_interval").commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        // Assuming default config
        assertEquals("0", model.reminderInterval.value)
    }

    @Test
    fun testExtremeCheckBox_disablesSevereCheckBox_onChangeUnchecked() {
        // Assuming default config: disable_severe_when_extreme_disabled is true
        sharedPreferences
            .edit()
            .putBoolean("enable_cmas_severe_threat_alerts", true)
            .putBoolean("enable_cmas_extreme_threat_alerts", true)
            .commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        model.extremeCheckBox.onChange(false)

        assertFalse(model.severeCheckBox.enabled)
        assertFalse(model.severeCheckBox.checked)
    }

    @Test
    fun testOverrideDndCheckBox_disablesVibrateCheckBox_onChangedChecked() {
        sharedPreferences.edit().putBoolean("override_dnd", false).commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        model.overrideDndCheckBox.onChange(true)

        assertFalse(model.enableVibrateCheckBox.enabled)
        assertTrue(model.enableVibrateCheckBox.checked)
        assertTrue(sharedPreferences.getBoolean(KEY_OVERRIDE_DND_SETTINGS_CHANGED, false))
    }

    @Test
    fun testMasterToggle_disablesAlerts_onChangedUnchecked() {
        sharedPreferences.edit().putBoolean("enable_alerts_master_toggle", true).commit()
        val model = SettingsViewModel(sharedPreferences, resources)

        model.masterToggle.onChange(false)

        assertFalse(model.emergencyAlertsCheckBox.enabled)
        assertFalse(model.emergencyAlertsCheckBox.checked)

        assertFalse(model.amberCheckBox.enabled)
        assertFalse(model.amberCheckBox.checked)

        assertFalse(model.severeCheckBox.enabled)
        assertFalse(model.severeCheckBox.checked)

        assertFalse(model.areaUpdateInfoCheckBox.enabled)
        assertFalse(model.areaUpdateInfoCheckBox.checked)
    }
}
