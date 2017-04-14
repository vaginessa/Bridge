package moe.shizuku.bridge.utils;

import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import moe.shizuku.bridge.BridgeSettings;
import moe.shizuku.bridge.BuildConfig;

/**
 * Created by Rikka on 2017/4/6.
 */

public class ResolveInfoHelper {

    public static ArrayList<ResolveInfo> filter(Collection<ResolveInfo> resolveInfo, boolean editMode) {
        ArrayList<ResolveInfo> list = new ArrayList<>();
        for (ResolveInfo info : resolveInfo) {
            /*if ((info.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }*/
            if ((info.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID) && info.activityInfo.name.endsWith(".ForwardingActivity"))
                    || info.activityInfo.packageName.startsWith("com.android.")
                    || info.activityInfo.packageName.startsWith("com.google.")
                    || (editMode && info.activityInfo.packageName.equals("android"))) {
                continue;
            }

            if (!editMode
                    && !BridgeSettings.isActivityForward(info.activityInfo.name)
                    && !isForward(info)) {
                continue;
            }

            list.add(info);
        }

        Collections.sort(list, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo o1, ResolveInfo o2) {
                return (o1.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID) && o1.activityInfo.name.endsWith(".SaveActivity") ? -1 : 1)
                        + (o2.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID) && o2.activityInfo.name.endsWith(".SaveActivity") ? 1 : -1);
            }
        });

        return list;
    }

    public static boolean isForward(ResolveInfo resolveInfo) {
        return "android".equals(resolveInfo.activityInfo.packageName)
                && resolveInfo.activityInfo.name.contains("Forward");
    }

    public static ResolveInfo findForward(Collection<ResolveInfo> resolveInfo) {
        for (ResolveInfo info : resolveInfo) {
            if (isForward(info)) {
                return info;
            }
        }

        return null;
    }

    public static ArrayList<ResolveInfo> filterForward(Collection<ResolveInfo> resolveInfo) {
        ArrayList<ResolveInfo> list = new ArrayList<>();
        for (ResolveInfo info : resolveInfo) {
            if (!isForward(info)) {
                list.add(info);

            }
        }
        return list;
    }
}
