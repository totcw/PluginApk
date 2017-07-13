package com.lyf.pluginapk;

import android.app.Application;
import android.app.Instrumentation;
import android.content.pm.PackageParser;
import android.os.Environment;

import com.lyf.pluginapkcore.LoadPlugin;
import com.lyf.pluginapkcore.PackageParserManager;
import com.lyf.pluginapkcore.VAInstrumentation;
import com.lyf.pluginapkcore.utils.ReflectUtil;

import java.io.File;

/**
 * 版权：版权所有 (厦门北特达软件有限公司) 2017
 * author : lyf
 * version : 1.0.0
 * email:totcw@qq.com
 * see:
 * 创建日期： 2017/7/13
 * 功能说明：
 * begin
 * 修改记录:
 * 修改后版本:
 * 修改人:
 * 修改内容:
 * end
 */

public class MyApplication extends Application {
    public static  LoadPlugin loadPlugin;
    public static  MyApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        File apk = new File(Environment.getExternalStorageDirectory(), "Test3.apk");
        if (apk.exists()) {
            try {
                 loadPlugin = new LoadPlugin(this, apk);
                //hook Instrumentation类 Instrumentation在Activitythread中
                Instrumentation instrumentation = ReflectUtil.getInstrumentation(this);
                VAInstrumentation vaInstrumentation = new VAInstrumentation(instrumentation,this,loadPlugin);
                Object activityThread = ReflectUtil.getActivityThread(this);
                ReflectUtil.setInstrumentation(activityThread, vaInstrumentation);
                //为了设置application,否则可以不调用
                loadPlugin.invokeApplication(vaInstrumentation);
            } catch (Exception e) {
                System.out.println("加载失败");
                e.printStackTrace();
            }
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
