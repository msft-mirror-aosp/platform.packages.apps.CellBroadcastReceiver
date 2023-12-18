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

package com.android.cellbroadcastreceiver.unit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.cellbroadcastreceiver.ListWidgetState
import com.android.cellbroadcastreceiver.SwitchWidgetState
import com.android.cellbroadcastreceiver.ui.WearActionWidget
import com.android.cellbroadcastreceiver.ui.WearListWidget
import com.android.cellbroadcastreceiver.ui.WearSwitchWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearSettingsWidgetsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun switchWidget() {
        val state1 = MutableSwitchWidgetState(false)
        composeTestRule.setContent { WearSwitchWidget("switch1", state1) }

        composeTestRule.onNode(hasText("switch1")).assert(isOff())
        composeTestRule.onNode(hasText("switch1")).performClick()
        composeTestRule.onNode(hasText("switch1")).assert(isOn())
        assertTrue(state1.checked)
    }

    @Test
    fun actionWidget() {
        var action1click = false
        composeTestRule.setContent {
            WearActionWidget("action1", onClick = { action1click = true })
        }

        composeTestRule.onNode(hasText("action1")).assertExists()
        composeTestRule.onNode(hasText("action1")).performClick()
        assertTrue(action1click)
    }

    @Test
    fun listWidget() {
        val listState = MutableListWidgetState("1")
        composeTestRule.setContent {
            WearListWidget(
                "list1",
                values = listOf("0", "1", "2"),
                entries = listOf("Item 0", "Item 1", "Item 2"),
                state = listState
            )
        }

        composeTestRule.onNode(hasText("list1") and hasText("Item 1")).performClick()
        // opens dialog.

        composeTestRule.onNode(hasAnyAncestor(isDialog()) and hasText("Item 1")).assertIsSelected()
        composeTestRule.onNode(hasAnyAncestor(isDialog()) and hasText("Item 0")).performClick()

        // Verify dialog is closed.
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNode(hasText("list1") and hasText("Item 0")).assertExists()
        assertEquals("0", listState.value)
    }
}

internal class MutableSwitchWidgetState(
    initialValue: Boolean,
    override val enabled: Boolean = true
) : SwitchWidgetState {
    var _state by mutableStateOf(initialValue)

    override fun onChange(value: Boolean) {
        _state = value
    }

    override val checked: Boolean
        get() = _state
}

internal class MutableListWidgetState(initialValue: String) : ListWidgetState {
    var _state = mutableStateOf(initialValue)

    override fun onChange(newValue: String) {
        _state.value = newValue
    }

    override val value by _state
}
