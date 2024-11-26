/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.cellbroadcastreceiver.unit;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.database.Cursor;
import android.os.UserManager;
import android.os.Vibrator;

import com.android.cellbroadcastreceiver.CellBroadcastSearchIndexableProvider;
import com.android.cellbroadcastreceiver.R;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CellBroadcastSearchIndexableProviderTest extends CellBroadcastTest {
    CellBroadcastSearchIndexableProvider mSearchIndexableProvider;

    @Mock
    UserManager mUserManager;

    @Before
    public void setUp() throws Exception {
        super.setUp(getClass().getSimpleName());
        mSearchIndexableProvider = spy(new CellBroadcastSearchIndexableProvider());
        doReturn(mContext).when(mSearchIndexableProvider).getContextMethod();
        doReturn(false).when(mSearchIndexableProvider).isAutomotive();
        doReturn(false).when(mSearchIndexableProvider).isDisableAllCbMessages();
        doReturn("testPackageName").when(mContext).getPackageName();
        doReturn(mResources).when(mSearchIndexableProvider).getResourcesMethod();
        doReturn("testString").when(mResources).getString(anyInt());
        doReturn(null).when(mSearchIndexableProvider).queryRawData(null);
        doReturn(Context.USER_SERVICE).when(mContext).getSystemServiceName(UserManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(true).when(mUserManager).isAdminUser();
    }

    @Test
    public void testQueryXmlResources() {
        Cursor cursor = mSearchIndexableProvider.queryXmlResources(null);
        assertThat(cursor.getCount()).isEqualTo(
                CellBroadcastSearchIndexableProvider.INDEXABLE_RES.length);
    }

    @Test
    public void testQueryRawData() {
        Cursor cursor = mSearchIndexableProvider.queryRawData(new String[]{""});

        verify(mResources, times(2)).getString(R.string.sms_cb_settings);
        verify(mResources, times(2 + CellBroadcastSearchIndexableProvider
                .INDEXABLE_KEYWORDS_RESOURCES.length)).getString(anyInt());
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Mock
    Vibrator mVibrator;

    @Test
    public void testQueryNonIndexableKeys() {
        doReturn(false).when(mSearchIndexableProvider).isTestAlertsToggleVisible();
        doReturn(false).when(mResources).getBoolean(anyInt());
        doReturn("").when(mResources).getString(anyInt());
        doReturn("test").when(mContext).getSystemServiceName(Vibrator.class);
        doReturn(mVibrator).when(mContext).getSystemService("test");
        doReturn(true).when(mVibrator).hasVibrator();
        doReturn(false).when(mSearchIndexableProvider).isShowFullScreenMessageVisible(mResources);
        doReturn(false).when(mSearchIndexableProvider)
                .isExerciseTestAlertsToggleVisible(any());
        doReturn(false).when(mSearchIndexableProvider)
                .isOperatorTestAlertsToggleVisible(any());
        Cursor cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});

        //KEY_RECEIVE_CMAS_IN_SECOND_LANGUAGE
        //KEY_ENABLE_TEST_ALERTS
        //KEY_ENABLE_STATE_LOCAL_TEST_ALERTS
        //KEY_ENABLE_PUBLIC_SAFETY_MESSAGES
        //KEY_ENABLE_EMERGENCY_ALERTS
        //KEY_ENABLE_CMAS_AMBER_ALERTS
        //KEY_ENABLE_AREA_UPDATE_INFO_ALERTSf
        //KEY_ENABLE_CMAS_AMBER_ALERTS
        //KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS
        //KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS
        //KEY_ENABLE_ALERT_SPEECH
        //KEY_ENABLE_CMAS_PRESIDENTIAL_ALERTS
        //KEY_ENABLE_ALERTS_MASTER_TOGGLE
        //KEY_OVERRIDE_DND
        //KEY_ENABLE_EXERCISE_ALERTS
        //KEY_OPERATOR_DEFINED_ALERTS
        assertThat(cursor.getCount()).isEqualTo(17);

        doReturn(false).when(mVibrator).hasVibrator();
        //KEY_ENABLE_ALERT_VIBRATE
        cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});
        assertThat(cursor.getCount()).isEqualTo(18);

        doReturn(true).when(mSearchIndexableProvider).isTestAlertsToggleVisible();
        //KEY_ENABLE_TEST_ALERTS
        cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});
        assertThat(cursor.getCount()).isEqualTo(17);

        doReturn(true).when(mSearchIndexableProvider).isShowFullScreenMessageVisible(mResources);
        //KEY_ENABLE_TEST_ALERTS
        cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});
        assertThat(cursor.getCount()).isEqualTo(16);

        doReturn(true).when(mSearchIndexableProvider)
                .isExerciseTestAlertsToggleVisible(any());
        doReturn(true).when(mSearchIndexableProvider)
                .isOperatorTestAlertsToggleVisible(any());
        cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});
        assertThat(cursor.getCount()).isEqualTo(14);
    }

    @Test
    public void testQueryNonIndexableKeysWithNonAdminMode() {
        doReturn(false).when(mSearchIndexableProvider).isTestAlertsToggleVisible();
        doReturn(false).when(mResources).getBoolean(anyInt());
        doReturn("").when(mResources).getString(anyInt());
        doReturn("test").when(mContext).getSystemServiceName(Vibrator.class);
        doReturn(mVibrator).when(mContext).getSystemService("test");
        doReturn(true).when(mVibrator).hasVibrator();
        doReturn(false).when(mSearchIndexableProvider).isShowFullScreenMessageVisible(mResources);
        doReturn(false).when(mSearchIndexableProvider)
                .isExerciseTestAlertsToggleVisible(any());
        doReturn(false).when(mSearchIndexableProvider)
                .isOperatorTestAlertsToggleVisible(any());
        doReturn(false).when(mUserManager).isAdminUser();
        Cursor cursor = mSearchIndexableProvider.queryNonIndexableKeys(new String[]{""});

        //KEY_RECEIVE_CMAS_IN_SECOND_LANGUAGE
        //KEY_ENABLE_TEST_ALERTS
        //KEY_ENABLE_STATE_LOCAL_TEST_ALERTS
        //KEY_ENABLE_PUBLIC_SAFETY_MESSAGES
        //KEY_ENABLE_EMERGENCY_ALERTS
        //KEY_ENABLE_CMAS_AMBER_ALERTS
        //KEY_ENABLE_AREA_UPDATE_INFO_ALERTS
        //KEY_ENABLE_CMAS_AMBER_ALERTS
        //KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS
        //KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS
        //KEY_ENABLE_ALERT_SPEECH
        //KEY_ENABLE_CMAS_PRESIDENTIAL_ALERTS
        //KEY_ENABLE_ALERTS_MASTER_TOGGLE
        //KEY_OVERRIDE_DND
        //KEY_ENABLE_EXERCISE_ALERTS
        //KEY_OPERATOR_DEFINED_ALERTS
        //KEY_EMERGENCY_ALERT_HISTORY
        //KEY_ALERT_REMINDER_INTERVAL
        //TITLE
        assertThat(cursor.getCount()).isEqualTo(21);
    }
}
