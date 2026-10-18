package com.wesbos.homehq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class InstallResultReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Log.i("HomeHQ", "Install result: status=" + intent.getIntExtra("android.content.pm.extra.STATUS", 1) + ", message=" + intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE"));
        Updater.clearInstallInProgress();
    }
}
