# Developer & Porting Guide

This document describes the internal architecture of
**FlightAssistantXaeroCompat** and explains how to update or port the mod when
either FlightAssistant or Xaero's World Map releases a new version.

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Dependency Isolation Strategy](#2-dependency-isolation-strategy)
3. [FlightAssistantCompat — Reflection Layer](#3-flightassistantcompat--reflection-layer)
   - [Class-name constants](#class-name-constants)
   - [Key methods](#key-methods)
   - [Porting to a new FA version](#porting-to-a-new-fa-version)
4. [XaeroCompat — Xaero Reflection Layer](#4-xaerocompat--xaero-reflection-layer)
   - [Porting to a new Xaero version](#porting-to-a-new-xaero-version)
5. [MixinGuiMap — Context Menu Injection](#5-mixinguimap--context-menu-injection)
6. [WaypointContextMenuHandler — Forge Event Listener](#6-waypointcontextmenuhandler--forge-event-listener)
7. [FlightPlanMapOverlay — Overlay Renderer](#7-flightplanmapoverlay--overlay-renderer)
8. [WaypointAltitudeScreen — Altitude GUI](#8-waypointaltitudescreen--altitude-gui)
9. [Adding a New Feature](#9-adding-a-new-feature)
10. [Versioning & Release Checklist](#10-versioning--release-checklist)

---

## 1. Project Structure

```
src/main/
├── java/dev/djshelfmushroom/flightassistantxaerocompat/
│   ├── FlightAssistantXaeroCompat.java      # @Mod entry point; feature guard flags
│   ├── compat/
│   │   ├── FlightAssistantCompat.java       # ALL FA API / reflection access
│   │   └── XaeroCompat.java                 # ALL Xaero reflection helpers
│   ├── events/
│   │   └── WaypointContextMenuHandler.java  # ScreenEvent.Render.Post → overlay
│   ├── gui/
│   │   └── WaypointAltitudeScreen.java      # Altitude / speed input screen
│   ├── map/
│   │   └── FlightPlanMapOverlay.java        # Overlay renderer
│   └── mixin/
│       └── MixinGuiMap.java                 # Xaero GuiMap context-menu mixin
└── resources/
    ├── META-INF/mods.toml                   # Forge mod metadata & dependencies
    ├── pack.mcmeta                          # Resource-pack descriptor
    └── flightassistant_xaero_compat.mixins.json  # Mixin config
```

---

## 2. Dependency Isolation Strategy

The mod has two hard runtime dependencies — FlightAssistant (FA) and Xaero's
World Map (Xaero). Either dependency may be absent at runtime. The mod handles
this with two boolean flags set during `FMLClientSetupEvent`:

```java
// FlightAssistantXaeroCompat.java
public static boolean xaeroPresent         = false;
public static boolean flightAssistantPresent = false;
```

Every feature that touches FA checks `flightAssistantPresent` first.  
Every feature that touches Xaero checks `xaeroPresent` first.

**Hard class references to either dependency are forbidden in any class that
is not inside `compat/` or `mixin/`.** All other code accesses the dependency
only through the `FlightAssistantCompat` and `XaeroCompat` façades, which are
safe to call even if the dependency is missing (they return `null`/`false`).

The one exception is `MixinGuiMap`, which has direct compile-time imports of
Xaero classes — this is unavoidable for a Mixin. If Xaero is absent the mixin
simply never applies.

---

## 3. FlightAssistantCompat — Reflection Layer

`FlightAssistantCompat.java` is the **single file** that must be updated when
FlightAssistant releases a new version. Every reflected class name, field name,
and method name is documented with a comment that says which FA version it was
verified against.

### Class-name constants

```java
// All FQN strings are at the top of the file.
private static final String CLASS_COMPUTER_HOST =
    "ru.octol1ttle.flightassistant.impl.computer.ComputerHost";
private static final String CLASS_AUTO_FLIGHT_COMPUTER =
    "ru.octol1ttle.flightassistant.impl.computer.autoflight.AutoFlightComputer";
private static final String CLASS_FLIGHT_PLAN_COMPUTER =
    "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer";
private static final String CLASS_DIRECT_COORDS_LATERAL =
    "ru.octol1ttle.flightassistant.impl.computer.autoflight.modes.DirectCoordinatesLateralMode";
private static final String CLASS_ENROUTE_WAYPOINT =
    "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$EnrouteWaypoint";
private static final String CLASS_ENROUTE_ACTIVE =
    "ru.octol1ttle.flightassistant.impl.computer.autoflight.FlightPlanComputer$EnrouteWaypoint$Active";
```

### Key methods

| Method | What it does |
|--------|--------------|
| `getAutoFlightComputer()` | Fetches the `AutoFlightComputer` singleton from `ComputerHost` |
| `getFlightPlanComputer()` | Fetches the `FlightPlanComputer` singleton |
| `isAutopilotEngaged()` | Reads `AutoFlightComputer.getAutopilot()` |
| `setCoordsTarget(x, z)` | Constructs a `DirectCoordinatesLateralMode` and calls `setSelectedLateralMode()` |
| `getEnrouteWaypoints()` | Returns the live `MutableList<EnrouteWaypoint>` from `FlightPlanComputer` |
| `addEnrouteWaypoint(x, z, alt, speed)` | Constructs an `EnrouteWaypoint` and appends it to the live list |
| `getDepartureData()` / `getArrivalData()` | Returns departure / arrival data objects |
| `getPlanCoordinatesX/Z(obj)` | Reads `coordinatesX`/`coordinatesZ` from any plan data object |
| `getEnrouteAltitude(obj)` | Reads `altitude` from an `EnrouteWaypoint` |
| `getActiveWaypointIndex()` | Scans the enroute list for the waypoint with `active == Active.TARGET` |
| `isPlanDataDefault(obj)` | Calls `isDefault()` on departure or arrival data |

### Porting to a new FA version

1. Open the new FA jar (or source) in a decompiler / IDE.
2. Check that the FQN strings in `CLASS_*` constants still match.
3. Check each reflected getter/setter name used in the methods above.
4. If `EnrouteWaypoint`'s constructor signature changed, update the
   `addEnrouteWaypoint` method (both the primary call and the Kotlin
   default-parameter fallback).
5. Update `gradle.properties`:
   ```properties
   flightassistant_version=<new_version>
   ```
6. Update `mods.toml` version range for the `flightassistant` dependency.
7. Run `./gradlew build` and verify the mod compiles and runs correctly.

---

## 4. XaeroCompat — Xaero Reflection Layer

`XaeroCompat.java` isolates all Xaero-specific reflection. It currently
provides:

| Method | What it does |
|--------|--------------|
| `getWaypointX/Y/Z(waypoint)` | Reads coordinates from a `Waypoint` object |
| `getWaypointName(waypoint)` | Reads the display name |
| `getWaypointDimension(waypoint)` | Reads the dimension string (if available) |
| `getGuiMapCameraX/Z(guiMap)` | Reads `cameraX`/`cameraZ` fields for overlay rendering |
| `getGuiMapScale(guiMap)` | Reads the `scale` (pixels-per-block) field |

All methods use a method-first, field-fallback pattern and walk the class
hierarchy to handle obfuscated or renamed members.

### Porting to a new Xaero version

1. Decompile the new `XaeroWorldMap` jar and check:
   - Does `Waypoint` still have `getX()`, `getY()`, `getZ()`, `getName()`?
   - Does `GuiMap` still have `cameraX`, `cameraZ`, `scale` fields?
   - Has `GuiMap.getRightClickOptions()` or `IRightClickableElement` moved?
2. Update field/method names in `XaeroCompat.java` if they changed.
3. Update `MixinGuiMap.java` if `GuiMap`'s structure changed:
   - Check the `@Shadow` field names (`rightClickX`, `rightClickZ`).
   - Check the `@Inject` target method name and descriptor.
4. Update `gradle.properties`:
   ```properties
   xaero_worldmap_version=<new_version>_Forge_1.20.1
   ```
5. Update `mods.toml` version range for `xaeroworldmap`.

---

## 5. MixinGuiMap — Context Menu Injection

`MixinGuiMap` injects into `xaero.map.gui.GuiMap.getRightClickOptions()` using
SpongePowered Mixin. It appends three `RightClickOption` entries to the list
returned by the vanilla Xaero implementation.

```
@Inject(
    method = "getRightClickOptions",
    at = @At("RETURN"),
    remap = false         // Xaero classes are NOT remapped by ForgeGradle
)
private void injectFAOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir)
```

Key design decisions:
- `remap = false` is critical because Xaero ships its own non-remapped classes.
- The injected actions delegate entirely to `FlightAssistantCompat` so that
  the mixin itself contains no FA-specific logic.
- Coordinate validation (`Integer.MAX_VALUE` sentinel check) prevents acting
  on Xaero's uninitialized right-click coordinate state.

---

## 6. WaypointContextMenuHandler — Forge Event Listener

`WaypointContextMenuHandler` listens to `ScreenEvent.Render.Post` on Forge's
main event bus. When the active screen is `xaero.map.gui.GuiMap`, it delegates
rendering to `FlightPlanMapOverlay`.

The class detection uses a cached `Class<?>` reference to avoid calling
`Class.forName` every render frame:

```java
private static Class<?> guiMapClass;           // cached
private static boolean  guiMapClassLoadFailed; // set on ClassNotFoundException
```

---

## 7. FlightPlanMapOverlay — Overlay Renderer

`FlightPlanMapOverlay.render(GuiGraphics, Screen)` is called every render frame
when GuiMap is open. It:

1. Fetches departure, enroute, and arrival data from `FlightAssistantCompat`.
2. Reads `cameraX`, `cameraZ`, and `scale` from the GuiMap instance via
   `XaeroCompat`.
3. Converts each world coordinate to a screen position using:
   ```java
   screenX = (screenWidth  / 2.0) + (worldX - cameraX) * scale;
   screenY = (screenHeight / 2.0) + (worldZ - cameraZ) * scale;
   ```
4. Draws route lines with `Tesselator` + `POSITION_COLOR` vertex format in
   `DEBUG_LINES` mode.
5. Draws filled square markers with `GuiGraphics.fill`.
6. Draws text labels with `GuiGraphics.drawString`.

No Xaero waypoint list is modified. The overlay is purely additive.

---

## 8. WaypointAltitudeScreen — Altitude GUI

`WaypointAltitudeScreen` extends `net.minecraft.client.gui.screens.Screen`.
It follows vanilla 1.20.1 patterns:

- `init()` creates `EditBox` and `Button` widgets via `addRenderableWidget`.
- `render()` draws a translucent background panel, title, and field labels,
  then calls `super.render()` to draw the widgets.
- `keyPressed()` maps Enter → confirm, Escape → cancel.
- `isPauseScreen()` returns `false` (the world continues ticking while open).

The altitude field starts empty (no placeholder number) to force the player
to make an explicit choice.

---

## 9. Adding a New Feature

### Feature that only touches FA state

1. Add a `public static` method to `FlightAssistantCompat.java`.
2. Call it from a new Forge event handler or from `MixinGuiMap`.

### Feature that adds a new context-menu entry

1. Add a new `options.add(new RightClickOption(...) { ... })` block in
   `MixinGuiMap.injectFAOptions`.
2. Implement the action in `FlightAssistantCompat` if it touches FA, or
   inline it if it is purely Xaero-side.

### Feature that needs a new screen

1. Create a class in `gui/` extending `Screen`.
2. Open it with `Minecraft.getInstance().setScreen(new YourScreen(...))`.

### Feature that modifies the overlay

1. All overlay code lives in `FlightPlanMapOverlay`. Add new draw calls to
   `render()`.

---

## 10. Versioning & Release Checklist

Before tagging a release:

- [ ] `gradle.properties` — bump `mod_version`.
- [ ] `mods.toml` — verify dependency version ranges still match actual dependency versions.
- [ ] All reflected class/method names verified against the target FA and Xaero versions.
- [ ] `./gradlew build` produces no warnings or errors.
- [ ] `./gradlew runClient` — test each feature in-game:
  - [ ] Right-click a waypoint → both menu entries appear.
  - [ ] Set as COORDS Target → chat confirmation + autopilot turns.
  - [ ] Add to Flight Plan → altitude screen opens, waypoint added.
  - [ ] Fly Here → chat confirmation + autopilot turns.
  - [ ] Flight plan overlay visible with departure/enroute/arrival markers.
  - [ ] Active waypoint highlighted in yellow.
- [ ] Tag the release: `git tag v<version>` and push.

---

*Back to [README](../README.md)*
