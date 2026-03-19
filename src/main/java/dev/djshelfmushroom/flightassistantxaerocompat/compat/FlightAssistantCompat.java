package dev.djshelfmushroom.flightassistantxaerocompat.compat;

import dev.djshelfmushroom.flightassistantxaerocompat.FlightAssistantXaeroCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * All FlightAssistant API / reflection access is isolated here so that only
 * this file needs to change when FlightAssistant updates.
 *
 * <p>Every reflected field/method access is marked with a comment noting that
 * it was verified against FlightAssistant 3.0.1. All access is wrapped in
 * try/catch blocks so that failures are non-fatal.</p>
 *
 * <p>FlightAssistant 3.0.1 package root: {@code ru.octol1ttle.flightassistant}</p>
 */
public class FlightAssistantCompat {

    private static final Logger LOGGER = LogManager.getLogger(FlightAssistantXaeroCompat.MOD_ID);

    // -------------------------------------------------------------------------
    // Fully-qualified class names (verified against FA 3.0.1)
    // -------------------------------------------------------------------------

    /**
     * The ComputerHost singleton — FA 3.0.1's {@code internal object ComputerHost}.
     * In JVM bytecode, Kotlin {@code internal object} compiles to a class with a
     * public {@code INSTANCE} field.
     *
     * <p>Verified against FA 3.0.1: ComputerHost.kt</p>
     */
    private static final String CLASS_COMPUTER_HOST =
            "ru.octol1ttle.flightassistant.impl.computer.ComputerHost";

    /**
     * Manages autopilot modes.
     * <p>Verified against FA 3.0.1: AutoFlightComputer.kt</p>
     */
    private static final String CLASS_AUTO_FLIGHT_COMPUTER =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.AutoFlightComputer";

    /**
     * Contains the flight plan (departure, enroute list, arrival).
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt</p>
     */
    private static final String CLASS_FLIGHT_PLAN_COMPUTER =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer";

    /**
     * The COORDS lateral mode — flies toward fixed X/Z coordinates.
     * <p>Verified against FA 3.0.1: BuiltInLateralModes.kt,
     * class {@code DirectCoordinatesLateralMode(targetX: Int, targetZ: Int,
     * textOverride: Component? = null)}</p>
     */
    private static final String CLASS_DIRECT_COORDS_LATERAL =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.modes.DirectCoordinatesLateralMode";

    /**
     * The selected-altitude vertical mode — descends/climbs to a fixed world Y.
     * <p>Verified against FA 3.0.1: BuiltInVerticalModes.kt,
     * class {@code SelectedAltitudeVerticalMode(targetAltitude: Int,
     * textOverride: Component? = null)}</p>
     */
    private static final String CLASS_SELECTED_ALT_VERTICAL =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.modes.SelectedAltitudeVerticalMode";

    /**
     * The constant-thrust mode — holds a fixed thrust percentage.
     * <p>Verified against FA 3.0.1: BuiltInThrustModes.kt,
     * class {@code ConstantThrustMode(target: Float, textOverride: Component? = null)}</p>
     */
    private static final String CLASS_CONSTANT_THRUST =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.modes.ConstantThrustMode";

    /**
     * A single enroute waypoint in the flight plan.
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt,
     * nested data class {@code EnrouteWaypoint(coordinatesX, coordinatesZ,
     * altitude, speed, active, uuid)}</p>
     */
    private static final String CLASS_ENROUTE_WAYPOINT =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$EnrouteWaypoint";

    /**
     * The {@code Active} state enum nested inside {@code EnrouteWaypoint}.
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt</p>
     */
    private static final String CLASS_ENROUTE_ACTIVE =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$EnrouteWaypoint$Active";

    /**
     * Departure airport data — {@code data class DepartureData(coordinatesX, coordinatesZ,
     * elevation, takeoffThrust)}.
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt nested data class.</p>
     */
    private static final String CLASS_DEPARTURE_DATA =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$DepartureData";

    /**
     * Arrival airport data — {@code data class ArrivalData(coordinatesX, coordinatesZ,
     * elevation, landingThrust, minimums, minimumsType, goAroundAltitude,
     * approachReEntryWaypointIndex)}.
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt nested data class.</p>
     */
    private static final String CLASS_ARRIVAL_DATA =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$ArrivalData";

    /**
     * MinimumsType enum nested inside {@code ArrivalData}.
     * <p>Verified against FA 3.0.1: FlightPlanComputer.kt</p>
     */
    private static final String CLASS_MINIMUMS_TYPE =
            "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$ArrivalData$MinimumsType";

    /**
     * FA 3.0.1 FMS departure screen — has a companion object with a static {@code state}
     * field and a {@code reload(DepartureData)} method to sync it with live plan data.
     */
    private static final String CLASS_DEPARTURE_SCREEN =
            "ru.octol1ttle.flightassistant.screen.fms.departure.DepartureScreen";

    /**
     * FA 3.0.1 FMS arrival screen — companion {@code reload(ArrivalData)}.
     */
    private static final String CLASS_ARRIVAL_SCREEN =
            "ru.octol1ttle.flightassistant.screen.fms.arrival.ArrivalScreen";

    /**
     * FA 3.0.1 FMS enroute screen — companion {@code reload(List<EnrouteWaypoint>)}.
     */
    private static final String CLASS_ENROUTE_SCREEN =
            "ru.octol1ttle.flightassistant.screen.fms.enroute.EnrouteScreen";

    // ResourceLocation IDs for each computer (verified against FA 3.0.1)
    // AutoFlightComputer.ID = FlightAssistant.id("auto_flight")
    private static final ResourceLocation ID_AUTO_FLIGHT =
            new ResourceLocation("flightassistant", "auto_flight");
    // FlightPlanComputer.ID = FlightAssistant.id("flight_plan")
    private static final ResourceLocation ID_FLIGHT_PLAN =
            new ResourceLocation("flightassistant", "flight_plan");

    // -------------------------------------------------------------------------
    // Chat prefix shared by all user-visible messages
    // -------------------------------------------------------------------------
    private static final String CHAT_PREFIX = "§7[FlightAssistant] §r";

    // =========================================================================
    // Per-tick navigation state
    // =========================================================================

    /** Sentinel: navigation tracker has not been initialised or autopilot was off. */
    private static final int UNINITIALIZED_INDEX = Integer.MIN_VALUE;

    /**
     * Active-waypoint index cached from the previous call to
     * {@link #tickNavigation()}.  Used to detect when FA's internal plan
     * advances to a new waypoint (or finishes the enroute sequence entirely).
     */
    private static int lastTickActiveWaypointIndex = UNINITIALIZED_INDEX;

    /**
     * {@code true} once the one-shot approach fallback has been fired for the
     * current "all enroute done" segment, preventing repeated
     * {@code setCoordsTarget} calls every tick.
     */
    private static boolean approachFallbackTriggered = false;

    // -------------------------------------------------------------------------
    // Reflection caches for getActiveWaypointIndex() — populated on first use
    // to avoid Class.forName + enum scan + getMethod on every client tick.
    // -------------------------------------------------------------------------

    /** Cached {@code EnrouteWaypoint.Active} enum class, or {@code null} if not yet loaded. */
    private static Class<?> cachedEnrouteActiveClass;
    /** Cached {@code Active.TARGET} enum constant, or {@code null} if not yet loaded. */
    private static Object   cachedTargetConstant;
    /**
     * Cached {@code getActive()} method retrieved from the first EnrouteWaypoint instance seen.
     * All waypoints share the same class so one cache entry suffices.
     */
    private static Method   cachedGetActiveMethod;
    /** Set to {@code true} if the enum class or TARGET constant could not be loaded. */
    private static boolean  enrouteActiveClassLoadFailed;

    // =========================================================================
    // Computer access helpers
    // =========================================================================

    /**
     * Returns the {@code ComputerHost.INSTANCE} singleton, or {@code null}.
     *
     * <p>Verified against FA 3.0.1: {@code internal object ComputerHost} →
     * JVM class with public static {@code INSTANCE} field.</p>
     */
    private static Object getComputerHost() {
        try {
            Class<?> clazz = Class.forName(CLASS_COMPUTER_HOST);
            // Kotlin objects compile to a class with a public static INSTANCE field
            Field instance = clazz.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getComputerHost failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a specific computer from the {@code ComputerHost} by its
     * {@code ResourceLocation} ID.
     *
     * <p>Verified against FA 3.0.1: {@code ModuleView.get(ResourceLocation)} is
     * implemented by {@code ComputerHost}.</p>
     */
    private static Object getComputer(ResourceLocation id) {
        Object host = getComputerHost();
        if (host == null) return null;
        try {
            // Verified against FA 3.0.1: ComputerHost.get(ResourceLocation)
            Method get = host.getClass().getMethod("get", ResourceLocation.class);
            return get.invoke(host, id);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getComputer({}) failed: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the {@code AutoFlightComputer} instance, or {@code null}.
     *
     * <p>Verified against FA 3.0.1: {@code AutoFlightComputer.ID =
     * FlightAssistant.id("auto_flight")}</p>
     */
    public static Object getAutoFlightComputer() {
        Object computer = getComputer(ID_AUTO_FLIGHT);
        if (computer != null) return computer;
        return getComputerByHostGetter("getAutoflight");
    }

    /**
     * Returns the {@code FlightPlanComputer} instance, or {@code null}.
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.ID =
     * FlightAssistant.id("flight_plan")}</p>
     */
    public static Object getFlightPlanComputer() {
        Object computer = getComputer(ID_FLIGHT_PLAN);
        if (computer != null) return computer;
        return getComputerByHostGetter("getPlan");
    }

    /**
     * Fallback for cases where lookup by ResourceLocation fails:
     * uses direct ComputerHost getters (e.g. getAutoflight/getPlan).
     */
    private static Object getComputerByHostGetter(String getterName) {
        Object host = getComputerHost();
        if (host == null) return null;
        try {
            Method getter = host.getClass().getMethod(getterName);
            return getter.invoke(host);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] {} fallback failed: {}", getterName, e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // AutoFlightComputer helpers
    // =========================================================================

    /**
     * Returns {@code true} if the autopilot is currently engaged.
     *
     * <p>Verified against FA 3.0.1: {@code AutoFlightComputer.autopilot: Boolean}
     * → JVM getter {@code getAutopilot()}.</p>
     */
    public static boolean isAutopilotEngaged() {
        Object afc = getAutoFlightComputer();
        if (afc == null) return false;
        try {
            // Verified against FA 3.0.1: getAutopilot() on AutoFlightComputer
            Method getter = afc.getClass().getMethod("getAutopilot");
            Object val = getter.invoke(afc);
            return Boolean.TRUE.equals(val);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] isAutopilotEngaged failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets FlightAssistant's lateral mode to {@code COORDS}, updates the
     * target to the given X/Z position, and engages autopilot.
     *
     * <p>Vertical and thrust modes are left completely untouched.</p>
     *
     * <p>Verified against FA 3.0.1:
     * <ul>
     *   <li>{@code AutoFlightComputer.selectedLateralMode: LateralMode?} —
     *       JVM setter {@code setSelectedLateralMode(LateralMode)}</li>
     *   <li>{@code DirectCoordinatesLateralMode(targetX: Int, targetZ: Int,
     *       textOverride: Component?)} — primary constructor</li>
     *   <li>{@code AutoFlightComputer.setAutoPilot(Boolean, Boolean?)} —
     *       engages autopilot so heading control is actually applied</li>
     * </ul>
     * </p>
     *
     * @param x world X coordinate of the target
     * @param z world Z coordinate of the target
     * @return {@code true} if the change was applied successfully
     */
    public static boolean setCoordsTarget(double x, double z) {
        Object afc = getAutoFlightComputer();
        if (afc == null) {
            LOGGER.warn("[FACompat] setCoordsTarget: AutoFlightComputer not available");
            return false;
        }
        try {
            // Build a DirectCoordinatesLateralMode(targetX: Int, targetZ: Int, textOverride: Component?)
            // Verified against FA 3.0.1: BuiltInLateralModes.kt
            Class<?> modeClass = Class.forName(CLASS_DIRECT_COORDS_LATERAL);
            // Primary constructor: (int, int, Component) — textOverride has a Kotlin default of null
            Object modeInstance = modeClass
                    .getDeclaredConstructor(int.class, int.class, Component.class)
                    .newInstance((int) x, (int) z, null);

            // Call setSelectedLateralMode(LateralMode?)
            // Verified against FA 3.0.1: Kotlin var selectedLateralMode generates setSelectedLateralMode
            Method setter = findMethodByName(afc.getClass(), "setSelectedLateralMode");
            if (setter == null) {
                LOGGER.warn("[FACompat] setSelectedLateralMode not found on AutoFlightComputer");
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(afc, modeInstance);
            return enableAutoPilot(afc);

        } catch (Exception e) {
            LOGGER.warn("[FACompat] setCoordsTarget failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets FA's {@code selectedVerticalMode} to
     * {@code SelectedAltitudeVerticalMode(targetAltitude)} so the autopilot
     * descends/climbs toward the given absolute MC world Y coordinate.
     *
     * <p>Verified against FA 3.0.1: {@code BuiltInVerticalModes.kt} —
     * {@code SelectedAltitudeVerticalMode(targetAltitude: Int, textOverride: Component?)}
     * and {@code AutoFlightComputer.setSelectedVerticalMode(VerticalMode)}.</p>
     *
     * @param targetAltitude absolute MC world Y to target (must be {@code != 0};
     *                       0 is FA's "not set" sentinel and will be skipped)
     * @return {@code true} if applied successfully
     */
    public static boolean setApproachVerticalMode(int targetAltitude) {
        if (targetAltitude == 0) {
            LOGGER.debug("[FACompat] setApproachVerticalMode: skipped — elevation is 0 (unset sentinel)");
            return false;
        }
        Object afc = getAutoFlightComputer();
        if (afc == null) return false;
        try {
            // Verified against FA 3.0.1: SelectedAltitudeVerticalMode(int, Component)
            Class<?> modeClass = Class.forName(CLASS_SELECTED_ALT_VERTICAL);
            Object modeInstance = modeClass
                    .getDeclaredConstructor(int.class, Component.class)
                    .newInstance(targetAltitude, null);

            Method setter = findMethodByName(afc.getClass(), "setSelectedVerticalMode");
            if (setter == null) {
                LOGGER.warn("[FACompat] setSelectedVerticalMode not found on AutoFlightComputer");
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(afc, modeInstance);
            return true;
        } catch (Exception e) {
            LOGGER.warn("[FACompat] setApproachVerticalMode failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets FA's {@code selectedThrustMode} to {@code ConstantThrustMode(target)}
     * so the autopilot holds the given thrust fraction during approach/landing.
     *
     * <p>Verified against FA 3.0.1: {@code BuiltInThrustModes.kt} —
     * {@code ConstantThrustMode(target: Float, textOverride: Component?)}
     * and {@code AutoFlightComputer.setSelectedThrustMode(ThrustMode)}.</p>
     *
     * @param thrust approach thrust fraction in [0, 1] (e.g. {@code 0.3f} = 30 %)
     * @return {@code true} if applied successfully
     */
    public static boolean setApproachThrustMode(float thrust) {
        Object afc = getAutoFlightComputer();
        if (afc == null) return false;
        try {
            // Verified against FA 3.0.1: ConstantThrustMode(float, Component)
            Class<?> modeClass = Class.forName(CLASS_CONSTANT_THRUST);
            Object modeInstance = modeClass
                    .getDeclaredConstructor(float.class, Component.class)
                    .newInstance(thrust, null);

            Method setter = findMethodByName(afc.getClass(), "setSelectedThrustMode");
            if (setter == null) {
                LOGGER.warn("[FACompat] setSelectedThrustMode not found on AutoFlightComputer");
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(afc, modeInstance);
            return true;
        } catch (Exception e) {
            LOGGER.warn("[FACompat] setApproachThrustMode failed: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // FlightPlanComputer helpers
    // =========================================================================

    /**
     * Returns the enroute waypoint list from the current flight plan.
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.enrouteData:
     * MutableList<EnrouteWaypoint>} — JVM getter {@code getEnrouteData()}.</p>
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getEnrouteWaypoints() {
        Object plan = getFlightPlanComputer();
        if (plan == null) return Collections.emptyList();
        try {
            // Verified against FA 3.0.1: getEnrouteData() on FlightPlanComputer
            Method getter = plan.getClass().getMethod("getEnrouteData");
            Object list = getter.invoke(plan);
            if (list instanceof List) return (List<Object>) list;
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getEnrouteWaypoints failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Returns the departure data object (or {@code null} if default / unset).
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.departureData:
     * DepartureData} — JVM getter {@code getDepartureData()}.</p>
     */
    public static Object getDepartureData() {
        Object plan = getFlightPlanComputer();
        if (plan == null) return null;
        try {
            // Verified against FA 3.0.1: getDepartureData() on FlightPlanComputer
            Method getter = plan.getClass().getMethod("getDepartureData");
            return getter.invoke(plan);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getDepartureData failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns the arrival data object (or {@code null} if default / unset).
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.arrivalData:
     * ArrivalData} — JVM getter {@code getArrivalData()}.</p>
     */
    public static Object getArrivalData() {
        Object plan = getFlightPlanComputer();
        if (plan == null) return null;
        try {
            // Verified against FA 3.0.1: getArrivalData() on FlightPlanComputer
            Method getter = plan.getClass().getMethod("getArrivalData");
            return getter.invoke(plan);
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getArrivalData failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reads the X coordinate of any flight-plan position object
     * (DepartureData, EnrouteWaypoint, or ArrivalData).
     *
     * <p>Verified against FA 3.0.1: all three data classes have
     * {@code coordinatesX: Int} → JVM getter {@code getCoordinatesX()}.</p>
     */
    public static Integer getPlanCoordinatesX(Object dataObj) {
        if (dataObj == null) return null;
        try {
            // Verified against FA 3.0.1: getCoordinatesX() on DepartureData/EnrouteWaypoint/ArrivalData
            Method m = dataObj.getClass().getMethod("getCoordinatesX");
            Object v = m.invoke(dataObj);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getPlanCoordinatesX failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Reads the Z coordinate of any flight-plan position object.
     *
     * <p>Verified against FA 3.0.1: all three data classes have
     * {@code coordinatesZ: Int} → JVM getter {@code getCoordinatesZ()}.</p>
     */
    public static Integer getPlanCoordinatesZ(Object dataObj) {
        if (dataObj == null) return null;
        try {
            // Verified against FA 3.0.1: getCoordinatesZ() on DepartureData/EnrouteWaypoint/ArrivalData
            Method m = dataObj.getClass().getMethod("getCoordinatesZ");
            Object v = m.invoke(dataObj);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getPlanCoordinatesZ failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Reads the target altitude of an enroute waypoint.
     *
     * <p>Verified against FA 3.0.1: {@code EnrouteWaypoint.altitude: Int}
     * → JVM getter {@code getAltitude()}.</p>
     */
    public static Integer getEnrouteAltitude(Object waypointObj) {
        if (waypointObj == null) return null;
        try {
            // Verified against FA 3.0.1: getAltitude() on EnrouteWaypoint
            Method m = waypointObj.getClass().getMethod("getAltitude");
            Object v = m.invoke(waypointObj);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getEnrouteAltitude failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Reads the airport elevation of a {@code DepartureData} or
     * {@code ArrivalData} object.
     *
     * <p>Verified against FA 3.0.1: {@code DepartureData.elevation: Int} and
     * {@code ArrivalData.elevation: Int} → JVM getter {@code getElevation()}.</p>
     *
     * <p><b>Coordinate note:</b> FA stores elevation as an <em>absolute MC world
     * Y coordinate</em> (same as the block Y in the world), not as a height above
     * sea level or height above terrain.  The value 0 is used as the
     * "not set / default" sentinel by FA's own data classes.</p>
     *
     * @param planData a {@code DepartureData} or {@code ArrivalData} instance
     * @return the stored elevation (absolute world Y), or {@code null} if not available
     */
    public static Integer getPlanElevation(Object planData) {
        if (planData == null) return null;
        try {
            Method m = planData.getClass().getMethod("getElevation");
            Object v = m.invoke(planData);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (NoSuchMethodException e) {
            // Method absent — silently return null (no WARN to avoid per-frame spam)
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getPlanElevation failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Returns the landing thrust fraction (0–1) stored in an {@code ArrivalData} object.
     *
     * <p>Verified against FA 3.0.1: {@code ArrivalData.landingThrust: Float}
     * → JVM getter {@code getLandingThrust()}.</p>
     *
     * @param arrivalData an {@code ArrivalData} instance
     * @return the landing thrust fraction, or {@code null} if not available
     */
    public static Float getPlanLandingThrust(Object arrivalData) {
        if (arrivalData == null) return null;
        try {
            Method m = arrivalData.getClass().getMethod("getLandingThrust");
            Object v = m.invoke(arrivalData);
            if (v instanceof Number) return ((Number) v).floatValue();
        } catch (NoSuchMethodException e) {
            // Method absent — silently return null
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getPlanLandingThrust failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Returns the index of the active (TARGET) enroute waypoint, or {@code -1}.
     *
     * <p>Verified against FA 3.0.1: {@code EnrouteWaypoint.active: Active?}
     * where {@code Active.TARGET} marks the currently targeted waypoint.</p>
     *
     * <p>Reflection artefacts ({@code Active} class, {@code TARGET} constant,
     * {@code getActive} method) are cached on first successful use to avoid
     * repeated {@code Class.forName} and enum-scan overhead on every client
     * tick.</p>
     */
    public static int getActiveWaypointIndex() {
        List<Object> wps = getEnrouteWaypoints();
        if (wps == null || wps.isEmpty()) return -1;
        try {
            // Load and cache the Active enum class + TARGET constant on first call.
            if (!enrouteActiveClassLoadFailed && cachedTargetConstant == null) {
                Class<?> activeClass = Class.forName(CLASS_ENROUTE_ACTIVE);
                for (Object c : activeClass.getEnumConstants()) {
                    if ("TARGET".equals(((Enum<?>) c).name())) {
                        cachedTargetConstant = c;
                        cachedEnrouteActiveClass = activeClass;
                        break;
                    }
                }
                if (cachedTargetConstant == null) {
                    enrouteActiveClassLoadFailed = true;
                    return -1;
                }
            }
            if (enrouteActiveClassLoadFailed) return -1;

            // Pre-populate cachedGetActiveMethod from the first waypoint's class;
            // all EnrouteWaypoint instances share the same class so one entry suffices.
            // Verified against FA 3.0.1: getActive() on EnrouteWaypoint
            if (cachedGetActiveMethod == null) {
                cachedGetActiveMethod = wps.get(0).getClass().getMethod("getActive");
            }

            for (int i = 0; i < wps.size(); i++) {
                Object wp = wps.get(i);
                Object active = cachedGetActiveMethod.invoke(wp);
                if (cachedTargetConstant.equals(active)) return i;
            }
        } catch (Exception e) {
            LOGGER.warn("[FACompat] getActiveWaypointIndex failed: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Returns true if a departure or arrival data object is the default (empty) sentinel.
     *
     * <p>Verified against FA 3.0.1: {@code DepartureData.isDefault()} and
     * {@code ArrivalData.isDefault()}.</p>
     */
    public static boolean isPlanDataDefault(Object dataObj) {
        if (dataObj == null) return true;
        try {
            // Verified against FA 3.0.1: isDefault() on DepartureData and ArrivalData
            Method m = dataObj.getClass().getMethod("isDefault");
            Object result = m.invoke(dataObj);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // If the method doesn't exist, treat as non-default (i.e., has data)
        }
        return false;
    }

    /**
     * Appends a new waypoint to the end of the enroute waypoint list.
     *
     * <p>This method is safe to call while the autopilot is active — it only
     * appends, never reorders.</p>
     *
     * <p>Verified against FA 3.0.1:
     * <ul>
     *   <li>{@code FlightPlanComputer.enrouteData: MutableList<EnrouteWaypoint>}
     *       — the backing ArrayList is mutated directly via the returned List ref</li>
     *   <li>{@code EnrouteWaypoint(coordinatesX: Int, coordinatesZ: Int,
     *       altitude: Int, speed: Int, active: Active?, uuid: UUID)}
     *       — primary constructor (6 params)</li>
     * </ul>
     * </p>
     *
     * @param x        world X coordinate (converted to int)
     * @param z        world Z coordinate (converted to int)
     * @param altitude target altitude in blocks (Y, converted to int)
     * @param speed    optional target speed (pass {@code null} to use 0 = unset)
     * @return {@code true} if the waypoint was added successfully
     */
    @SuppressWarnings("unchecked")
    public static boolean addEnrouteWaypoint(double x, double z, double altitude, Double speed) {
        List<Object> waypoints = getEnrouteWaypoints();
        if (waypoints == null) {
            LOGGER.warn("[FACompat] addEnrouteWaypoint: could not obtain enroute waypoint list");
            return false;
        }
        try {
            Class<?> wpClass = Class.forName(CLASS_ENROUTE_WAYPOINT);
            Class<?> activeClass = Class.forName(CLASS_ENROUTE_ACTIVE);
            Object activeTarget = hasAnyActiveWaypoint(waypoints) ? null : getEnumConstant(activeClass, "TARGET");

            // Verified against FA 3.0.1: primary constructor (int, int, int, int, Active?, UUID)
            Object newWaypoint = wpClass
                    .getDeclaredConstructor(int.class, int.class, int.class, int.class, activeClass, UUID.class)
                    .newInstance(
                            (int) x,
                            (int) z,
                            (int) altitude,
                            speed != null ? speed.intValue() : 0,
                            activeTarget,
                            UUID.randomUUID()
                    );

            waypoints.add(newWaypoint);
            reloadFMSEnrouteScreen(waypoints);
            clearSelectedLateralMode();
            return true;

        } catch (NoSuchMethodException ex) {
            // Fallback for synthetic constructor with Kotlin default-parameter marker
            // mask = 0b110000 = 48 → use defaults for params 4 (active) and 5 (uuid)
            try {
                Class<?> wpClass     = Class.forName(CLASS_ENROUTE_WAYPOINT);
                Class<?> activeClass = Class.forName(CLASS_ENROUTE_ACTIVE);
                Class<?> marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                Object newWaypoint = wpClass
                        .getDeclaredConstructor(int.class, int.class, int.class, int.class,
                                activeClass, UUID.class, int.class, marker)
                        .newInstance(
                                (int) x,
                                (int) z,
                                (int) altitude,
                                speed != null ? speed.intValue() : 0,
                                null, null,  // replaced by defaults (active=null, uuid=random)
                                48,          // mask: bits 4+5 → use defaults
                                null         // DefaultConstructorMarker
                        );
                if (!hasAnyActiveWaypoint(waypoints)) {
                    Object target = getEnumConstant(activeClass, "TARGET");
                    if (target != null) {
                        setWaypointActive(newWaypoint, target);
                    }
                }
                waypoints.add(newWaypoint);
                reloadFMSEnrouteScreen(waypoints);
                clearSelectedLateralMode();
                return true;
            } catch (Exception e2) {
                LOGGER.warn("[FACompat] addEnrouteWaypoint (fallback) failed: {}", e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("[FACompat] addEnrouteWaypoint failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets the flight plan departure waypoint to the given X/Z map coordinate.
     *
     * <p>Creates a {@code DepartureData(coordinatesX, coordinatesZ, elevation,
     * takeoffThrust=1.0)} instance and stores it on the {@code FlightPlanComputer}.
     * The player can refine values through FA's own FMS screen later.</p>
     *
     * <p>{@code elevation} is the absolute MC world Y coordinate of the runway
     * surface (not terrain-relative, not sea-level-relative).  Passing 0 signals
     * "not set"; the in-world renderer will then fall back to the terrain surface
     * height at that location.</p>
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.departureData} is a
     * Kotlin {@code var} property → JVM setter {@code setDepartureData(DepartureData)}.
     * {@code DepartureData} primary constructor: {@code (Int, Int, Int, Float)}.</p>
     *
     * @param x         world X coordinate (converted to int)
     * @param z         world Z coordinate (converted to int)
     * @param elevation absolute MC world Y of the runway surface, or 0 if unknown
     * @return {@code true} if the departure was set successfully
     */
    public static boolean setDepartureWaypoint(double x, double z, int elevation) {
        Object plan = getFlightPlanComputer();
        if (plan == null) {
            LOGGER.warn("[FACompat] setDepartureWaypoint: FlightPlanComputer not available");
            return false;
        }
        try {
            // Verified against FA 3.0.1: DepartureData(coordinatesX: Int, coordinatesZ: Int,
            //   elevation: Int, takeoffThrust: Float) — primary constructor.
            // elevation is absolute MC world Y; takeoffThrust 1.0f = full power for takeoff.
            Class<?> depClass = Class.forName(CLASS_DEPARTURE_DATA);
            Object depData = depClass
                    .getDeclaredConstructor(int.class, int.class, int.class, float.class)
                    .newInstance((int) x, (int) z, elevation, 1.0f /* takeoffThrust: 100% power */);

            // Verified against FA 3.0.1: Kotlin var departureData → setDepartureData(DepartureData)
            Method setter = findMethodByName(plan.getClass(), "setDepartureData");
            if (setter == null) {
                LOGGER.warn("[FACompat] setDepartureData not found on FlightPlanComputer");
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(plan, depData);
            reloadFMSDepartureScreen(depData);
            clearSelectedLateralMode();
            return true;
        } catch (NoSuchMethodException ex) {
            // Fallback: use Kotlin synthetic constructor — pass all params explicitly (mask=0)
            try {
                Class<?> depClass = Class.forName(CLASS_DEPARTURE_DATA);
                Class<?> marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                Object depData = depClass
                        .getDeclaredConstructor(int.class, int.class, int.class, float.class, int.class, marker)
                        .newInstance((int) x, (int) z, elevation, 1.0f /* takeoffThrust: 100% power */, 0 /* mask=0: all params explicit */, null);
                Method setter = findMethodByName(plan.getClass(), "setDepartureData");
                if (setter == null) {
                    LOGGER.warn("[FACompat] setDepartureData (fallback) not found");
                    return false;
                }
                setter.setAccessible(true);
                setter.invoke(plan, depData);
                reloadFMSDepartureScreen(depData);
                clearSelectedLateralMode();
                return true;
            } catch (Exception e2) {
                LOGGER.warn("[FACompat] setDepartureWaypoint (fallback) failed: {}", e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("[FACompat] setDepartureWaypoint failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sets the flight plan arrival waypoint to the given X/Z map coordinate.
     *
     * <p>Creates an {@code ArrivalData(coordinatesX, coordinatesZ, elevation,
     * landingThrust=0.3)} instance and stores it on the {@code FlightPlanComputer}.
     * The remaining fields (minimums, minimumsType, goAroundAltitude,
     * approachReEntryWaypointIndex) are left at their FA defaults.
     * The player can refine them through FA's own FMS screen.</p>
     *
     * <p>{@code elevation} is the absolute MC world Y coordinate of the runway
     * surface (not terrain-relative, not sea-level-relative).  Passing 0 signals
     * "not set"; the in-world renderer will then fall back to the terrain surface
     * height at that location.</p>
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.arrivalData} is a
     * Kotlin {@code var} property → JVM setter {@code setArrivalData(ArrivalData)}.
     * {@code ArrivalData} primary constructor:
     * {@code (Int, Int, Int, Float, Int, MinimumsType, Int, Int)}.</p>
     *
     * @param x         world X coordinate (converted to int)
     * @param z         world Z coordinate (converted to int)
     * @param elevation absolute MC world Y of the runway surface, or 0 if unknown
     * @return {@code true} if the arrival was set successfully
     */
    public static boolean setArrivalWaypoint(double x, double z, int elevation) {
        Object plan = getFlightPlanComputer();
        if (plan == null) {
            LOGGER.warn("[FACompat] setArrivalWaypoint: FlightPlanComputer not available");
            return false;
        }
        try {
            // Verified against FA 3.0.1: ArrivalData(coordinatesX: Int, coordinatesZ: Int,
            //   elevation: Int, landingThrust: Float, minimums: Int,
            //   minimumsType: MinimumsType, goAroundAltitude: Int, approachReEntryWaypointIndex: Int)
            // elevation is absolute MC world Y; landingThrust 0.3f = 30% approach power.
            Class<?> arrClass    = Class.forName(CLASS_ARRIVAL_DATA);
            Class<?> minTypeClass = Class.forName(CLASS_MINIMUMS_TYPE);
            Object absoluteConst = getEnumConstant(minTypeClass, "ABSOLUTE");

            Object arrData = arrClass
                    .getDeclaredConstructor(int.class, int.class, int.class, float.class,
                            int.class, minTypeClass, int.class, int.class)
                    .newInstance((int) x, (int) z, elevation, 0.3f /* landingThrust: 30% approach power */, 0, absoluteConst, 0, 0);

            // Verified against FA 3.0.1: Kotlin var arrivalData → setArrivalData(ArrivalData)
            Method setter = findMethodByName(plan.getClass(), "setArrivalData");
            if (setter == null) {
                LOGGER.warn("[FACompat] setArrivalData not found on FlightPlanComputer");
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(plan, arrData);
            reloadFMSArrivalScreen(arrData);
            clearSelectedLateralMode();
            return true;
        } catch (NoSuchMethodException ex) {
            // Fallback: use Kotlin synthetic constructor.
            // Bits 4-7 (mask=240) → defaults for minimums/minimumsType/goAroundAlt/approachReEntry;
            // elevation (bit 2) and landingThrust (bit 3) are passed explicitly.
            try {
                Class<?> arrClass    = Class.forName(CLASS_ARRIVAL_DATA);
                Class<?> minTypeClass = Class.forName(CLASS_MINIMUMS_TYPE);
                Class<?> marker      = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
                Object arrData = arrClass
                        .getDeclaredConstructor(int.class, int.class, int.class, float.class,
                                int.class, minTypeClass, int.class, int.class, int.class, marker)
                        .newInstance((int) x, (int) z, elevation, 0.3f /* landingThrust: 30% approach power */, 0, null, 0, 0,
                                240 /* bits 4-7: defaults for minimums/minimumsType/goAroundAlt/approachReEntry */, null);
                Method setter = findMethodByName(plan.getClass(), "setArrivalData");
                if (setter == null) {
                    LOGGER.warn("[FACompat] setArrivalData (fallback) not found");
                    return false;
                }
                setter.setAccessible(true);
                setter.invoke(plan, arrData);
                reloadFMSArrivalScreen(arrData);
                clearSelectedLateralMode();
                return true;
            } catch (Exception e2) {
                LOGGER.warn("[FACompat] setArrivalWaypoint (fallback) failed: {}", e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("[FACompat] setArrivalWaypoint failed: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Per-tick navigation management
    // =========================================================================

    /**
     * Monitors the flight-plan navigation state and corrects the autopilot's
     * lateral mode when FA's internal plan advances past the last enroute
     * waypoint into the arrival/approach phase.
     *
     * <p>Call this once per client tick while FlightAssistant is loaded.</p>
     *
     * <h4>Problem addressed</h4>
     * <p>When the player uses <em>Fly Here</em> (or <em>[FA] Set as COORDS
     * Target</em>) to navigate toward an enroute waypoint,
     * {@link #setCoordsTarget} writes a static
     * {@code DirectCoordinatesLateralMode} into
     * {@code AutoFlightComputer.selectedLateralMode}.  FA resolves the active
     * lateral mode as {@code selectedLateralMode ?: plan.getLateralMode()}, so
     * this override persists even after FA's own plan marks the waypoint as
     * passed and tries to transition to the arrival approach.  The result is
     * that the autopilot keeps flying toward — and past — the already-passed
     * enroute waypoint instead of initiating the approach.</p>
     *
     * <h4>Fix — two layers</h4>
     * <ol>
     *   <li><b>Clear on plan advance</b> — the last active-waypoint index is
     *       cached each tick; whenever it changes (FA advanced the plan),
     *       {@link #clearSelectedLateralMode()} is called so FA's own sequencer
     *       can take over for the new phase.</li>
     *   <li><b>One-shot approach fallback</b> — as a safety net, when all
     *       enroute waypoints are done, arrival data is present, and no lateral
     *       mode is currently active (FA's plan returned {@code null}), a
     *       direct-to-arrival COORDS target is set once, together with a
     *       {@code SelectedAltitudeVerticalMode} targeting the runway elevation
     *       and a {@code ConstantThrustMode} at the arrival's landing thrust,
     *       so the plane descends and reduces power on the way to the
     *       destination.</li>
     * </ol>
     */
    public static void tickNavigation() {
        if (!isAutopilotEngaged()) {
            lastTickActiveWaypointIndex = UNINITIALIZED_INDEX;
            approachFallbackTriggered = false;
            return;
        }

        int currentActiveIdx = getActiveWaypointIndex();

        // Layer 1 — detect plan advancement and clear any stale COORDS override.
        if (lastTickActiveWaypointIndex != UNINITIALIZED_INDEX
                && currentActiveIdx != lastTickActiveWaypointIndex) {
            clearSelectedLateralMode();
            approachFallbackTriggered = false;
            LOGGER.debug("[FACompat] Plan advanced ({} → {}): cleared lateral-mode override.",
                    lastTickActiveWaypointIndex, currentActiveIdx);
        }

        lastTickActiveWaypointIndex = currentActiveIdx;

        // Layer 2 — one-shot approach fallback.
        // If all enroute waypoints have been passed (no active TARGET) but
        // arrival data is present, and no lateral mode is currently selected
        // (FA's plan mode returned null), explicitly fly toward the arrival so
        // the plane does not just maintain its last heading.
        // Also set the vertical mode to descend to the runway elevation and the
        // thrust mode to the arrival's landing thrust so FA doesn't keep full
        // throttle all the way to the ground.
        if (!approachFallbackTriggered && currentActiveIdx == -1) {
            List<Object> enroute = getEnrouteWaypoints();
            if (enroute != null && !enroute.isEmpty()) {
                Object arrival = getArrivalData();
                if (arrival != null && !isPlanDataDefault(arrival)) {
                    Integer arrX = getPlanCoordinatesX(arrival);
                    Integer arrZ = getPlanCoordinatesZ(arrival);
                    if (arrX != null && arrZ != null
                            && !isSelectedLateralModeActive()
                            && getPlanLateralMode() == null) {
                        setCoordsTarget(arrX, arrZ);

                        // Descend to runway elevation if it is set (non-zero sentinel).
                        Integer elevation = getPlanElevation(arrival);
                        if (elevation != null && elevation != 0) {
                            setApproachVerticalMode(elevation);
                        }

                        // Reduce thrust to the arrival's landing-thrust setting.
                        Float landingThrust = getPlanLandingThrust(arrival);
                        if (landingThrust != null) {
                            setApproachThrustMode(landingThrust);
                        }

                        approachFallbackTriggered = true;
                        LOGGER.info("[FACompat] No lateral mode after enroute — "
                                + "initiated fallback approach to arrival ({}, {}), "
                                + "elevation={}, landingThrust={}.",
                                arrX, arrZ, elevation, landingThrust);
                        sendChatMessage("§eApproach to arrival initiated.");
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if {@code AutoFlightComputer.selectedLateralMode}
     * is currently non-{@code null} (i.e. a manual COORDS override is active).
     */
    private static boolean isSelectedLateralModeActive() {
        Object afc = getAutoFlightComputer();
        if (afc == null) return false;
        try {
            Method getter = findMethodByName(afc.getClass(), "getSelectedLateralMode");
            if (getter == null) return false;
            getter.setAccessible(true);
            return getter.invoke(afc) != null;
        } catch (Exception e) {
            LOGGER.debug("[FACompat] isSelectedLateralModeActive failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the lateral mode that {@code FlightPlanComputer.getLateralMode()}
     * would supply (i.e. FA's plan-driven lateral mode, excluding any manual
     * {@code selectedLateralMode} override), or {@code null} if the plan
     * currently provides no lateral guidance.
     *
     * <p>This is used by the layer-2 approach fallback in
     * {@link #tickNavigation()} to distinguish between "FA's plan has an
     * approach mode ready" (do not override it) and "FA's plan returned
     * nothing" (safe to fire the fallback).</p>
     *
     * <p>Verified against FA 3.0.1: {@code FlightPlanComputer.getLateralMode():
     * LateralMode?} — a regular function returning a nullable
     * {@code LateralMode}.</p>
     */
    private static Object getPlanLateralMode() {
        Object plan = getFlightPlanComputer();
        if (plan == null) return null;
        try {
            Method m = findMethodByName(plan.getClass(), "getLateralMode");
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(plan);
        } catch (Exception e) {
            LOGGER.debug("[FACompat] getPlanLateralMode failed: {}", e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    // -------------------------------------------------------------------------
    // FMS companion-object reload helpers
    // -------------------------------------------------------------------------
    // FA's FMS screens (DepartureScreen, ArrivalScreen, EnrouteScreen) cache
    // the plan state in a companion-object (static) field. When this mod writes
    // directly to the live FlightPlanComputer that cache becomes stale:
    //   • The "Discard Changes" button lights up (screen state ≠ live data).
    //   • Worse: DepartureScreen/ArrivalScreen call state.save(computers.plan) on
    //     close, which writes the OLD cached data back and wipes our changes.
    // Calling companion.reload(newData) immediately after each write keeps the
    // cache in sync so the FMS screens open cleanly without any prompt.

    /**
     * Syncs the DepartureScreen companion-object state with {@code departureData}.
     * <p>Equivalent to calling {@code DepartureScreen.Companion.reload(departureData)}.</p>
     */
    private static void reloadFMSDepartureScreen(Object departureData) {
        try {
            Class<?> depDataClass = Class.forName(CLASS_DEPARTURE_DATA);
            reloadFMSCompanion(CLASS_DEPARTURE_SCREEN, departureData, depDataClass);
        } catch (Exception e) {
            LOGGER.debug("[FACompat] reloadFMSDepartureScreen failed: {}", e.getMessage());
        }
    }

    /**
     * Syncs the ArrivalScreen companion-object state with {@code arrivalData}.
     * <p>Equivalent to calling {@code ArrivalScreen.Companion.reload(arrivalData)}.</p>
     */
    private static void reloadFMSArrivalScreen(Object arrivalData) {
        try {
            Class<?> arrDataClass = Class.forName(CLASS_ARRIVAL_DATA);
            reloadFMSCompanion(CLASS_ARRIVAL_SCREEN, arrivalData, arrDataClass);
        } catch (Exception e) {
            LOGGER.debug("[FACompat] reloadFMSArrivalScreen failed: {}", e.getMessage());
        }
    }

    /**
     * Syncs the EnrouteScreen companion-object state with the current
     * {@code enrouteData} list.
     * <p>Equivalent to calling {@code EnrouteScreen.Companion.reload(enrouteData)}.</p>
     */
    private static void reloadFMSEnrouteScreen(List<Object> enrouteData) {
        try {
            Class<?> screenClass = Class.forName(CLASS_ENROUTE_SCREEN);
            Field companionField = screenClass.getDeclaredField("Companion");
            companionField.setAccessible(true);
            Object companion = companionField.get(null);
            // Generic erasure: JVM sees List, not List<EnrouteWaypoint>
            Method reload = companion.getClass().getMethod("reload", List.class);
            reload.invoke(companion, enrouteData);
        } catch (Exception e) {
            LOGGER.debug("[FACompat] reloadFMSEnrouteScreen failed: {}", e.getMessage());
        }
    }

    /** Calls the {@code reload(data)} method on the Kotlin companion object of the named screen. */
    private static void reloadFMSCompanion(String screenClassName, Object data, Class<?> dataClass)
            throws Exception {
        Class<?> screenClass = Class.forName(screenClassName);
        Field companionField = screenClass.getDeclaredField("Companion");
        companionField.setAccessible(true);
        Object companion = companionField.get(null);
        Method reload = companion.getClass().getMethod("reload", dataClass);
        reload.invoke(companion, data);
    }

    /**
     * Clears {@code AutoFlightComputer.selectedLateralMode} to {@code null} so that
     * FA's own {@code FlightPlanComputer.getLateralMode()} drives navigation.
     *
     * <p>FA resolves the active lateral mode as
     * {@code selectedLateralMode ?: computers.plan.getLateralMode()}.
     * If a previous "Fly Here" or "Set as COORDS Target" action left a static
     * {@code DirectCoordinatesLateralMode} in {@code selectedLateralMode}, it would
     * permanently override the flight-plan mode even after FA advances to the next
     * waypoint. Clearing it here hands control back to FA's sequencing logic.</p>
     *
     * <p>Verified against FA 3.0.1: Kotlin {@code var selectedLateralMode: LateralMode?}
     * → JVM setter {@code setSelectedLateralMode(LateralMode)}.</p>
     */
    private static void clearSelectedLateralMode() {
        Object afc = getAutoFlightComputer();
        if (afc == null) return;
        try {
            Method setter = findMethodByName(afc.getClass(), "setSelectedLateralMode");
            if (setter == null) {
                LOGGER.debug("[FACompat] clearSelectedLateralMode: setSelectedLateralMode not found");
                return;
            }
            setter.setAccessible(true);
            setter.invoke(afc, (Object) null);
        } catch (Exception e) {
            LOGGER.debug("[FACompat] clearSelectedLateralMode failed: {}", e.getMessage());
        }
    }

    /** Enables FA autopilot so selected modes affect heading/pitch outputs. */
    private static boolean enableAutoPilot(Object autoFlightComputer) {
        if (autoFlightComputer == null) return false;
        try {
            Method setAutoPilot = findMethod(autoFlightComputer.getClass(), "setAutoPilot", boolean.class, Boolean.class);
            if (setAutoPilot == null) {
                LOGGER.warn("[FACompat] setAutoPilot(boolean, Boolean) not found");
                return false;
            }
            setAutoPilot.setAccessible(true);
            setAutoPilot.invoke(autoFlightComputer, true, null);
            return true;
        } catch (Exception e) {
            LOGGER.warn("[FACompat] enableAutoPilot failed: {}", e.getMessage());
            return false;
        }
    }

    /** Returns true if any enroute waypoint is currently marked ORIGIN/TARGET. */
    private static boolean hasAnyActiveWaypoint(List<Object> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) return false;
        for (Object waypoint : waypoints) {
            if (waypoint == null) continue;
            try {
                Method getter = findMethodByName(waypoint.getClass(), "getActive");
                if (getter == null) continue;
                getter.setAccessible(true);
                if (getter.invoke(waypoint) != null) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn("[FACompat] hasAnyActiveWaypoint failed: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    /** Sets the mutable {@code active} property on an enroute waypoint instance. */
    private static void setWaypointActive(Object waypoint, Object active) throws Exception {
        if (waypoint == null) return;
        Method setter = findMethodByName(waypoint.getClass(), "setActive");
        if (setter != null) {
            setter.setAccessible(true);
            setter.invoke(waypoint, active);
        }
    }

    /** Finds an enum constant by name, returning null if unavailable. */
    private static Object getEnumConstant(Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum()) return null;
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) return null;
        for (Object c : constants) {
            if (c instanceof Enum<?> e && name.equals(e.name())) {
                return c;
            }
        }
        return null;
    }

    /** Searches class hierarchy for a method with an exact name/signature match. */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException ignored) {
                // Continue searching
            }
            for (Class<?> iface : current.getInterfaces()) {
                try {
                    return iface.getDeclaredMethod(name, paramTypes);
                } catch (NoSuchMethodException ignored) {
                    // Continue searching
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /** Searches a class (and supertypes) for a method matching the given name. */
    private static Method findMethodByName(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            for (Class<?> iface : current.getInterfaces()) {
                for (Method m : iface.getDeclaredMethods()) {
                    if (m.getName().equals(name)) return m;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    // =========================================================================
    // Chat helpers
    // =========================================================================

    /** Sends a prefixed message to the local player's chat HUD. */
    public static void sendChatMessage(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(CHAT_PREFIX + text));
        }
    }
}
