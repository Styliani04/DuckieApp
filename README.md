# Duckie — Distributed Food Ordering Platform

A university team project for the **Distributed Systems** course at **Athens University of Economics and Business (2025)**. Duckie models food ordering and store management using a Java backend with TCP sockets, multiple worker processes, and MapReduce-style result aggregation.

The repository also includes an **Android UI prototype**. Its screens use local sample data and placeholder actions; they are not connected to the distributed backend in this version. Use the Java console applications to interact with the backend.

## Features

The backend source implements:

- Store registration from local JSON files.
- Store search by price category, food category, rating, and distance.
- Product purchases, stock updates, and store ratings.
- Manager operations for adding products, changing stock, hiding products, and requesting sales reports.
- Worker-side request processing and reducer-side aggregation of search results and sales counts.
- Thread-based connection handling, serialized request/response objects, and worker connection checks.

This is an academic prototype. The verification section below describes the flows tested for these instructions.

## Architecture

| Component | Responsibility |
| --- | --- |
| Master | Accepts client and manager connections, tracks stores and workers, routes requests, and starts local workers when needed. |
| Workers | Hold store data in memory and process requests. |
| Reducer | Merges search results and aggregates sales counts received through the Master. |
| Client console | Searches stores, purchases products, and submits ratings. |
| Manager console | Loads stores and manages products, stock, and sales queries. |
| Android prototype | Demonstrates customer and manager screens independently of the backend. |

Backend communication uses Java TCP sockets with `ObjectInputStream` and `ObjectOutputStream`. Request and model classes are shared through the `common` package.

## Repository structure

| Path | Contents |
| --- | --- |
| `backend/src/common/` | Shared models, requests, results, and connection metadata |
| `backend/src/master/` | Master server and connection handler |
| `backend/src/worker/` | Worker server and request processing |
| `backend/src/reducer/` | Reducer server and aggregation logic |
| `backend/src/client/` | Customer console application |
| `backend/src/manager/` | Manager console application |
| `backend/stores/` | Example store JSON files and images |
| `frontend/app/src/main/` | Android activities, layouts, resources, and manifest |
| `frontend/gradle/` | Gradle wrapper files and dependency version catalog |

## Run the Java backend locally

### Requirements

- **JDK 17**, with both `java` and `javac` available on `PATH`.
- Separate terminals for the Master, Reducer, Manager, and Client.
- Available ports: `9000` for the Master, `6500` for the Reducer, and worker ports starting at `6000`.

The backend source uses only Java standard-library imports. The commands below compile it directly; they do not use `backend/build.gradle`, which contains an Android plugin declaration rather than a Java backend build configuration.

### 1. Compile

From the repository root, enter the backend directory:

```shell
cd backend
```

Run this command from `backend/` in Windows PowerShell, Command Prompt, or a macOS/Linux shell:

```shell
javac -encoding UTF-8 --release 17 -d out/production/Duckie src/common/*.java src/master/*.java src/worker/*.java src/reducer/*.java src/client/*.java src/manager/*.java
```

**Keep this exact output path.** The Master currently uses `out/production/Duckie` as the classpath when launching worker processes. All runtime commands below must also run with `backend/` as the working directory.

There is no `AppLauncher.java` in this version. Start the components separately as follows.

### 2. Start the Master — terminal 1

```shell
java -cp out/production/Duckie master.MasterServer 127.0.0.1 9000
```

Wait for the listening message before continuing.

### 3. Start the Reducer — terminal 2

```shell
java -cp out/production/Duckie reducer.ReducerServer 127.0.0.1 9000 6500
```

Wait for registration with the Master and `Reducer listening on port 6500`.

### 4. Start the Manager and register a store — terminal 3

```shell
java -cp out/production/Duckie manager.ManagerApp 127.0.0.1 9000
```

Choose `1` (**Add Store**) and enter:

```text
Burgermania
```

This loads `backend/stores/Burgermania/store.json`. Enter the **folder name**, not the full JSON path. Other included examples are `CoffeeState`, `Greenhive`, `IndianCorner`, `Pastadoros`, `PizzaPath`, and `TisFroswsToKalamaki`.

Stores are not loaded automatically at startup. Register at least one before searching. The Master starts local worker processes as needed; no manual worker startup is required for this walkthrough.

### 5. Start the Client — terminal 4

```shell
java -cp out/production/Duckie client.ClientApp 127.0.0.1 9000
```

Choose `1` (**Search Store**) and follow the prompts for search filters, store selection, purchases, and ratings.

The console client currently supplies fixed coordinates near Athens and a 5 km search radius in `ClientApp.java`. Adjust those source values and recompile if you need a different search location.

### Stop and restart

Use menu option `0` to exit a console application and `Ctrl+C` to stop servers. Workers are separate Java processes; if any remain after stopping the Master, stop those specific worker processes before restarting. Runtime store state is held in memory; register stores again for a fresh session.

## Android UI prototype

The Android project is under `frontend/`. The checked-in configuration specifies Android SDK 35 for compilation/targeting, minimum SDK 24, Java 11 source compatibility, Android Gradle Plugin 8.10.1, and Gradle wrapper 8.11.1.

To attempt a local build:

1. Open **`frontend/`** in Android Studio as the project directory.
2. Configure a JDK 17 Gradle runtime and install Android SDK Platform 35.
3. Let Android Studio configure the SDK location for your machine and sync the Gradle project. Internet access is required to download missing dependencies.
4. Select the `app` configuration and an emulator or device with API 24 or newer, then run it.

An Android build and emulator run have **not** been verified as part of this README update. The interface includes role selection, search, cart, profile, and management screens, but displayed confirmation messages do not represent backend operations. Starting the Java servers does not connect these screens to them.

## Generated and machine-specific files

The following files are not required in version control:

- `.gradle/` — generated Gradle caches.
- `.idea/` and `*.iml` — local IDE configuration.
- `out/`, `build/`, and `*.class` — build output, regenerated by compilation.
- `local.properties` — machine-specific Android SDK location.

Keep `frontend/gradle/` **without the leading dot**, including its wrapper JAR, wrapper properties, and version catalog. Also keep `gradlew`, `gradlew.bat`, the Gradle build/settings files, source files, Android resources, and sample store data.

Deleting the generated `backend/out/` directory is fine, but the compile step must recreate `out/production/Duckie` before launching the Master.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `javac` is not recognized or found | Install a full JDK and add its `bin` directory to `PATH`. |
| `Could not find or load main class` | Compile first, use the full package name, and run from `backend/`. |
| Worker fails to launch | Ensure `java` is on `PATH` and compiled classes are under `backend/out/production/Duckie`. |
| `Connection refused` | Start the Master first and use matching host/port arguments. |
| Reducer missing or unavailable | Start the Reducer after the Master and wait for registration before searching or requesting reports. |
| Store JSON not found | Run the Manager from `backend/` and enter the exact folder name, such as `Burgermania`. |
| No matching stores | Register stores and check filters, fixed client coordinates, and search radius. |
| Port already in use | Stop the previous server or worker using that port; keep component arguments consistent if changing ports. |

## Verification

For this README update, the backend was compiled from source with the Java 17 compiler into a clean output directory. A local smoke test verified Master startup, Reducer registration, automatic Worker startup, and successful registration of the bundled `Burgermania` store through the Manager console.

Purchases, ratings, search, sales reports, failure recovery, and the Android build were not exercised in that smoke test. The implementation should not be interpreted as a production-ready delivery service or a guarantee of fault tolerance.

## Team

- Στυλιανή Μουμτζή(only backend)
- Ευγενία Λαζανά
- Ορφέας Νίνος

Developed as a team project for the Distributed Systems course, 2025. This README describes the shared project; it does not attribute the entire implementation to an individual contributor.
