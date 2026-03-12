# Compilation Guide

This document walks you through every step needed to build
**FlightAssistantXaeroCompat** from source.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Clone the Repository](#2-clone-the-repository)
3. [Resolve Dependencies](#3-resolve-dependencies)
   - [Xaero's World Map](#xaeros-world-map)
   - [FlightAssistant — Modrinth (automatic)](#flightassistant--modrinth-automatic)
   - [FlightAssistant — local jar (fallback)](#flightassistant--local-jar-fallback)
4. [Build the Jar](#4-build-the-jar)
5. [Run a Dev Client](#5-run-a-dev-client)
6. [IDE Setup](#6-ide-setup)
   - [IntelliJ IDEA](#intellij-idea)
   - [Eclipse](#eclipse)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

| Tool        | Minimum version | Notes                                           |
|-------------|-----------------|-------------------------------------------------|
| **Java**    | 17              | Must be on `PATH`; JDK (not JRE) required       |
| **Gradle**  | 8.1.1           | Use the included wrapper (`./gradlew`) — do **not** install a separate Gradle |
| **Network** | —               | Required on first build to download Forge, mappings, Xaero, and FlightAssistant |

Verify your Java installation:

```bash
java -version
# Expected output contains: openjdk 17  (or higher, within JDK 17 LTS)
```

> **Apple Silicon / ARM64 users:** Azul Zulu JDK 17 for arm64 is recommended.
> AdoptOpenJDK / Eclipse Temurin 17 also works.

---

## 2. Clone the Repository

```bash
git clone https://github.com/DJshelfmushroom/XWM-FlightAssistant-integration.git
cd XWM-FlightAssistant-integration
```

---

## 3. Resolve Dependencies

### Xaero's World Map

Resolved automatically from [Xaero's Maven](https://maven.xeadmc.net/).
No manual action needed.

Relevant `build.gradle` block:

```groovy
maven {
    name = "Xaero's Maven"
    url = "https://maven.xeadmc.net/"
}
// ...
compileOnly fg.deobf("xaero.public.maven:XaeroWorldMap:1.39.2_Forge_1.20.1")
```

### FlightAssistant — Modrinth (automatic)

By default, FlightAssistant is resolved from
[Modrinth's Maven mirror](https://api.modrinth.com/maven):

```groovy
maven {
    name = "Modrinth Maven"
    url = "https://api.modrinth.com/maven"
}
// ...
compileOnly fg.deobf("maven.modrinth:flightassistant:3.0.1+1.20.1-forge") {
    transitive = false
}
```

This requires no manual action. If the Modrinth Maven is unavailable or the
artifact is unlisted, use the local-jar fallback below.

### FlightAssistant — local jar (fallback)

1. Download **FlightAssistant-3.0.1.jar** (the Forge build) from the
   [GitHub Releases page](https://github.com/Octol1ttle/FlightAssistant/releases/tag/3.0.1).

2. Create a `libs/` directory in the project root (it is `.gitignore`-d so it
   will not be committed):

   ```
   XWM-FlightAssistant-integration/
   └── libs/
       └── FlightAssistant-3.0.1.jar   ← place the jar here
   ```

3. The `build.gradle` checks for this file at **configuration time** and
   automatically uses it instead of the Modrinth coordinate when present:

   ```groovy
   if (file("libs/FlightAssistant-${project.flightassistant_version}.jar").exists()) {
       dependencies {
           compileOnly fg.deobf(files("libs/FlightAssistant-3.0.1.jar"))
       }
   }
   ```

---

## 4. Build the Jar

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

ForgeGradle will:
1. Download the Forge installer and mappings (first run only, ~5 min).
2. Deobfuscate/remap the dependency jars.
3. Compile the mod source.
4. Apply the Mixin annotation processor.
5. Produce `build/libs/flightassistant_xaero_compat-1.0.0.jar` — the
   distributable, reobfuscated jar.

> **First-build note:** The initial Gradle run can take **5–15 minutes** while
> it downloads Forge, the official mappings, and all dependency jars.
> Subsequent builds are much faster (seconds).

### Output files

| Path                                                         | Description                             |
|--------------------------------------------------------------|-----------------------------------------|
| `build/libs/flightassistant_xaero_compat-1.0.0.jar`         | Distributable mod jar (use this one)    |
| `build/libs/flightassistant_xaero_compat-1.0.0-sources.jar` | Source jar (generated only if requested)|
| `build/reobf/jar/`                                           | Intermediate reobf output (internal)    |

---

## 5. Run a Dev Client

The `build.gradle` includes a Forge `client` run configuration. It launches a
Minecraft client with the mod loaded into a `run/` working directory.

```bash
./gradlew runClient
```

> For the integration features to activate you will also need the **runtime**
> jars for Xaero's World Map 1.39.2 and FlightAssistant 3.0.1 in the mods
> folder. Add them as `runtimeOnly` dependencies (they are `compileOnly` by
> default to avoid shipping them in the mod jar).
>
> Alternatively, place both jars in `run/mods/` before running.

---

## 6. IDE Setup

### IntelliJ IDEA

```bash
./gradlew genIntellijRuns
```

Then open the project as a Gradle project. The `runClient` run configuration
will appear automatically.

### Eclipse

```bash
./gradlew eclipse genEclipseRuns
```

Import the project as an existing Gradle project. A launch configuration named
`runClient` will be created.

---

## 7. Troubleshooting

### `Could not resolve xaero.public.maven:XaeroWorldMap:1.39.2_Forge_1.20.1`

The Xaero Maven (`https://maven.xeadmc.net/`) was unreachable during the
build. Check your network connection or VPN. The server is occasionally slow;
retry with:

```bash
./gradlew build --refresh-dependencies
```

### `Could not resolve maven.modrinth:flightassistant:3.0.1+1.20.1-forge`

The Modrinth Maven could not find the artifact. Use the
[local jar fallback](#flightassistant--local-jar-fallback) instead.

### `java.lang.UnsupportedClassVersionError` during build

Your JDK is older than 17. Install JDK 17 and ensure `JAVA_HOME` points to it:

```bash
# macOS / Linux
export JAVA_HOME=/path/to/jdk17
./gradlew build

# Windows (PowerShell)
$env:JAVA_HOME = "C:\path\to\jdk17"
.\gradlew.bat build
```

### Gradle runs out of memory

The `gradle.properties` already sets `-Xmx3G`. If your machine has less than
4 GB of available RAM, reduce the value:

```properties
org.gradle.jvmargs=-Xmx2G
```

### `MixinGuiMap` fails to apply at runtime

This mixin targets `xaero.map.gui.GuiMap`, which is a non-remapped Xaero
class. Ensure:

1. Xaero's World Map **1.39.2** (Forge) is installed, not a different version.
2. The mixin config `flightassistant_xaero_compat.mixins.json` is referenced
   in the jar manifest (`MixinConfigs` attribute) — this is set in `build.gradle`.

### Build succeeds but the mod jar is empty / missing classes

Run `./gradlew clean build` to clear the incremental build cache.

---

*Back to [README](../README.md)*
