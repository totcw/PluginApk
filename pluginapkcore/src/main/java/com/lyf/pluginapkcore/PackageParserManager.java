package com.lyf.pluginapkcore;

import android.content.Context;
import android.content.pm.PackageParser;
import android.os.Build;

import com.lyf.pluginapkcore.utils.ReflectUtil;

import java.io.File;

/**
 * 版权：版权所有 (厦门北特达软件有限公司) 2017
 * author : lyf
 * version : 1.0.0
 * email:totcw@qq.com
 * see:
 * 创建日期： 2017/7/12
 * 功能说明： apk解析器
 * begin
 * 修改记录:
 * 修改后版本:
 * 修改人:
 * 修改内容:
 * end
 */

public class PackageParserManager {



    public static final PackageParser.Package parsePackage(final Context context, final File apk, final int flags) throws PackageParser.PackageParserException {
        if (Build.VERSION.SDK_INT >= 24) {
            return PackageParserV24.parsePackage(context, apk, flags);
        } else if (Build.VERSION.SDK_INT >= 21) {
            return PackageParserLollipop.parsePackage(context, apk, flags);
        } else {
            return PackageParserLegacy.parsePackage(context, apk, flags);
        }
    }

    private static final class PackageParserV24 {

        static final PackageParser.Package parsePackage(Context context, File apk, int flags) throws PackageParser.PackageParserException {
            PackageParser parser = new PackageParser();
            PackageParser.Package pkg = parser.parsePackage(apk, flags);
            ReflectUtil.invokeNoException(PackageParser.class, null, "collectCertificates",
                    new Class[]{PackageParser.Package.class, int.class}, pkg, flags);
            return pkg;
        }
    }

    private static final class PackageParserLollipop {

        static final PackageParser.Package parsePackage(final Context context, final File apk, final int flags) throws PackageParser.PackageParserException {
            PackageParser parser = new PackageParser();
            PackageParser.Package pkg = parser.parsePackage(apk, flags);
            try {
                parser.collectCertificates(pkg, flags);
            } catch (Throwable e) {
                // ignored
            }
            return pkg;
        }

    }

    private static final class PackageParserLegacy {

        static final PackageParser.Package parsePackage(Context context, File apk, int flags) {
            PackageParser parser = new PackageParser(apk.getAbsolutePath());
            PackageParser.Package pkg = parser.parsePackage(apk, apk.getAbsolutePath(), context.getResources().getDisplayMetrics(), flags);
            ReflectUtil.invokeNoException(PackageParser.class, parser, "collectCertificates",
                    new Class[]{PackageParser.Package.class, int.class}, pkg, flags);
            return pkg;
        }

    }
}
