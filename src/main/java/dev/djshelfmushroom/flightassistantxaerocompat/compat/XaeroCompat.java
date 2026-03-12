package dev.djshelfmushroom.flightassistantxaerocompat.compat;

import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * All Xaero's World Map API access is isolated here so that only this file
 * needs to change when Xaero's World Map updates.
 *
 * <p>The flight plan overlay is rendered via a Forge {@code ScreenEvent.Render.Post}
 * listener (see {@link dev.djshelfmushroom.flightassistantxaerocompat.events.WaypointContextMenuHandler})
 * rather than through Xaero's overlay registration API, which is more robust
 * across Xaero version changes.</p>
 *
 * <p>The right-click context menu options are injected via a Mixin on
 * {@code xaero.map.gui.GuiMap} (see MixinGuiMap).</p>
 */
public class XaeroCompat {

    private static final Logger LOGGER = LogManager.getLogger(FlightAssistantXaeroCompat.MOD_ID);

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * Called during {@code FMLClientSetupEvent}. Logs that the Xaero compat
     * layer is active. Context menu injection happens via Mixin; overlay
     * rendering happens via Forge's ScreenEvent.
     */
    public static void init() {
        LOGGER.info("[XaeroCompat] Xaero compat initialised (overlay via ScreenEvent, menu via Mixin).");
    }

    // =========================================================================
    // Waypoint helpers — used by MixinGuiMap context menu actions
    // =========================================================================

    /**
     * Returns the world X coordinate of a Xaero {@code Waypoint} object.
     * <p>Xaero 1.39.2: {@code Waypoint#getX()} — public method.</p>
     */
    public static Integer getWaypointX(Object waypoint) {
        if (waypoint == null) return null;
        try {
            // Xaero 1.39.2: public int getX() on xaero.common.minimap.waypoints.Waypoint
            Method m = waypoint.getClass().getMethod("getX");
            return ((Number) m.invoke(waypoint)).intValue();
        } catch (Exception e) {
            // Fallback: try direct field access (older Xaero builds)
            try {
                java.lang.reflect.Field f = waypoint.getClass().getDeclaredField("x");
                f.setAccessible(true);
                return ((Number) f.get(waypoint)).intValue();
            } catch (Exception ex) {
                LOGGER.warn("[XaeroCompat] getWaypointX failed: {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Returns the world Z coordinate of a Xaero {@code Waypoint} object.
     * <p>Xaero 1.39.2: {@code Waypoint#getZ()} — public method.</p>
     */
    public static Integer getWaypointZ(Object waypoint) {
        if (waypoint == null) return null;
        try {
            // Xaero 1.39.2: public int getZ() on xaero.common.minimap.waypoints.Waypoint
            Method m = waypoint.getClass().getMethod("getZ");
            return ((Number) m.invoke(waypoint)).intValue();
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = waypoint.getClass().getDeclaredField("z");
                f.setAccessible(true);
                return ((Number) f.get(waypoint)).intValue();
            } catch (Exception ex) {
                LOGGER.warn("[XaeroCompat] getWaypointZ failed: {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Returns the world Y coordinate of a Xaero {@code Waypoint} object.
     * <p>Xaero 1.39.2: {@code Waypoint#getY()} — public method.</p>
     */
    public static Integer getWaypointY(Object waypoint) {
        if (waypoint == null) return null;
        try {
            // Xaero 1.39.2: public int getY() on xaero.common.minimap.waypoints.Waypoint
            Method m = waypoint.getClass().getMethod("getY");
            return ((Number) m.invoke(waypoint)).intValue();
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = waypoint.getClass().getDeclaredField("y");
                f.setAccessible(true);
                return ((Number) f.get(waypoint)).intValue();
            } catch (Exception ex) {
                LOGGER.warn("[XaeroCompat] getWaypointY failed: {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Returns the display name of a Xaero {@code Waypoint} object.
     * <p>Xaero 1.39.2: {@code Waypoint#getName()} — public method.</p>
     */
    public static String getWaypointName(Object waypoint) {
        if (waypoint == null) return null;
        try {
            // Xaero 1.39.2: public String getName() on xaero.common.minimap.waypoints.Waypoint
            Method m = waypoint.getClass().getMethod("getName");
            Object result = m.invoke(waypoint);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            LOGGER.warn("[XaeroCompat] getWaypointName failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns the dimension resource-location string of a Xaero waypoint, or
     * {@code null} if unavailable.
     * <p>Xaero 1.39.2: the dimension may be stored in {@code WaypointSet} or
     * obtained from the waypoint's context. Falls back to field access.</p>
     */
    public static String getWaypointDimension(Object waypoint) {
        if (waypoint == null) return null;
        // Try getDimension() first (some Xaero versions expose this)
        try {
            Method m = waypoint.getClass().getMethod("getDimension");
            Object result = m.invoke(waypoint);
            return result != null ? result.toString() : null;
        } catch (Exception ignored) {
            // Not available in this Xaero version
        }
        // Fallback: try field "dim"
        try {
            java.lang.reflect.Field f = waypoint.getClass().getDeclaredField("dim");
            f.setAccessible(true);
            Object result = f.get(waypoint);
            return result != null ? result.toString() : null;
        } catch (Exception ex) {
            LOGGER.warn("[XaeroCompat] getWaypointDimension failed: {}", ex.getMessage());
            return null;
        }
    }

    // =========================================================================
    // GuiMap coordinate conversion helpers (used by FlightPlanMapOverlay)
    // =========================================================================

    /**
     * Reads the camera X (world) from a {@code GuiMap} instance via reflection.
     * <p>Xaero 1.39.2: private {@code double cameraX} field on GuiMap.</p>
     */
    public static double getGuiMapCameraX(Object guiMap) {
        return getDoubleField(guiMap, "cameraX");
    }

    /**
     * Reads the camera Z (world) from a {@code GuiMap} instance via reflection.
     * <p>Xaero 1.39.2: private {@code double cameraZ} field on GuiMap.</p>
     */
    public static double getGuiMapCameraZ(Object guiMap) {
        return getDoubleField(guiMap, "cameraZ");
    }

    /**
     * Reads the rendering scale from a {@code GuiMap} instance via reflection.
     * <p>Xaero 1.39.2: private {@code double scale} field on GuiMap —
     * pixels per block at the current zoom level.</p>
     */
    public static double getGuiMapScale(Object guiMap) {
        double s = getDoubleField(guiMap, "scale");
        // Guard against zero/uninitialized scale
        return s == 0.0 ? 1.0 : s;
    }

    /** Reads a {@code double} field by name from any object; returns 0.0 on failure. */
    private static double getDoubleField(Object target, String fieldName) {
        if (target == null) return 0.0;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getDouble(target);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                LOGGER.warn("[XaeroCompat] getDoubleField({}) failed: {}", fieldName, e.getMessage());
                return 0.0;
            }
        }
        return 0.0;
    }
}

