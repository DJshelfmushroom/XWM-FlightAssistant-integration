# FlightAssistantXaeroCompat

A purely **client-side** Forge 1.20.1 mod that integrates
[Xaero's World Map](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map) with
[FlightAssistant](https://github.com/Octol1ttle/FlightAssistant). It lets you set
autopilot targets and manage flight plans directly from the world map.

---

## Documentation

| Page | Contents |
|------|----------|
| 📖 **[COMPILATION.md](docs/COMPILATION.md)** | Prerequisites, step-by-step build instructions, IDE setup, troubleshooting |
| 🎮 **[USAGE.md](docs/USAGE.md)** | In-game feature guide: context menus, Fly Here, flight-plan overlay, chat messages |
| 🔧 **[DEVELOPMENT.md](docs/DEVELOPMENT.md)** | Architecture overview, porting guide, adding features, release checklist |

---

## Versions

| Dependency          | Version                       |
|---------------------|-------------------------------|
| Minecraft           | 1.20.1                        |
| Forge               | 47.4.10                       |
| Xaero's World Map   | 1.39.2 (Forge)                |
| FlightAssistant     | 3.0.1                         |

---

## Quick Start

```bash
# Clone
git clone https://github.com/DJshelfmushroom/XWM-FlightAssistant-integration.git
cd XWM-FlightAssistant-integration

# Build (requires Java 17 and internet access)
./gradlew build
# Output: build/libs/flightassistant_xaero_compat-1.0.0.jar
```

See **[COMPILATION.md](docs/COMPILATION.md)** for full build and dependency details,
including how to supply FlightAssistant as a local jar if the Modrinth Maven is
unavailable.

---

## Features at a Glance

- **Right-click any Xaero waypoint** → set it as the autopilot COORDS target, or
  add it to FlightAssistant's enroute flight plan with a custom altitude and speed.
- **Right-click anywhere on the map** → "Fly Here" — sets COORDS to the clicked
  position instantly.
- **Flight-plan overlay** — renders departure (green), enroute (cyan), active
  waypoint (yellow), and arrival (red) markers with a connecting route line on
  top of the world map.

All features degrade gracefully: if FlightAssistant or Xaero's World Map is not
installed, the affected features are silently disabled — no crash.

See **[USAGE.md](docs/USAGE.md)** for step-by-step instructions and screenshots of
every feature.

---

## Project Structure

```
src/main/java/dev/djshelfmushroom/flightassistantxaerocompat/
├── FlightAssistantXaeroCompat.java         # Main mod class & feature guard flags
├── compat/
│   ├── FlightAssistantCompat.java          # All FA API / reflection access
│   └── XaeroCompat.java                    # All Xaero API / reflection access
├── events/
│   └── WaypointContextMenuHandler.java     # ScreenEvent listener → overlay render
├── gui/
│   └── WaypointAltitudeScreen.java         # Altitude / speed input screen
├── map/
│   └── FlightPlanMapOverlay.java           # Custom map overlay renderer
└── mixin/
    └── MixinGuiMap.java                    # Xaero GuiMap context-menu injection
```

See **[DEVELOPMENT.md](docs/DEVELOPMENT.md)** for architecture details and a
porting guide.

---

## License

MIT — see [LICENSE](LICENSE) if present, otherwise consider the code freely reusable.
