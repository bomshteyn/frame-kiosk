package com.wesbos.homehq;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.JSONObject;

/* loaded from: classes.dex */
public final class Updater {
    private static final long CHECK_INTERVAL_MS = 300000;
    private static final String TAG = "HomeHQ";
    private static final int TIMEOUT_MS = 15000;
    private static boolean installInProgress;
    private static long lastCheckAt;

    private Updater() {
    }

    static void check(Context context, final String str) {
        synchronized (Updater.class) {
            long currentTimeMillis = System.currentTimeMillis();
            if (!installInProgress && currentTimeMillis - lastCheckAt >= CHECK_INTERVAL_MS) {
                lastCheckAt = currentTimeMillis;
                final Context applicationContext = context.getApplicationContext();
                Log.i(TAG, "Starting app update check");
                new Thread(new Runnable() { // from class: com.wesbos.homehq.Updater$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        Updater.checkInBackground(applicationContext, str);
                    }
                }, "HomeHQUpdater").start();
            }
        }
    }

    static void clearInstallInProgress() {
        synchronized (Updater.class) {
            installInProgress = false;
        }
        Log.i(TAG, "App install finished");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void checkInBackground(Context context, String str) {
        File file = null;
        try {
            URL url = new URL(str);
            JSONObject json = getJson(new URL(url, "/kiosk/version.json"));
            int i = json.getInt("versionCode");
            String string = json.getString("versionName");
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Log.i(TAG, "Available app version " + string + " (" + i + ")");
            if (i <= packageInfo.versionCode) {
                Log.i(TAG, "App is up to date");
                return;
            }
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
            if (devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(context.getPackageName())) {
                String string2 = json.getString("sha256");
                if (!string2.matches("(?i)[0-9a-f]{64}")) {
                    Log.w(TAG, "Invalid APK checksum in update manifest");
                    return;
                }
                URL url2 = new URL(url, json.getString("apk"));
                if (!sameOrigin(url, url2)) {
                    Log.w(TAG, "Skipping APK URL outside the dashboard origin");
                    return;
                }
                File file2 = new File(context.getCacheDir(), "update.apk");
                try {
                    download(url2, file2, url);
                    if (!string2.equalsIgnoreCase(sha256(file2))) {
                        Log.e(TAG, "Downloaded APK checksum does not match");
                        file2.delete();
                        return;
                    } else {
                        Log.i(TAG, "APK checksum verified; starting install");
                        install(context, file2);
                        file2.delete();
                        return;
                    }
                } catch (Exception e) {
                    file = file2;
                    Log.e(TAG, "App update failed", e);
                    if (file == null || !file.exists()) {
                        return;
                    }
                    file.delete();
                    return;
                }
            }
            Log.i(TAG, "Skipping app update because this app is not device owner");
        } catch (Exception e2) {
            Log.e(TAG, "App update failed", e2);
        }
    }

    private static JSONObject getJson(URL url) throws Exception {
        HttpURLConnection open = open(url);
        try {
            if (open.getResponseCode() != 200) {
                throw new IllegalStateException("Update manifest request failed: HTTP " + open.getResponseCode());
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = open.getInputStream();
            try {
                copy(inputStream, byteArrayOutputStream);
                if (inputStream != null) {
                    inputStream.close();
                }
                return new JSONObject(new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8));
            } finally {
            }
        } finally {
            open.disconnect();
        }
    }

    private static void download(URL url, File file, URL url2) throws Exception {
        HttpURLConnection open = open(url);
        try {
            if (open.getResponseCode() != 200) {
                throw new IllegalStateException("APK request failed: HTTP " + open.getResponseCode());
            }
            if (!sameOrigin(url2, open.getURL())) {
                throw new IllegalStateException("APK download redirected outside the dashboard origin");
            }
            InputStream inputStream = open.getInputStream();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    copy(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    Log.i(TAG, "Downloaded APK: " + file.length() + " bytes");
                } finally {
                }
            } finally {
            }
        } finally {
            open.disconnect();
        }
    }

    private static HttpURLConnection open(URL url) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setConnectTimeout(TIMEOUT_MS);
        httpURLConnection.setReadTimeout(TIMEOUT_MS);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setInstanceFollowRedirects(true);
        httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
        return httpURLConnection;
    }

    private static void install(Context context, File file) throws Exception {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(1);
        sessionParams.setAppPackageName(context.getPackageName());
        int createSession = packageInstaller.createSession(sessionParams);
        PackageInstaller.Session session = null;
        try {
            try {
                PackageInstaller.Session openSession = packageInstaller.openSession(createSession);
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    OutputStream openWrite = openSession.openWrite("homehq.apk", 0L, file.length());
                    try {
                        copy(fileInputStream, openWrite);
                        openSession.fsync(openWrite);
                        if (openWrite != null) {
                            openWrite.close();
                        }
                        fileInputStream.close();
                        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, (Class<?>) InstallResultReceiver.class), 134217728);
                        synchronized (Updater.class) {
                            installInProgress = true;
                        }
                        openSession.commit(broadcast.getIntentSender());
                        Log.i(TAG, "PackageInstaller session committed");
                        if (openSession != null) {
                            openSession.close();
                        }
                    } finally {
                    }
                } catch (Throwable th) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (0 != 0) {
                    session.close();
                }
                throw th3;
            }
        } catch (Exception e) {
            synchronized (Updater.class) {
                installInProgress = false;
                packageInstaller.abandonSession(createSession);
                throw e;
            }
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[8192];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read == -1) {
                    break;
                }
                messageDigest.update(bArr, 0, read);
            }
            fileInputStream.close();
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest.digest()) {
                sb.append(String.format("%02x", Integer.valueOf(b & 255)));
            }
            return sb.toString();
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static boolean sameOrigin(URL url, URL url2) {
        return url.getProtocol().equalsIgnoreCase(url2.getProtocol()) && url.getHost().equalsIgnoreCase(url2.getHost()) && effectivePort(url) == effectivePort(url2);
    }

    private static int effectivePort(URL url) {
        return url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] bArr = new byte[8192];
        while (true) {
            int read = inputStream.read(bArr);
            if (read == -1) {
                outputStream.flush();
                return;
            }
            outputStream.write(bArr, 0, read);
        }
    }
}
