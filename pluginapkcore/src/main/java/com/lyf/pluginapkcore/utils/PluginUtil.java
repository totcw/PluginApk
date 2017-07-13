/*
 * Copyright (C) 2017 Beijing Didi Infinity Technology and Development Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lyf.pluginapkcore.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.lyf.pluginapkcore.LoadPlugin;

/**
 * Created by renyugang on 16/8/15.
 */
public class PluginUtil {

    public static String getTargetActivity(Intent intent) {
        return intent.getStringExtra(Constants.KEY_TARGET_ACTIVITY);
    }

    public static ComponentName getComponent(Intent intent) {
        return new ComponentName(intent.getStringExtra(Constants.KEY_TARGET_PACKAGE),
                intent.getStringExtra(Constants.KEY_TARGET_ACTIVITY));
    }

    public static boolean isIntentFromPlugin(Intent intent) {
        return intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false);
    }

    public static int getTheme(Context context, Intent intent, LoadPlugin loadPlugin) {
        return PluginUtil.getTheme(context, PluginUtil.getComponent(intent),loadPlugin);
    }

    public static int getTheme(Context context, ComponentName component,LoadPlugin loadPlugin) {

        if (null == loadPlugin) {
            return 0;
        }

        ActivityInfo info = loadPlugin.getActivityInfo(component);
        if (null == info) {
            return 0;
        }

        if (0 != info.theme) {
            return info.theme;
        }

        ApplicationInfo appInfo = info.applicationInfo;
        if (null != appInfo && appInfo.theme != 0) {
            return appInfo.theme;
        }

        return PluginUtil.selectDefaultTheme(0, Build.VERSION.SDK_INT);
    }

    public static int selectDefaultTheme(final int curTheme, final int targetSdkVersion) {
        return selectSystemTheme(curTheme, targetSdkVersion,
                android.R.style.Theme,
                android.R.style.Theme_Holo,
                android.R.style.Theme_DeviceDefault,
                android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
    }

    private static int selectSystemTheme(final int curTheme, final int targetSdkVersion, final int orig, final int holo, final int dark, final int deviceDefault) {
        if (curTheme != 0) {
            return curTheme;
        }

        if (targetSdkVersion < 11 /* Build.VERSION_CODES.HONEYCOMB */) {
            return orig;
        }

        if (targetSdkVersion < 14 /* Build.VERSION_CODES.ICE_CREAM_SANDWICH */) {
            return holo;
        }

        if (targetSdkVersion < 24 /* Build.VERSION_CODES.N */) {
            return dark;
        }

        return deviceDefault;
    }
}
