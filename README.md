# FlightAssistantXaeroCompat

A purely **client-side** Forge 1.20.1 mod that integrates
[Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) with
[FlightAssistant](https://github.com/Octol1ttle/FlightAssistant) to let you set autopilot
targets and manage flight plans directly from the world map.

---

## Versions

| Dependency          | Version                       |
|---------------------|-------------------------------|
| Minecraft           | 1.20.1                        |
| Forge               | 47.4.10                       |
| Xaero's World Map   | 1.39.2 (Forge)                |
| FlightAssistant     | 3.0.1                         |

---

## Features

### 1. Right-click context menu on Xaero waypoints

When you right-click an existing waypoint on Xaero's World Map, two extra
entries appear in the context menu under a **FlightAssistant** section:

- **[FA] Set as COORDS Target** — sets the autopilot's lateral mode to
  `COORDS` targeting the waypoint's X/Z. Only the lateral mode is changed;
  thrust and vertical modes are untouched. A chat confirmation is shown:
  `[FlightAssistant] COORDS set to X: {x}, Z: {z}`

- **[FA] Add to Flight Plan (Enroute)** — opens the
  [Waypoint Altitude Screen](#waypoint-altitude-screen) to enter a target
  altitude (required) and optional speed, then appends the waypoint to
  FlightAssistant's enroute list. A chat confirmation is shown:
  `[FlightAssistant] Waypoint added: X: {x}, Z: {z}, ALT: {alt}`

### 2. "Fly Here" — right-click anywhere on the map

When you right-click an empty area of the world map, a **Fly Here
(FlightAssistant)** option appears. Selecting it sets the lateral mode to
`COORDS` targeting the clicked block position. A tooltip reminds you that
the autopilot will circle the target and will not land automatically.

### 3. Flight Plan Map Overlay

A custom overlay is rendered on top of Xaero's World Map screen whenever a
FlightAssistant flight plan is active:

- 🟢 **Green square** — Departure position
- 🔵 **Cyan squares** — Enroute waypoints, labelled `{index}/{altitude}`
- 🟡 **Yellow square** — The enroute waypoint currently being navigated toward
- 🔴 **Red square** — Arrival position
- **Cyan line** — Route connecting Departure → Enroute WPs → Arrival

The overlay polls FlightAssistant's state every render tick (no events required).

---

## Waypoint Altitude Screen

A small vanilla-styled GUI with two fields:

| Field    | Required | Description                       |
|----------|----------|-----------------------------------|
| Alt (Y)  | ✅ Yes   | Target altitude in blocks         |
| Speed    | ❌ No    | Target speed (blocks/tick or m/s) |

Press **Confirm** (or Enter) to add the waypoint.  
Press **Cancel** (or Escape) to abort without changing anything.

---

## Edge Cases

| Situation                                    | Behaviour                                                          |
|----------------------------------------------|--------------------------------------------------------------------|
| Waypoint in a different dimension            | Warning chat message; waypoint is not sent to FlightAssistant      |
| FlightAssistant not loaded                   | Autopilot features silently disabled; no crash                     |
| Xaero's World Map not loaded                 | Map features silently disabled; no crash                           |
| Null / invalid coordinates                   | Logged warning; operation is a no-op                               |
| Autopilot active in F/PLAN mode              | Enroute waypoint appended to the end without disrupting navigation |

---

## Build Instructions

### Prerequisites

- Java 17
- Gradle 8+ (the wrapper is included)
- Internet access to download Forge, Xaero, and FlightAssistant from their
  respective Maven/Modrinth repositories

### Building

```bash
# Clone the repository
git clone https://github.com/DJshelfmushroom/XWM-FlightAssistant-integration.git
cd XWM-FlightAssistant-integration

# Build the mod jar (the ForgeGradle reobf jar ends up in build/libs/)
./gradlew build
```

The distributable jar will be at `build/libs/flightassistant_xaero_compat-1.0.0.jar`.

### (Optional) Providing FlightAssistant as a local jar

If the Modrinth Maven coordinate fails to resolve FlightAssistant 3.0.1,
download the jar from the
[GitHub Releases page](https://github.com/Octol1ttle/FlightAssistant/releases/tag/3.0.1)
and place it at:

```
libs/FlightAssistant-3.0.1.jar
```

The `build.gradle` will automatically prefer the local jar if it is present.

### Running in a dev environment

```bash
./gradlew runClient
```

Both Xaero's World Map and FlightAssistant must be in the mods folder (or
declared as runtime dependencies) for the integration features to activate.

---

## Project Structure

```
src/main/java/dev/djshelfmushroom/flightassistantxaerocompat/
├── FlightAssistantXaeroCompat.java         # Main mod class & feature guard flags
├── compat/
│   ├── FlightAssistantCompat.java          # All FA API / reflection access
│   └── XaeroCompat.java                    # All Xaero API / reflection access
├── gui/
│   └── WaypointAltitudeScreen.java         # Altitude / speed prompt GUI
├── map/
│   └── FlightPlanMapOverlay.java           # Custom Xaero map overlay renderer
├── events/
│   └── WaypointContextMenuHandler.java     # ScreenEvent listener for the overlay
└── mixin/
    └── MixinGuiMap.java                    # Xaero GuiMap context-menu injection
```

---

## License

MIT
