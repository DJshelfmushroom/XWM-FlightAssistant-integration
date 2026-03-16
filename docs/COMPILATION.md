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

| Tool        | Version         | Notes                                           |
|-------------|-----------------|-------------------------------------------------|
| **Java**    | 17 – 25         | Must be on `PATH`; JDK (not JRE) required. If JDK 17 is missing, Gradle auto-downloads a matching toolchain. |
| **Gradle**  | 8.14.4          | Use the included wrapper (`./gradlew`) — do **not** install a separate Gradle |
| **Network** | —               | Required on first build to download Forge, mappings, Xaero, and FlightAssistant |

Verify your Java installation:

```bash
java -version
# Expected output contains: openjdk 17  (or higher, up to Java 25)
```

> **Java version note:** The Gradle wrapper bundled with this project (8.14.4)
> supports Java 17 through Java 25. If you are using a newer JDK, update the
> `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` to the latest
> Gradle 8.x release.

> **Toolchain note:** The project compiles against Java 17. If only Java 21/25
> is installed locally, Gradle will automatically provision a Java 17 toolchain
> via Foojay on first build.

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

The two compile-only dependencies (Xaero's World Map and FlightAssistant) are
each resolved in order of preference:

1. **Local jar** in the `libs/` directory — no network needed at all.
2. **Remote Maven** — downloaded automatically if no local jar is present.

The `libs/` directory already exists in the repository. JAR files placed there
are listed in `.gitignore` and will never be accidentally committed.

> **Why can't everything be bundled?**
> Gradle itself, the ForgeGradle build plugin, and the Minecraft/Forge game
> data are downloaded on the first build and cached in `~/.gradle/`. They are
> hundreds of megabytes in size (and the Minecraft assets are proprietary), so
> they cannot be shipped inside this repository. A one-time internet connection
> is always required on a fresh machine.

### Xaero's World Map — automatic (default)

By default, Xaero's World Map is resolved from
[Modrinth's Maven mirror](https://api.modrinth.com/maven) using:

```text
maven.modrinth:xaeros-world-map:1.39.2_Forge_1.20
```

No manual action needed.

### Xaero's World Map — local jar (fallback)

If Modrinth Maven is unreachable, place the JAR in `libs/` and the build will
use it automatically:

1. Download **XaeroWorldMap-1.39.2_Forge_1.20.1.jar** from the
   [Xaero's World Map CurseForge page](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map/files)
   (filter by **Forge 1.20.1**, pick version **1.39.2**) or directly from
   Xaero's Maven:
   ```
   https://maven.xeadmc.net/xaero/public/maven/XaeroWorldMap/1.39.2_Forge_1.20.1/XaeroWorldMap-1.39.2_Forge_1.20.1.jar
   ```

2. Place the downloaded file in the `libs/` folder:
   ```
   XWM-FlightAssistant-integration/
   └── libs/
       └── XaeroWorldMap-1.39.2_Forge_1.20.1.jar   ← place the jar here
   ```

3. `build.gradle` automatically detects the file and skips the remote Maven
   lookup:
   ```groovy
    def localXaeroJar = file("libs/XaeroWorldMap-${project.xaero_worldmap_version}.jar")
    if (localXaeroJar.exists()) {
        compileOnly fg.deobf(files(localXaeroJar))
    } else {
        compileOnly fg.deobf("maven.modrinth:xaeros-world-map:${project.xaero_worldmap_modrinth_version}") {
            transitive = false
        }
    }
    ```

### FlightAssistant — automatic (default)

By default, FlightAssistant is resolved from
[Modrinth's Maven mirror](https://api.modrinth.com/maven). No manual action
needed. If the Modrinth Maven is unavailable or the artifact is unlisted, use
the local-jar fallback below.

### FlightAssistant — local jar (fallback)

1. Download **FlightAssistant-3.0.1.jar** (the Forge build) from the
   [GitHub Releases page](https://github.com/Octol1ttle/FlightAssistant/releases/tag/3.0.1).

2. Place the downloaded file in the `libs/` folder:

   ```
   XWM-FlightAssistant-integration/
   └── libs/
       └── FlightAssistant-3.0.1.jar   ← place the jar here
   ```

3. `build.gradle` automatically detects the file and skips the remote Maven
   lookup:

   ```groovy
   def localFAJar = file("libs/FlightAssistant-${project.flightassistant_version}.jar")
   if (localFAJar.exists()) {
       compileOnly fg.deobf(files(localFAJar))
   } else {
       compileOnly fg.deobf("maven.modrinth:flightassistant:${project.flightassistant_version}+1.20.1-forge") {
           transitive = false
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

### `Could not resolve maven.modrinth:xaeros-world-map:1.39.2_Forge_1.20`

The Modrinth Maven (`https://api.modrinth.com/maven`) was unreachable during
the build. Check your network connection or VPN, then retry with:

```bash
./gradlew build --refresh-dependencies
```

If the issue persists, use the [local jar fallback](#xaeros-world-map--local-jar-fallback).

### `Could not resolve maven.modrinth:flightassistant:3.0.1+1.20.1-forge`

The Modrinth Maven could not find the artifact. Use the
[local jar fallback](#flightassistant--local-jar-fallback) instead.

### `java.lang.UnsupportedClassVersionError` or `Unsupported class file major version` during build

Your JDK version is not supported by the Gradle wrapper bundled with this project.

- `Unsupported class file major version N` in the **Gradle startup phase** means your JDK is
  **newer** than what this Gradle version understands. Update the wrapper by changing
  `gradle/wrapper/gradle-wrapper.properties` to point to the latest Gradle 8.x release:

  ```properties
  distributionUrl=https\://services.gradle.org/distributions/gradle-<VERSION>-bin.zip
  ```

  Replace `<VERSION>` with the latest release from
  <https://gradle.org/releases/>.

- `java.lang.UnsupportedClassVersionError` **during compilation** means your JDK is
  **older** than 17. Install JDK 17 and ensure `JAVA_HOME` points to it:

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
