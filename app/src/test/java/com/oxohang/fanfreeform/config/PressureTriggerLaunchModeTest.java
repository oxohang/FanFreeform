package com.oxohang.fanfreeform.config;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PressureTriggerLaunchModeTest {
    @Test
    public void jsonRoundTripKeepsPerTriggerLaunchMode() throws Exception {
        PressureTrigger trigger = new PressureTrigger(3, true, 20, 80, 12,
                ConfigContract.PRESSURE_ACTION_SINGLE_TARGET, null,
                true, true);
        JSONObject json = trigger.toJson();
        PressureTrigger restored = PressureTrigger.fromJson(json);
        assertEquals(3, restored.id);
        assertTrue(restored.enabled);
        assertTrue(restored.openAsFreeform);
        assertTrue(restored.heavyLaunchEnabled);
    }

    @Test
    public void legacyJsonWithoutLaunchModeUsesContractDefaults() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", 1);
        json.put("enabled", true);
        json.put("centerX", 50);
        json.put("centerY", 50);
        json.put("radius", 12);
        json.put("action", ConfigContract.DEFAULT_PRESSURE_ACTION);
        PressureTrigger restored = PressureTrigger.fromJson(json);
        assertEquals(ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM,
                restored.openAsFreeform);
        assertEquals(ConfigContract.DEFAULT_PRESSURE_HEAVY_LAUNCH_ENABLED,
                restored.heavyLaunchEnabled);
    }

    @Test
    public void withLaunchModeDoesNotTouchOtherFields() {
        PressureTrigger original = PressureTrigger.defaultTrigger();
        PressureTrigger updated = original.withLaunchMode(true, true);
        assertEquals(original.id, updated.id);
        assertEquals(original.centerXPercent, updated.centerXPercent);
        assertEquals(original.centerYPercent, updated.centerYPercent);
        assertEquals(original.radiusPercent, updated.radiusPercent);
        assertEquals(original.action, updated.action);
        assertTrue(updated.openAsFreeform);
        assertTrue(updated.heavyLaunchEnabled);
    }

    @Test
    public void contractDefaultsAreStable() {
        assertEquals(ConfigContract.ACTION_EDGE_PIN,
                ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION);
        assertEquals(59, ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS);
        assertFalse(ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH);
        assertFalse(ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH);
    }
}
