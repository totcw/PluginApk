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

package com.lyf.pluginapkcore;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.lyf.pluginapkcore.utils.Constants;
import com.lyf.pluginapkcore.utils.PluginUtil;
import com.lyf.pluginapkcore.utils.ReflectUtil;


/**
 * Created by renyugang on 16/8/10.
 */
public class VAInstrumentation extends Instrumentation implements Handler.Callback {
    public static final String TAG = "VAInstrumentation";
    public static final int LAUNCH_ACTIVITY         = 100;

    private Instrumentation mBase;
    private Context mContext;
    private LoadPlugin mLoadPlugin;

    public VAInstrumentation(Instrumentation base,Context mContext,LoadPlugin loadPlugin) {

        this.mBase = base;
        this.mContext = mContext;
        this.mLoadPlugin = loadPlugin;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        String targetPackageName = intent.getComponent().getPackageName();
        String targetClassName = intent.getComponent().getClassName();
        // search map and return specific launchmode stub activity
        if (!targetPackageName.equals(mContext.getPackageName()) ) {
            intent.putExtra(Constants.KEY_IS_PLUGIN, true);
            intent.putExtra(Constants.KEY_TARGET_PACKAGE, targetPackageName);
            intent.putExtra(Constants.KEY_TARGET_ACTIVITY, targetClassName);
            dispatchStubActivity(intent);
        }

        ActivityResult result = null;
        try {
            Class[] parameterTypes = {Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class,
                    int.class, Bundle.class};
            result = (ActivityResult) ReflectUtil.invoke(Instrumentation.class, mBase,
                    "execStartActivity", parameterTypes,
                    who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }


    private void dispatchStubActivity(Intent intent) {
        ComponentName component = intent.getComponent();
        String targetClassName = intent.getComponent().getClassName();

        ActivityInfo info = mLoadPlugin.getActivityInfo(component);
        if (info == null) {
            throw new RuntimeException("can not find " + component);
        }

        Resources.Theme themeObj = mLoadPlugin.getResources().newTheme();
        themeObj.applyStyle(info.theme, true);
        String stubActivity = "com.lyf.pluginapk.SecondeActivity";
        Log.i(TAG, String.format("dispatchStubActivity,[%s -> %s]", targetClassName, stubActivity));
        intent.setClassName(mContext, stubActivity);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false)) {
            String targetClassName = PluginUtil.getTargetActivity(intent);
            if (targetClassName != null) {
                Activity activity = mBase.newActivity(mLoadPlugin.getClassLoader(), targetClassName, intent);
                activity.setIntent(intent);

                try {
                    // for 4.1+
                       ReflectUtil.setField(ContextThemeWrapper.class, activity, "mResources", mLoadPlugin.getResources());
                } catch (Exception ignored) {
                    // ignored.
                }

                return activity;
            }
        }
        return mBase.newActivity(cl, className, intent);
      /*  try {
            cl.loadClass(className);
        } catch (ClassNotFoundException e) {



        }

        return mBase.newActivity(cl, className, intent);*/
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        final Intent intent = activity.getIntent();
        if (PluginUtil.isIntentFromPlugin(intent)) {
            System.out.println("插件calloncreate");
            Context base = activity.getBaseContext();
            try {

                ReflectUtil.setField(base.getClass(), base, "mResources", mLoadPlugin.getResources());
                ReflectUtil.setField(ContextWrapper.class, activity, "mBase", mLoadPlugin.getPluginContext());
                ReflectUtil.setField(Activity.class, activity, "mApplication", mLoadPlugin.getApplication());
                ReflectUtil.setFieldNoException(ContextThemeWrapper.class, activity, "mBase", mLoadPlugin.getPluginContext());

                // set screenOrientation
                ActivityInfo activityInfo = mLoadPlugin.getActivityInfo(PluginUtil.getComponent(intent));
                if (activityInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    activity.setRequestedOrientation(activityInfo.screenOrientation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        mBase.callActivityOnCreate(activity, icicle);
    }

    @Override
    public boolean handleMessage(Message msg) {
   /*     if (msg.what == LAUNCH_ACTIVITY) {
            // ActivityClientRecord r
            Object r = msg.obj;
            try {
                Intent intent = (Intent) ReflectUtil.getField(r.getClass(), r, "intent");
                intent.setExtrasClassLoader(VAInstrumentation.class.getClassLoader());
                ActivityInfo activityInfo = (ActivityInfo) ReflectUtil.getField(r.getClass(), r, "activityInfo");

                if (PluginUtil.isIntentFromPlugin(intent)) {
                    int theme = PluginUtil.getTheme(mLoadPlugin.getHostContext(), intent,mLoadPlugin);
                    if (theme != 0) {
                        System.out.println( "resolve theme, current theme:" + activityInfo.theme + "  after :0x" + Integer.toHexString(theme));
                       // activityInfo.theme = theme;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

        return false;
    }

    @Override
    public Context getContext() {
        return mBase.getContext();
    }

    @Override
    public Context getTargetContext() {
        return mBase.getTargetContext();
    }

    @Override
    public ComponentName getComponentName() {
        return mBase.getComponentName();
    }



}
