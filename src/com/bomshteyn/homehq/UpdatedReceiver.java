package com.bomshteyn.homehq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class UpdatedReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Log.i("HomeHQ", "App updated; starting dashboard");
        Intent intent2 = new Intent(context, (Class<?>) MainActivity.class);
        intent2.addFlags(268435456);
        context.startActivity(intent2);
    }
}
