# libs/

This directory holds optional local copies of the compile-only mod JARs.
Placing the correct file here lets you build **without network access** to
Xaero's Maven or Modrinth.

The `build.gradle` automatically prefers a file in this folder over the
remote Maven coordinate when it is present.

## Required file names

| File | Where to get it |
|------|----------------|
| `XaeroWorldMap-1.39.2_Forge_1.20.1.jar` | [Xaero's World Map releases](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map/files) — download the **1.39.2 (Forge 1.20.1)** file |
| `FlightAssistant-3.0.1.jar` | [FlightAssistant GitHub Releases](https://github.com/Octol1ttle/FlightAssistant/releases/tag/3.0.1) — download the Forge jar |

> **Note:** These files are listed in `.gitignore` and will **not** be
> committed to the repository.

## What cannot be bundled

Gradle itself, the ForgeGradle build plugin, and the Minecraft/Forge game
data are downloaded automatically on the first build and cached in
`~/.gradle/`. They are hundreds of megabytes in size (and some are
proprietary), so they cannot be shipped inside this repository.
A one-time internet connection is always required for a fresh machine.
