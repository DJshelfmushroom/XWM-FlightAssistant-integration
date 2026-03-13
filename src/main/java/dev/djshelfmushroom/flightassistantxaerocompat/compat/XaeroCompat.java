package dev.djshelfmushroom.flightassistantxaerocompat.compat;

import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    // All-waypoints list — used by InWorldWaypointRenderer
    // =========================================================================

    /**
     * Returns all Xaero waypoints saved for the player's current dimension.
     *
     * <p>Uses reflection with multiple fallback strategies to stay compatible
     * across Xaero versions. The hierarchy navigated is:
     * <pre>
     * XaeroMinimapSession (or WorldMap.instance)
     *   → WaypointsManager
     *     → WaypointWorldContainer  (one per dimension / server world)
     *       → WaypointSet(s)
     *         → List&lt;Waypoint&gt;
     * </pre>
     * Returns an empty list on any failure so callers never have to null-check.</p>
     *
     * <p>Xaero 1.39.2: {@code xaero.common.XaeroMinimapSession.getCurrentSession()
     * .getWaypointsManager()}</p>
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getCurrentDimensionWaypoints() {
        if (!FlightAssistantXaeroCompat.xaeroPresent) return Collections.emptyList();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Collections.emptyList();

        String dimension = mc.level.dimension().location().toString();

        // Strategy 1: via xaero.common.XaeroMinimapSession (available in Xaero Minimap + World Map)
        try {
            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Method getCurrentSession = sessionClass.getMethod("getCurrentSession");
            Object session = getCurrentSession.invoke(null);
            if (session != null) {
                List<Object> wps = extractWaypointsFromRoot(session);
                if (!wps.isEmpty()) return wps;
            }
        } catch (Exception ignored) {}

        // Strategy 2: via xaero.map.WorldMap static instance field
        try {
            Class<?> worldMapClass = Class.forName("xaero.map.WorldMap");
            Object worldMap = null;
            for (String fname : new String[]{"instance", "INSTANCE"}) {
                try {
                    Field f = worldMapClass.getDeclaredField(fname);
                    f.setAccessible(true);
                    worldMap = f.get(null);
                    if (worldMap != null) break;
                } catch (NoSuchFieldException ignored) {}
            }
            if (worldMap != null) {
                List<Object> wps = extractWaypointsFromRoot(worldMap);
                if (!wps.isEmpty()) return wps;
            }
        } catch (Exception ignored) {}

        // Strategy 3: try dimension-aware lookup via XaeroMinimapSession if strategies 1/2 returned empty
        try {
            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Method getCurrentSession = sessionClass.getMethod("getCurrentSession");
            Object session = getCurrentSession.invoke(null);
            if (session != null) {
                List<Object> wps = extractWaypointsFromRootByDimension(session, dimension);
                if (!wps.isEmpty()) return wps;
            }
        } catch (Exception ignored) {}

        return Collections.emptyList();
    }

    /**
     * Navigates from a session/mod-instance root down to the per-dimension
     * waypoint lists, using no-arg container getters (Xaero returns the
     * currently active world/dimension automatically).
     */
    @SuppressWarnings("unchecked")
    private static List<Object> extractWaypointsFromRoot(Object root) {
        // Step 1 — get WaypointsManager
        Object wm = tryInvokeOrField(root, "getWaypointsManager", "waypointsManager");
        if (wm == null) return Collections.emptyList();

        // Step 2 — get per-dimension container (no-arg variants that use the current world/dim)
        Object container = tryInvokeOrField(wm,
                "getCurrentWorldContainer", "getWorldContainer",
                "getCurrentDimensionContainer", "getDimensionContainer",
                "getContainer");
        if (container == null) return Collections.emptyList();

        // Step 3 — collect waypoints from all sets inside the container
        List<Object> result = new ArrayList<>();
        Object sets = tryInvokeOrField(container,
                "getAllWaypointSets", "getWaypointSets", "getAllSets", "getSets");
        if (sets instanceof Collection) {
            for (Object set : (Collection<?>) sets) {
                collectFromSet(set, result);
            }
        } else {
            // Container might itself be a single set
            collectFromSet(container, result);
        }
        return result;
    }

    /** Extracts the {@code List<Waypoint>} from a {@code WaypointSet} and appends it. */
    @SuppressWarnings("unchecked")
    private static void collectFromSet(Object set, List<Object> out) {
        if (set == null) return;
        Object list = tryInvokeOrField(set, "getList", "list", "getAll");
        if (list instanceof List) {
            out.addAll((List<?>) list);
        }
    }

    /**
     * Tries each name first as a no-arg method, then as a field (walking the
     * class hierarchy). Returns the first non-null result, or {@code null}.
     */
    private static Object tryInvokeOrField(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            // Try as public no-arg method
            try {
                Method m = target.getClass().getMethod(name);
                Object result = m.invoke(target);
                if (result != null) return result;
            } catch (Exception ignored) {}
            // Try as declared field (public or private), walking the superclass chain
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(name);
                    f.setAccessible(true);
                    Object result = f.get(target);
                    if (result != null) return result;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                } catch (Exception ignored) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Dimension-aware fallback: navigates to the waypoints manager and tries
     * single-arg container getter variants that accept a dimension string.
     * Used when the no-arg strategies in {@link #extractWaypointsFromRoot}
     * return no results (e.g. when the user is in a non-overworld dimension).
     */
    @SuppressWarnings("unchecked")
    private static List<Object> extractWaypointsFromRootByDimension(Object root, String dimension) {
        Object wm = tryInvokeOrField(root, "getWaypointsManager", "waypointsManager");
        if (wm == null) return Collections.emptyList();

        // Try single-arg container getters that accept the dimension resource-location
        Object container = null;
        for (String mname : new String[]{"getDimensionContainer", "getContainerForDimension", "getContainer"}) {
            try {
                Method m = wm.getClass().getMethod(mname, String.class);
                container = m.invoke(wm, dimension);
                if (container != null) break;
            } catch (Exception ignored) {}
        }
        if (container == null) return Collections.emptyList();

        List<Object> result = new ArrayList<>();
        Object sets = tryInvokeOrField(container,
                "getAllWaypointSets", "getWaypointSets", "getAllSets", "getSets");
        if (sets instanceof Collection) {
            for (Object set : (Collection<?>) sets) {
                collectFromSet(set, result);
            }
        } else {
            collectFromSet(container, result);
        }
        return result;
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

    /**
     * Reads the GUI scale used internally by Xaero's map screen.
     * <p>Xaero 1.39.2: private {@code double screenScale} on GuiMap.</p>
     */
    public static double getGuiMapScreenScale(Object guiMap) {
        double s = getDoubleField(guiMap, "screenScale");
        return s <= 0.0 ? 1.0 : s;
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
