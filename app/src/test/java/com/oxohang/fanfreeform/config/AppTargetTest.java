package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class AppTargetTest {
    @Test public void launcherShortcutJsonRoundTripsWithoutLosingLaunchIdentity() {
        AppTarget original = AppTarget.launcherShortcut(
                "com.android.chrome", "launcher:42", "网页",
                "#Intent;component=com.android.chrome/.Main;end", 0);
        AppTarget restored = AppTarget.fromJson(original.toJson());

        assertNotNull(restored);
        assertTrue(restored.isShortcut());
        assertTrue(restored.isLauncherShortcut());
        assertEquals(original, restored);
        assertEquals(original.shortcutIntentUri, restored.shortcutIntentUri);
    }
}
