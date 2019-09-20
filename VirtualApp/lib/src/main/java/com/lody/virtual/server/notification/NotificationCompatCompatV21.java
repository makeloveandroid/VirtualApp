package com.lody.virtual.server.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.NotificationChannelCompat;
import com.lody.virtual.helper.utils.Reflect;

import mirror.android.app.NotificationO;

/**
 * @author 247321543
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
/* package */ class NotificationCompatCompatV21 extends NotificationCompatCompatV14 {

    private static final String TAG = NotificationCompatCompatV21.class.getSimpleName();

    NotificationCompatCompatV21() {
        super();
    }

    @Override
    public boolean dealNotification(int id, Notification notification, String packageName,int userId) {
        Context appContext = getAppContext(packageName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.O) {
                if (TextUtils.isEmpty(notification.getChannelId())) {
                    if (NotificationO.mChannelId != null) {
                        //安通＋, 短信呼吸灯
                        if(packageName.equals("com.xdja.actoma") || packageName.equals("com.xdja.mms")) {
                            NotificationO.mChannelId.set(notification, NotificationChannelCompat.LIGHT_ID);
                        }else {
                            NotificationO.mChannelId.set(notification, NotificationChannelCompat.DEFAULT_ID);
                        }
                    }
                } else {
                    String channel = VNotificationManager.get().dealNotificationChannel(notification.getChannelId(), packageName, userId);
                    if (NotificationO.mChannelId != null) {
                        NotificationO.mChannelId.set(notification, channel);
                    }
                }
                if (notification.getGroup() != null) {
                    String group = VNotificationManager.get().dealNotificationGroup(notification.getGroup(), packageName, userId);
                    if (NotificationO.mGroupKey != null) {
                        NotificationO.mGroupKey.set(notification, group);
                    }
                }
            }
        }
        return resolveRemoteViews(appContext, id, packageName, notification)
                || resolveRemoteViews(appContext, id, packageName, notification.publicVersion);
    }

    private PackageInfo getOutSidePackageInfo(String packageName){
        try {
            return  VirtualCore.get().getUnHookPackageManager().getPackageInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES);
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean resolveRemoteViews(Context appContext,int id, String packageName, Notification notification) {
        if (notification == null) {
            return false;
        }
        ApplicationInfo host = getHostContext().getApplicationInfo();
        PackageInfo outside = getOutSidePackageInfo(packageName);
        PackageInfo inside = VPackageManager.get().getPackageInfo(packageName,
                PackageManager.GET_SHARED_LIBRARY_FILES, 0);

        //check outside and inside's version
        boolean isInstalled = outside != null && outside.versionCode == inside.versionCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getNotificationFixer().fixIcon(notification.getSmallIcon(), appContext, isInstalled);
            getNotificationFixer().fixIcon(notification.getLargeIcon(), appContext, isInstalled);
        } else {
            getNotificationFixer().fixIconImage(appContext.getResources(), notification.contentView, false, notification);
        }
        notification.icon = host.icon;
        //Fix RemoteViews
        getNotificationFixer().fixNotificationRemoteViews(appContext, notification);

        //fix apk path
        ApplicationInfo proxyApplicationInfo;
        if(isInstalled){
            proxyApplicationInfo = outside.applicationInfo;
        }else{
            proxyApplicationInfo = inside.applicationInfo;
        }
        proxyApplicationInfo.targetSdkVersion = 22;
        fixApplicationInfo(notification.tickerView, proxyApplicationInfo);
        fixApplicationInfo(notification.contentView, proxyApplicationInfo);
        fixApplicationInfo(notification.bigContentView, proxyApplicationInfo);
        fixApplicationInfo(notification.headsUpContentView, proxyApplicationInfo);
        Bundle bundle = Reflect.on(notification).get("extras");
        if (bundle != null) {
            bundle.putParcelable(EXTRA_BUILDER_APPLICATION_INFO, proxyApplicationInfo);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInstalled) {
            remakeRemoteViews(id, notification, appContext);
        }
        return true;
    }

    private ApplicationInfo getApplicationInfo(Notification notification) {
        ApplicationInfo ai = getApplicationInfo(notification.tickerView);
        if (ai != null) {
            return ai;
        }
        ai = getApplicationInfo(notification.contentView);
        if (ai != null) {
            return ai;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ai = getApplicationInfo(notification.bigContentView);
            if (ai != null) {
                return ai;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ai = getApplicationInfo(notification.headsUpContentView);
            if (ai != null) {
                return ai;
            }
        }
        return null;
    }

    private ApplicationInfo getApplicationInfo(RemoteViews remoteViews) {
        if (remoteViews != null) {
            return mirror.android.widget.RemoteViews.mApplication.get(remoteViews);
        }
        return null;
    }

    private void fixApplicationInfo(RemoteViews remoteViews, ApplicationInfo ai) {
        if (remoteViews != null) {
            mirror.android.widget.RemoteViews.mApplication.set(remoteViews, ai);
        }
    }
}
