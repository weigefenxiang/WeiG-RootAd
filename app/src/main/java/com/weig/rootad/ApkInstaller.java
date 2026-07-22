package com.weig.rootad;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

final class ApkInstaller {
    interface Callback { void completed(String message, boolean success); }
    private static final String ACTION = "com.weig.rootad.INSTALL_RESULT";

    private ApkInstaller() {}

    static void install(Activity activity, File apk, Callback callback) throws Exception {
        verifySigner(activity, apk);
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            callback.completed("Allow updates from Wei.G RootAd, then tap update again.", false);
            return;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirmation;
                    if (Build.VERSION.SDK_INT >= 33) {
                        confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
                    } else {
                        //noinspection deprecation
                        confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    }
                    if (confirmation != null) activity.startActivity(confirmation);
                    return;
                }
                try { activity.unregisterReceiver(this); } catch (Exception ignored) {}
                callback.completed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
                        status == PackageInstaller.STATUS_SUCCESS);
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            activity.registerReceiver(receiver, new IntentFilter(ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            // The PendingIntent is explicit to this package; Android 12 has no receiver export flag overload.
            activity.registerReceiver(receiver, new IntentFilter(ACTION));
        }

        PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(activity.getPackageName());
        params.setSize(apk.length());
        int sessionId = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(sessionId);
             FileInputStream input = new FileInputStream(apk);
             OutputStream output = session.openWrite("WeiGRootAd.apk", 0, apk.length())) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            session.fsync(output);
            Intent result = new Intent(ACTION).setPackage(activity.getPackageName());
            PendingIntent pending = PendingIntent.getBroadcast(activity, sessionId, result,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            session.commit(pending.getIntentSender());
        }
    }

    private static void verifySigner(Activity activity, File apk) throws Exception {
        PackageManager manager = activity.getPackageManager();
        PackageInfo candidate;
        PackageInfo installed;
        if (Build.VERSION.SDK_INT >= 33) {
            candidate = manager.getPackageArchiveInfo(apk.getAbsolutePath(),
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES));
            installed = manager.getPackageInfo(activity.getPackageName(),
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES));
        } else {
            //noinspection deprecation
            candidate = manager.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
            //noinspection deprecation
            installed = manager.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        }
        if (candidate == null || candidate.signingInfo == null || installed.signingInfo == null)
            throw new SecurityException("Cannot verify APK signing certificate");
        Signature[] candidateSigners = candidate.signingInfo.getApkContentsSigners();
        Signature[] installedSigners = installed.signingInfo.getApkContentsSigners();
        if (candidateSigners.length != 1 || installedSigners.length != 1 ||
                !candidateSigners[0].equals(installedSigners[0]))
            throw new SecurityException("Update APK is signed by a different key");
    }
}
