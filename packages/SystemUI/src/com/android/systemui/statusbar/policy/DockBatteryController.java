/*
 * Copyright (C) 2010 The Android Open Source Project
 * This code has been modified. Portions copyright (C) 2012, ParanoidAndroid Project.
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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.ColorUtils;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public class DockBatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.DockBatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    private static final int BATTERY_STYLE_NORMAL = 0;

    private static final int BATTERY_ICON_STYLE_UNKNOWN     = R.drawable.stat_sys_kb_battery_unknown;
    private static final int BATTERY_ICON_STYLE_NORMAL      = R.drawable.stat_sys_kb_battery;
    private static final int BATTERY_ICON_STYLE_CHARGE      = R.drawable.stat_sys_kb_battery_charge;

    private int mBatteryStyle;
    private int mLevel;
    private boolean mDockStatus = false;
    private boolean mDockCharging = false;
    private ColorUtils.ColorSettingInfo mColorInfo;

    private Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public DockBatteryController(Context context) {
        mContext = context;
        mHandler = new Handler();

        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        updateSettings();

        mColorInfo = ColorUtils.getColorSettingInfo(context, Settings.System.STATUS_ICON_COLOR);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void setColor(ColorUtils.ColorSettingInfo colorInfo) {
        mColorInfo = colorInfo;
        updateBatteryLevel();
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            mLevel = intent.getIntExtra(BatteryManager.EXTRA_DOCK_LEVEL, 0);
            mDockCharging = intent.getIntExtra(BatteryManager.EXTRA_DOCK_STATUS, 0) == BatteryManager.DOCK_STATE_CHARGING;
            mDockStatus = intent.getIntExtra(BatteryManager.EXTRA_DOCK_STATUS, 0) == BatteryManager.DOCK_STATE_DOCKED;
            updateBatteryLevel();
        }
    }

    public void updateBatteryLevel() {
        final int icon = mDockCharging ? BATTERY_ICON_STYLE_CHARGE
                : BATTERY_ICON_STYLE_NORMAL;

        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            Drawable batteryBitmap = mContext.getResources().getDrawable(icon);
            if (mColorInfo.isLastColorNull) {
                batteryBitmap.clearColorFilter();
            } else {
                batteryBitmap.setColorFilter(mColorInfo.lastColor, PorterDuff.Mode.SRC_IN);
            }
            v.setImageDrawable(batteryBitmap);
            v.setImageLevel(mLevel);
            v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                    mLevel));
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                    mLevel));
        }

        updateBattery();
    }

    private void updateBattery() {
        int icon = View.GONE;
        int iconStyle = BATTERY_ICON_STYLE_UNKNOWN;

        if (mDockStatus && mBatteryStyle == BATTERY_STYLE_NORMAL) {
            icon = View.VISIBLE;

            if (mDockCharging) {
                iconStyle = BATTERY_ICON_STYLE_NORMAL;
            } else {
                iconStyle = BATTERY_ICON_STYLE_CHARGE;
            }
        }

        int N = mIconViews.size();
        for (int i = 0; i < N; i++) {
            ImageView v = mIconViews.get(i);
            v.setVisibility(icon);
            v.setImageResource(iconStyle);
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mBatteryStyle = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY, BATTERY_STYLE_NORMAL));

        updateBattery();
    }
}
