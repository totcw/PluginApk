package com.lyf.pluginapkcore;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Process;
import android.support.annotation.WorkerThread;

import com.lyf.pluginapkcore.utils.Constants;
import com.lyf.pluginapkcore.utils.DexUtil;
import com.lyf.pluginapkcore.utils.PluginUtil;
import com.lyf.pluginapkcore.utils.ReflectUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * 版权：版权所有 (厦门北特达软件有限公司) 2017
 * author : lyf
 * version : 1.0.0
 * email:totcw@qq.com
 * see:
 * 创建日期： 2017/7/13
 * 功能说明：表示一个插件apk的类
 * begin
 * 修改记录:
 * 修改后版本:
 * 修改人:
 * 修改内容:
 * end
 */

public class LoadPlugin {
    private final File mNativeLibDir;
    private Context mHostContext;
    private final PackageParser.Package mPackage;
    private final PackageInfo mPackageInfo;
    private AssetManager mAssets;
    private Resources mResources;
    private ClassLoader mClassLoader;
    private Context mPluginContext;
    private Map<ComponentName, ActivityInfo> mActivityInfos;
    private Application mApplication;


    public LoadPlugin(Context context, File apk) throws PackageParser.PackageParserException {
        this.mHostContext = context;
        //加载插件apk
        mPackage = PackageParserManager.parsePackage(mHostContext, apk, PackageParser.PARSE_MUST_BE_APK);
        mPackageInfo = new PackageInfo();
        this.mPackageInfo.applicationInfo = this.mPackage.applicationInfo;
        this.mPackageInfo.applicationInfo.sourceDir = apk.getAbsolutePath();
        this.mPackageInfo.signatures = this.mPackage.mSignatures;
        this.mPackageInfo.packageName = this.mPackage.packageName;
        this.mPackageInfo.versionCode = this.mPackage.mVersionCode;
        this.mPackageInfo.versionName = this.mPackage.mVersionName;
        this.mPackageInfo.permissions = new PermissionInfo[0];
        //封装插件的context
        this.mPluginContext = new PluginContext(this,mHostContext);
        //创建插件的resource
        this.mResources = createResources(context, apk);

        this.mNativeLibDir = context.getDir(Constants.NATIVE_DIR, Context.MODE_PRIVATE);
        //设置插件的assets
        this.mAssets = this.mResources.getAssets();
        //创建插件的类加载器
        this.mClassLoader = createClassLoader(context, apk, this.mNativeLibDir, context.getClassLoader());

        Map<ComponentName, ActivityInfo> activityInfos = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity activity : this.mPackage.activities) {
            activityInfos.put(activity.getComponentName(), activity.info);
        }
        this.mActivityInfos = Collections.unmodifiableMap(activityInfos);
        this.mPackageInfo.activities = activityInfos.values().toArray(new ActivityInfo[activityInfos.size()]);

    }

    @WorkerThread
    private static Resources createResources(Context context, File apk) {
        if (Constants.COMBINE_RESOURCES) {
            Resources resources = new ResourcesManager().createResources(context, apk.getAbsolutePath());
            ResourcesManager.hookResources(context, resources);
            return resources;
        } else {
            Resources hostResources = context.getResources();
            AssetManager assetManager = createAssetManager(context, apk);
            return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        }
    }

    private static AssetManager createAssetManager(Context context, File apk) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            ReflectUtil.invoke(AssetManager.class, am, "addAssetPath", apk.getAbsolutePath());
            return am;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private  ClassLoader createClassLoader(Context context, File apk, File libsDir, ClassLoader parent) {
        File dexOutputDir = context.getDir(Constants.OPTIMIZE_DIR, Context.MODE_PRIVATE);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(apk.getAbsolutePath(), dexOutputPath, libsDir.getAbsolutePath(), parent);

        if (Constants.COMBINE_CLASSLOADER) {
            try {
                DexUtil.insertDex(loader,mHostContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return loader;
    }



    public String getPackageName() {
        return this.mPackage.packageName;
    }


    public AssetManager getAssets() {
        return this.mAssets;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }


    public Context getHostContext() {
        return this.mHostContext;
    }

    public Context getPluginContext() {
        return this.mPluginContext;
    }

    public Resources.Theme getTheme() {
        Resources.Theme theme = this.mResources.newTheme();
        theme.applyStyle(PluginUtil.selectDefaultTheme(this.mPackage.applicationInfo.theme, Build.VERSION.SDK_INT), false);
        return theme;
    }

    public void setTheme(int resid) {
        try {
            ReflectUtil.setField(Resources.class, this.mResources, "mThemeResId", resid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPackageResourcePath() {
        int myUid = Process.myUid();
        ApplicationInfo appInfo = this.mPackage.applicationInfo;
        return appInfo.uid == myUid ? appInfo.sourceDir : appInfo.publicSourceDir;
    }

    public String getCodePath() {
        return this.mPackage.applicationInfo.sourceDir;
    }



    public ActivityInfo getActivityInfo(ComponentName componentName) {
        return this.mActivityInfos.get(componentName);
    }





    public Application getApplication() {
        return mApplication;
    }

    public void invokeApplication(Instrumentation instrumentation) {
        if (mApplication != null) {
            return;
        }

        mApplication = makeApplication(false, instrumentation);
    }



    public Intent getLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, "") > 0) {
                    return Intent.makeMainActivity(activity.getComponentName());
                }
            }
        }

        return null;
    }

    public Intent getLeanbackLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, "") > 0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(activity.getComponentName());
                    intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    return intent;
                }
            }
        }

        return null;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mPackage.applicationInfo;
    }

    public PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }



    private Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) {
        if (null != this.mApplication) {
            return this.mApplication;
        }

        String appClass = this.mPackage.applicationInfo.className;
        if (forceDefaultAppClass || null == appClass) {
            appClass = "android.app.Application";
        }

        try {
            this.mApplication = instrumentation.newApplication(this.mClassLoader, appClass, this.getPluginContext());
            instrumentation.callApplicationOnCreate(this.mApplication);
            return this.mApplication;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
