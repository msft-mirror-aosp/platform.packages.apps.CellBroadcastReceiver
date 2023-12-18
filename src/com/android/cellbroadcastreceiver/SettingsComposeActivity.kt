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

import android.app.backup.BackupManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.android.cellbroadcastreceiver.ui.WearSettingsScreen
import com.android.cellbroadcastreceiver.ui.defaultSettingsScreenConfig
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val TAG = "CBR.Settings"

const val AREA_UPDATE_INFO_ENABLED_ACTION =
    "com.android.cellbroadcastreceiver.action.AREA_UPDATE_INFO_ENABLED"

/**
 * Settings activity for the cell broadcast receiver using Compose.
 *
 * Note: currently Compose settings screen is only provided for Wear devices but activity
 * can be extended to all platforms.
 */
class SettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        val config = defaultSettingsScreenConfig(this)
        setContent {
            val model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            LaunchedEffect(model) {
                launch {
                    snapshotFlow { model.areaUpdateInfoCheckBox.checked }
                        .drop(1) // drop initial value, only notify changes
                        .collect { notifyAreaInfoUpdate(it) }
                }

                launch {
                    snapshotFlow { model.preferenceChangedByUser }
                        .collect { if (it) onPreferenceChangedByUser() }
                }
            }

            WearSettingsScreen(
                model = model,
                config = config,
                onAlertHistoryClick = {
                    val intent = Intent(this, CellBroadcastListActivity::class.java)
                    startActivity(intent)
                }
            )
        }
    }

    private fun notifyAreaInfoUpdate(enabled: Boolean) {
        Log.d(TAG, "notifyAreaInfoUpdate $enabled")
        val areaInfoIntent = Intent(AREA_UPDATE_INFO_ENABLED_ACTION)
        areaInfoIntent.putExtra("enable", enabled)
        // sending broadcast protected by the permission which is only
        // granted for CBR mainline module.
        sendBroadcast(
            areaInfoIntent,
            "com.android.cellbroadcastservice.FULL_ACCESS_CELL_BROADCAST_HISTORY"
        )
    }

    private fun onPreferenceChangedByUser() {
        // logic based on CellBroadcastSettings.onPreferenceChangedByUser
        Log.d(TAG, "onPreferenceChangedByUser")
        CellBroadcastReceiver.startConfigService(
            this,
            CellBroadcastConfigService.ACTION_ENABLE_CHANNELS
        )
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean("any_preference_changed_by_user", true)
            .apply()

        // Notify backup manager a backup pass is needed.
        BackupManager(this).dataChanged()
    }
}
