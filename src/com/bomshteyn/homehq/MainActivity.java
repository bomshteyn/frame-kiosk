package com.bomshteyn.homehq;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bomshteyn.homehq.MainActivity;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    static final String DEFAULT_URL = "https://frame-dashboard.nathan-005.workers.dev/";
    static final long EXIT_HOLD_MS = 1500;
    static final float EXIT_SLOP_PX = 60.0f;
    static final String PREFS = "kiosk";
    static final String PREF_URL = "url";
    static final long RETRY_MS = 5000;
    static final String TAG = "HomeHQ";
    private boolean exitArmed;
    private LinearLayout offline;
    private TextView offlineText;
    private TextView offlineTitle;
    private boolean pageFailed;
    private String retryUrl;
    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] exitIds = new int[2];
    private final float[] exitX = new float[2];
    private final float[] exitY = new float[2];
    private final Runnable retry = new Runnable() { // from class: com.bomshteyn.homehq.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.retryUrl != null) {
                MainActivity.this.web.loadUrl(MainActivity.this.retryUrl);
            }
        }
    };
    private final Runnable showControls = new AnonymousClass2();

    /* renamed from: com.bomshteyn.homehq.MainActivity$2, reason: invalid class name */
    class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.exitArmed = false;
            Log.i(MainActivity.TAG, "Two-finger hold: opening controls");
            new AlertDialog.Builder(MainActivity.this).setTitle("Home HQ").setItems(new String[]{"Open launcher", "Android settings", "Wi-Fi settings", "Reload dashboard", "About this app"}, new DialogInterface.OnClickListener() { // from class: com.bomshteyn.homehq.MainActivity$2$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.AnonymousClass2.this.m3lambda$run$0$combomshteynhomehqMainActivity$2(dialogInterface, i);
                }
            }).setNegativeButton("Back to dashboard", (DialogInterface.OnClickListener) null).show();
        }

        /* renamed from: lambda$run$0$com-bomshteyn-homehq-MainActivity$2, reason: not valid java name */
        /* synthetic */ void m3lambda$run$0$combomshteynhomehqMainActivity$2(DialogInterface dialogInterface, int i) {
            if (i == 0) {
                MainActivity.this.openLauncher();
                return;
            }
            if (i != 1) {
                if (i == 2) {
                    MainActivity.this.openWifiSettings();
                    return;
                } else if (i == 3) {
                    MainActivity.this.web.reload();
                    return;
                } else {
                    MainActivity.this.showAbout();
                    return;
                }
            }
            MainActivity.this.startActivity(new Intent("android.settings.SETTINGS"));
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        applyDebugFlag(getIntent());
        applyHomeFlag(getIntent());
        keepNavigationEnabled();
        getWindow().addFlags(1152);
        CookieManager.getInstance().setAcceptCookie(true);
        WebView webView = new WebView(this);
        this.web = webView;
        webView.addJavascriptInterface(new Bridge(), TAG);
        WebSettings settings = this.web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        this.web.setWebViewClient(new WebViewClient() { // from class: com.bomshteyn.homehq.MainActivity.3
            @Override // android.webkit.WebViewClient
            public void onPageStarted(WebView webView2, String str, Bitmap bitmap) {
                MainActivity.this.pageFailed = false;
            }

            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView webView2, String str) {
                if (!MainActivity.this.pageFailed) {
                    MainActivity.this.offline.setVisibility(8);
                }
            }

            @Override // android.webkit.WebViewClient
            public void onReceivedError(WebView webView2, int i, String str, String str2) {
                Log.w(MainActivity.TAG, "Load failed (" + str + "), retrying in " + MainActivity.RETRY_MS + " ms");
                MainActivity.this.pageFailed = true;
                MainActivity.this.showOffline();
                MainActivity.this.retryUrl = str2;
                MainActivity.this.handler.removeCallbacks(MainActivity.this.retry);
                MainActivity.this.handler.postDelayed(MainActivity.this.retry, MainActivity.RETRY_MS);
            }
        });
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(this.web);
        frameLayout.addView(buildOffline());
        setContentView(frameLayout);
        String urlFrom = urlFrom(getIntent());
        WebView webView2 = this.web;
        if (urlFrom == null) {
            urlFrom = savedUrl();
        }
        webView2.loadUrl(urlFrom);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        NavigationService.setDashboardVisible(true);
    }

    @Override // android.app.Activity
    protected void onPause() {
        cancelExit();
        NavigationService.setDashboardVisible(false);
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onStop() {
        CookieManager.getInstance().flush();
        super.onStop();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyDebugFlag(intent);
        applyHomeFlag(intent);
        String urlFrom = urlFrom(intent);
        if (urlFrom != null) {
            this.handler.removeCallbacks(this.retry);
            this.web.loadUrl(urlFrom);
        }
    }

    private String urlFrom(Intent intent) {
        String stringExtra = intent.getStringExtra(PREF_URL);
        if (stringExtra == null) {
            return intent.getDataString();
        }
        String scheme = Uri.parse(stringExtra).getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            getSharedPreferences(PREFS, 0).edit().putString(PREF_URL, stringExtra).apply();
            Log.i(TAG, "Saved kiosk URL: " + stringExtra);
            return stringExtra;
        }
        Log.w(TAG, "Ignoring non-web kiosk URL: " + stringExtra);
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String savedUrl() {
        return getSharedPreferences(PREFS, 0).getString(PREF_URL, DEFAULT_URL);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceOwner() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService("device_policy");
        return devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(getPackageName());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAbout() {
        String str = "";
        int i = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            str = packageInfo.versionName;
            i = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read app version", e);
        }
        boolean isDeviceOwner = isDeviceOwner();
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(this).setTitle("About this app").setMessage("Version " + str + " (" + i + ")\nAutomatic updates: " + (isDeviceOwner ? "on" : "off — run the setup command")).setPositiveButton("Close", (DialogInterface.OnClickListener) null);
        if (isDeviceOwner) {
            positiveButton.setNeutralButton("Turn off automatic updates", new DialogInterface.OnClickListener() { // from class: com.bomshteyn.homehq.MainActivity$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    MainActivity.this.m2lambda$showAbout$0$combomshteynhomehqMainActivity(dialogInterface, i2);
                }
            });
        }
        positiveButton.show();
    }

    /* renamed from: lambda$showAbout$0$com-bomshteyn-homehq-MainActivity, reason: not valid java name */
    /* synthetic */ void m2lambda$showAbout$0$combomshteynhomehqMainActivity(DialogInterface dialogInterface, int i) {
        confirmDisableAutomaticUpdates();
    }

    private void confirmDisableAutomaticUpdates() {
        new AlertDialog.Builder(this).setTitle("Turn off automatic updates?").setMessage("Home HQ will no longer install app updates automatically.").setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).setPositiveButton("Turn off", new DialogInterface.OnClickListener() { // from class: com.bomshteyn.homehq.MainActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m1xabc02e80(dialogInterface, i);
            }
        }).show();
    }

    /* renamed from: lambda$confirmDisableAutomaticUpdates$1$com-bomshteyn-homehq-MainActivity, reason: not valid java name */
    /* synthetic */ void m1xabc02e80(DialogInterface dialogInterface, int i) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService("device_policy");
        if (devicePolicyManager == null || !devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            showAbout();
            return;
        }
        try {
            devicePolicyManager.clearDeviceOwnerApp(getPackageName());
            Log.i(TAG, "Automatic updates turned off");
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not turn off automatic updates", e);
        }
        showAbout();
    }

    private int dp(float f) {
        return (int) TypedValue.applyDimension(1, f, getResources().getDisplayMetrics());
    }

    private View buildOffline() {
        LinearLayout linearLayout = new LinearLayout(this);
        this.offline = linearLayout;
        linearLayout.setOrientation(1);
        this.offline.setGravity(17);
        this.offline.setBackgroundColor(Color.rgb(17, 17, 17));
        this.offline.setVisibility(8);
        this.offline.setClickable(true);
        TextView textView = new TextView(this);
        this.offlineTitle = textView;
        textView.setTextColor(-1);
        this.offlineTitle.setTextSize(2, 40.0f);
        this.offlineTitle.setGravity(17);
        this.offline.addView(this.offlineTitle);
        TextView textView2 = new TextView(this);
        this.offlineText = textView2;
        textView2.setTextColor(Color.rgb(170, 170, 170));
        this.offlineText.setTextSize(2, 20.0f);
        this.offlineText.setGravity(17);
        this.offlineText.setPadding(dp(24.0f), dp(12.0f), dp(24.0f), dp(28.0f));
        this.offline.addView(this.offlineText);
        Button button = new Button(this);
        button.setText("Open Wi-Fi settings");
        button.setTextSize(2, 22.0f);
        button.setPadding(dp(32.0f), dp(16.0f), dp(32.0f), dp(16.0f));
        button.setOnClickListener(new View.OnClickListener() { // from class: com.bomshteyn.homehq.MainActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m0lambda$buildOffline$2$combomshteynhomehqMainActivity(view);
            }
        });
        this.offline.addView(button, new LinearLayout.LayoutParams(-2, -2));
        return this.offline;
    }

    /* renamed from: lambda$buildOffline$2$com-bomshteyn-homehq-MainActivity, reason: not valid java name */
    /* synthetic */ void m0lambda$buildOffline$2$combomshteynhomehqMainActivity(View view) {
        openWifiSettings();
    }

    private boolean wifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        NetworkInfo networkInfo = connectivityManager != null ? connectivityManager.getNetworkInfo(1) : null;
        return networkInfo != null && networkInfo.isConnected();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showOffline() {
        if (wifiConnected()) {
            this.offlineTitle.setText("Can't reach the dashboard");
            this.offlineText.setText("Wi-Fi is connected. Retrying every few seconds.");
        } else {
            this.offlineTitle.setText("Connect to Wi-Fi?");
            this.offlineText.setText("The frame isn't on Wi-Fi. The dashboard comes back by itself once it is.");
        }
        this.offline.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openWifiSettings() {
        try {
            startActivity(new Intent("android.settings.WIFI_SETTINGS"));
        } catch (Exception e) {
            Log.w(TAG, "No Wi-Fi settings screen, opening Settings", e);
            startActivity(new Intent("android.settings.SETTINGS"));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Bridge {
        private Bridge() {
        }

        @JavascriptInterface
        public void setScreenOff(final boolean z) {
            MainActivity.this.handler.post(new Runnable() { // from class: com.bomshteyn.homehq.MainActivity$Bridge$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.Bridge.this.m5lambda$setScreenOff$0$combomshteynhomehqMainActivity$Bridge(z);
                }
            });
        }

        /* renamed from: lambda$setScreenOff$0$com-bomshteyn-homehq-MainActivity$Bridge, reason: not valid java name */
        /* synthetic */ void m5lambda$setScreenOff$0$combomshteynhomehqMainActivity$Bridge(boolean z) {
            WindowManager.LayoutParams attributes = MainActivity.this.getWindow().getAttributes();
            attributes.screenBrightness = z ? 0.0f : -1.0f;
            MainActivity.this.getWindow().setAttributes(attributes);
        }

        @JavascriptInterface
        public void checkAppUpdate() {
            Updater.check(MainActivity.this.getApplicationContext(), MainActivity.this.savedUrl());
        }

        @JavascriptInterface
        public String appInfo() {
            try {
                PackageInfo packageInfo = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0);
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("versionCode", packageInfo.versionCode);
                jSONObject.put("versionName", packageInfo.versionName);
                jSONObject.put("deviceOwner", MainActivity.this.isDeviceOwner());
                return jSONObject.toString();
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "Could not read app information", e);
                return "{\"versionCode\":0,\"versionName\":\"\",\"deviceOwner\":false}";
            }
        }

        @JavascriptInterface
        public void setVolume(int i) {
            AudioManager audioManager = (AudioManager) MainActivity.this.getSystemService("audio");
            if (audioManager == null) {
                return;
            }
            audioManager.setStreamVolume(3, Math.round((Math.max(0, Math.min(100, i)) * audioManager.getStreamMaxVolume(3)) / 100.0f), 0);
        }

        @JavascriptInterface
        public int getVolume() {
            int streamMaxVolume;
            AudioManager audioManager = (AudioManager) MainActivity.this.getSystemService("audio");
            if (audioManager == null || (streamMaxVolume = audioManager.getStreamMaxVolume(3)) == 0) {
                return 0;
            }
            return Math.round((audioManager.getStreamVolume(3) * 100.0f) / streamMaxVolume);
        }

        /* renamed from: lambda$restartApp$1$com-bomshteyn-homehq-MainActivity$Bridge, reason: not valid java name */
        /* synthetic */ void m4lambda$restartApp$1$combomshteynhomehqMainActivity$Bridge() {
            MainActivity.this.recreate();
        }

        @JavascriptInterface
        public void restartApp() {
            MainActivity.this.handler.post(new Runnable() { // from class: com.bomshteyn.homehq.MainActivity$Bridge$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.Bridge.this.m4lambda$restartApp$1$combomshteynhomehqMainActivity$Bridge();
                }
            });
        }
    }

    private void keepNavigationEnabled() {
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            return;
        }
        String str = getPackageName() + "/" + NavigationService.class.getName();
        String string = Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services");
        if (string != null) {
            ComponentName componentName = new ComponentName(this, (Class<?>) NavigationService.class);
            for (String str2 : string.split(":")) {
                if (componentName.equals(ComponentName.unflattenFromString(str2))) {
                    return;
                }
            }
        }
        if (string != null && !string.isEmpty()) {
            str = string + ":" + str;
        }
        try {
            Settings.Secure.putString(getContentResolver(), "enabled_accessibility_services", str);
            Settings.Secure.putString(getContentResolver(), "accessibility_enabled", "1");
            Log.i(TAG, "Re-enabled navigation service");
        } catch (SecurityException e) {
            Log.w(TAG, "Could not re-enable navigation service", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openLauncher() {
        Intent addCategory = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
        for (ResolveInfo resolveInfo : getPackageManager().queryIntentActivities(addCategory, 0)) {
            String str = resolveInfo.activityInfo.packageName;
            if (!str.equals(getPackageName()) && !str.equals("com.android.settings")) {
                startActivity(new Intent(addCategory).setComponent(new ComponentName(str, resolveInfo.activityInfo.name)));
                return;
            }
        }
        startActivity(new Intent("android.settings.SETTINGS"));
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
        WebView webView = this.web;
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private void applyDebugFlag(Intent intent) {
        boolean z = false;
        if (intent != null && intent.getBooleanExtra("debug", false)) {
            z = true;
        }
        WebView.setWebContentsDebuggingEnabled(z);
        Log.i(TAG, "WebView remote debugging " + (z ? "ON" : "off"));
    }

    private void applyHomeFlag(Intent intent) {
        if (intent == null || !intent.hasExtra("home")) {
            return;
        }
        boolean booleanExtra = intent.getBooleanExtra("home", false);
        getPackageManager().setComponentEnabledSetting(new ComponentName(this, MainActivity.class.getPackage().getName() + ".Home"), booleanExtra ? 1 : 0, 1);
        Log.i(TAG, "Home alias " + (booleanExtra ? "on" : "off"));
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        int i = 0;
        switch (motionEvent.getActionMasked()) {
            case 1:
            case 3:
            case 6:
                cancelExit();
                break;
            case 2:
                if (this.exitArmed) {
                    while (i < 2) {
                        int index = motionEvent.findPointerIndex(this.exitIds[i]);
                        if (index >= 0 && Math.hypot(motionEvent.getX(index) - this.exitX[i], motionEvent.getY(index) - this.exitY[i]) <= 60.0d) {
                            i++;
                        } else {
                            cancelExit();
                            break;
                        }
                    }
                    break;
                }
                break;
            case 5:
                if (motionEvent.getPointerCount() == 2) {
                    while (i < 2) {
                        this.exitIds[i] = motionEvent.getPointerId(i);
                        this.exitX[i] = motionEvent.getX(i);
                        this.exitY[i] = motionEvent.getY(i);
                        i++;
                    }
                    this.exitArmed = true;
                    this.handler.removeCallbacks(this.showControls);
                    this.handler.postDelayed(this.showControls, EXIT_HOLD_MS);
                    break;
                } else {
                    cancelExit();
                    break;
                }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    private void cancelExit() {
        if (this.exitArmed) {
            this.exitArmed = false;
            this.handler.removeCallbacks(this.showControls);
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            hideSystemUi();
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.web.canGoBack()) {
            this.web.goBack();
        }
    }
}
