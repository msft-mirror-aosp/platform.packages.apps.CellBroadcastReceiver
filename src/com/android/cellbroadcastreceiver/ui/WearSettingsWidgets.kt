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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.android.cellbroadcastreceiver.ListWidgetState
import com.android.cellbroadcastreceiver.SwitchWidgetState

/** Composable for showing switch-type of preference on wear material style settings screen. */
@Composable
fun WearSwitchWidget(title: String, state: SwitchWidgetState) {
    ToggleChip(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        checked = state.checked,
        enabled = state.enabled,
        toggleControl = { Switch(checked = state.checked) },
        onCheckedChange = { state.onChange(it) },
        label = { Text(text = title, overflow = TextOverflow.Ellipsis) }
    )
}

/** Composable for showing an action-type of preference on wear material style settings screen. */
@Composable
fun WearActionWidget(title: String, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        label = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        icon = {},
    )
}

private fun value2entry(
    value: String,
    values: List<String>,
    entries: List<String>,
    otherValue: String,
) = values.indexOf(value).let { if ((it >= 0) && (it < entries.size)) entries[it] else otherValue }

/** Composable for showing a list type of preference on wear material style settings screen. */
@Composable
fun WearListWidget(title: String, values: List<String>, entries: List<String>, state: ListWidgetState) {
    var showDialog by remember { mutableStateOf(false) }
    Chip(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        label = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(
                text =
                    value2entry(
                        state.value,
                        values,
                        entries,
                        stringResource(id = androidx.preference.R.string.not_set)
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {},
    )
    val dialogScrollState = rememberScalingLazyListState()

    Dialog(showDialog, onDismissRequest = { showDialog = false }, scrollState = dialogScrollState) {
        Alert(
            scrollState = dialogScrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 24.dp),
            icon = {},
            title = { Text(text = title, textAlign = TextAlign.Center) },
        ) {
            val selectedIndex = values.indexOf(state.value)
            for ((i, text) in entries.withIndex()) {
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = (i == selectedIndex),
                                onClick = {
                                    if (i < values.size) {
                                        state.onChange(values[i])
                                    }
                                    showDialog = false
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (i == selectedIndex),
                            onClick = null // null recommended for accessibility with screenreaders
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.body2.merge(),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
