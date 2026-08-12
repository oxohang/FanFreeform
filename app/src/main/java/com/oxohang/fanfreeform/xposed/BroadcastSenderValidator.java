package com.oxohang.fanfreeform.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;

/** Validates the real package that delivered a dynamically registered broadcast. */
final class BroadcastSenderValidator {
    private BroadcastSenderValidator() { }

    static boolean isFromPackage(Context context, BroadcastReceiver receiver,
                                 String expectedPackage) {
        if (context == null || receiver == null || expectedPackage == null
                || expectedPackage.isEmpty()) return false;
        try {
            // API < 34 cannot prove who sent the broadcast. Accepting every sender lets
            // arbitrary third-party apps drive privileged launch bridges, so fail closed.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false;
            int senderUid;
            String sentFromPackage = receiver.getSentFromPackage();
            if (expectedPackage.equals(sentFromPackage)) return true;
            senderUid = receiver.getSentFromUid();
            if (senderUid < 0) return false;
            return containsPackage(expectedPackage,
                    context.getPackageManager().getPackagesForUid(senderUid));
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean containsPackage(String expectedPackage, String[] packages) {
        if (expectedPackage == null || packages == null) return false;
        for (String packageName : packages) {
            if (expectedPackage.equals(packageName)) return true;
        }
        return false;
    }
}
