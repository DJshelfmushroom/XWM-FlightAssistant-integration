# Usage Guide

This document explains how to use every feature of **FlightAssistantXaeroCompat**
once it is installed in your Minecraft client.

---

## Table of Contents

1. [Installation](#1-installation)
2. [Opening the World Map](#2-opening-the-world-map)
3. [Feature: Set as COORDS Target](#3-feature-set-as-coords-target)
4. [Feature: Add to Flight Plan (Enroute)](#4-feature-add-to-flight-plan-enroute)
   - [Waypoint Altitude Screen](#waypoint-altitude-screen)
5. [Feature: Fly Here](#5-feature-fly-here)
6. [Feature: Flight Plan Overlay](#6-feature-flight-plan-overlay)
   - [Symbol Key](#symbol-key)
7. [Chat Message Reference](#7-chat-message-reference)
8. [Edge Cases & Warnings](#8-edge-cases--warnings)

---

## 1. Installation

Place all three jars in your Minecraft `mods/` folder:

| Jar                                              | Where to get it                                                                                      |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `flightassistant_xaero_compat-1.0.0.jar`         | Built from source — see [COMPILATION.md](COMPILATION.md), or download from the releases page        |
| `XaeroWorldMap_1.39.2_Forge_1.20.1.jar`          | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) or [Modrinth](https://modrinth.com/mod/xaeros-world-map) |
| `FlightAssistant-3.0.1+1.20.1-forge.jar`         | [GitHub Releases](https://github.com/Octol1ttle/FlightAssistant/releases/tag/3.0.1)                 |

Also ensure you are running **Minecraft 1.20.1** with **Forge 47.4.10** or a
compatible 47.x build.

> **Client-only mod:** Do **not** install this mod on a dedicated server.
> It is purely client-side and has no server-side component.

---

## 2. Opening the World Map

Open Xaero's World Map with the default key bind **`M`** (configurable in
Options → Controls). All FlightAssistantXaeroCompat features are accessed
from within this screen.

---

## 3. Feature: Set as COORDS Target

**Purpose:** Instantly point FlightAssistant's autopilot at an existing Xaero
waypoint without changing altitude or thrust.

### Steps

1. Open the World Map (`M`).
2. Find a waypoint you want to fly toward.
3. **Right-click** the waypoint icon.
4. In the context menu that appears, click **`[FA] Set as COORDS Target`**.

FlightAssistant's lateral autopilot mode switches to `COORDS` and locks onto
the waypoint's X/Z. A confirmation appears in chat:

```
[FlightAssistant] COORDS set to X: 1024, Z: -512
```

> **What changes vs. what doesn't:**
> - ✅ **Lateral mode** is set to `COORDS` targeting the waypoint X/Z.
> - ❌ **Vertical mode** (altitude hold, V/S, etc.) is **not** changed.
> - ❌ **Thrust mode** (autothrottle) is **not** changed.
>
> If the autopilot is already engaged, the change takes effect immediately and
> the aircraft will begin turning toward the new target.

---

## 4. Feature: Add to Flight Plan (Enroute)

**Purpose:** Append an existing Xaero waypoint to FlightAssistant's enroute
waypoint list, including a target altitude and optional speed.

### Steps

1. Open the World Map (`M`).
2. Right-click a waypoint.
3. Click **`[FA] Add to Flight Plan (Enroute)`** in the context menu.
4. The [Waypoint Altitude Screen](#waypoint-altitude-screen) opens.
5. Enter a target altitude and (optionally) a target speed, then click
   **Confirm**.

The waypoint is appended to the **end** of FlightAssistant's enroute list.
A confirmation appears in chat:

```
[FlightAssistant] Waypoint added: X: 1024, Z: -512, ALT: 128
```

> **Mid-flight safety:** If the autopilot is active in F/PLAN mode, the
> waypoint is always appended to the end of the list. The currently targeted
> waypoint is never changed, so navigation is not disrupted.

### Waypoint Altitude Screen

A small vanilla-styled dialog opens with two fields:

```
┌────────────────────────────────────┐
│  Add Enroute WP: MyWaypoint        │
│           X: 1024  Z: -512         │
│                                    │
│  Alt (Y): [____________] required  │
│  Speed:   [____________] optional  │
│                                    │
│  [   Confirm   ] [   Cancel   ]    │
└────────────────────────────────────┘
```

| Field        | Required | Accepts  | Notes                                           |
|--------------|----------|----------|-------------------------------------------------|
| **Alt (Y)**  | ✅ Yes   | Integer  | Target altitude in blocks (world Y coordinate). The field starts blank — you must enter a value. |
| **Speed**    | ❌ No    | Integer  | Target speed. Leave blank to use FlightAssistant's default speed for that segment. |

**Keyboard shortcuts:**

| Key            | Action                                     |
|----------------|--------------------------------------------|
| `Tab`          | Move focus between fields                  |
| `Enter`        | Confirm (only if Alt field is filled)      |
| `Escape`       | Cancel — no waypoint is added              |

---

## 5. Feature: Fly Here

**Purpose:** Quickly direct the autopilot to any arbitrary map position with
a single right-click, without needing a pre-existing waypoint.

### Steps

1. Open the World Map (`M`).
2. Right-click any **empty area** of the map (not on a waypoint).
3. Click **`Fly Here (FlightAssistant)`** in the context menu.

FlightAssistant's lateral mode is set to `COORDS` targeting the clicked X/Z.
A confirmation appears in chat:

```
[FlightAssistant] Flying to X: 256, Z: 800
```

> The autopilot will orbit the target coordinates in `COORDS` mode.
> It does not perform an automatic landing. You must switch to an approach/
> landing procedure manually.

---

## 6. Feature: Flight Plan Overlay

When Xaero's World Map is open and FlightAssistant has an active flight plan,
a graphical overlay is rendered directly on top of the map showing the current
plan.

### Symbol Key

| Symbol                   | Colour        | Meaning                                                            |
|--------------------------|---------------|--------------------------------------------------------------------|
| Filled square            | 🟢 Green      | **Departure** — the departure X/Z position                         |
| Filled square + label    | 🔵 Cyan       | **Enroute waypoint** — labelled `{index}/{altitude}` (e.g. `2/128`) |
| Filled square + label    | 🟡 Yellow     | **Active enroute waypoint** — the one FA is currently navigating toward |
| Filled square            | 🔴 Red        | **Arrival** — the arrival X/Z position                             |
| Line                     | Cyan (50% opacity) | **Route line** — connects Departure → Enroute WPs → Arrival in order |

Each marker has a thin white outline to distinguish it from Xaero's own
waypoint icons.

### How It Updates

The overlay polls FlightAssistant's state every render frame. Changes to the
flight plan (adding/removing waypoints, switching the active waypoint) are
reflected in the overlay the next time you open or look at the map — there is
no manual refresh needed.

### When the Overlay Is Hidden

- If the flight plan is completely empty (no departure, no enroute waypoints,
  no arrival), nothing is drawn.
- If either FlightAssistant or Xaero's World Map is not loaded, the overlay
  is silently disabled.

---

## 7. Chat Message Reference

All messages from this mod are prefixed with **`[FlightAssistant]`** in grey,
with the data in white.

| Trigger                              | Message                                                         |
|--------------------------------------|-----------------------------------------------------------------|
| Set as COORDS Target                 | `[FlightAssistant] COORDS set to X: {x}, Z: {z}`               |
| Add to Flight Plan (confirmed)       | `[FlightAssistant] Waypoint added: X: {x}, Z: {z}, ALT: {alt}` |
| Fly Here                             | `[FlightAssistant] Flying to X: {x}, Z: {z}`                   |
| Failed to set COORDS target          | `[FlightAssistant] Failed to set COORDS target — check logs.`   |
| Failed to add waypoint               | `[FlightAssistant] Failed to add waypoint — check logs.`        |

---

## 8. Edge Cases & Warnings

### Missing mod

- If **FlightAssistant** is not installed, all autopilot context-menu entries
  and the flight-plan overlay are silently disabled. The mod will not crash.
- If **Xaero's World Map** is not installed, all map features are disabled.
  The mod will not crash.

### Null / invalid coordinates

If a waypoint's coordinates cannot be read (null or a sentinel value), the
operation is aborted and a warning is written to the log. No chat message is
shown and no state is changed.

### Altitude field left blank

The **Confirm** button in the Waypoint Altitude Screen does nothing if the
altitude field is empty. Focus returns to the altitude field. You must enter
a value before confirming.

---

*Back to [README](../README.md)*
