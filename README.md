# UniverseBible

UniverseBible is an offline Android workspace for planning fictional cinematic universes. Version 0.1.0, Creative Foundation, connects productions, characters and phases in a local studio dashboard.

## Using the app

Install the APK from the repository’s [GitHub releases](https://github.com/LucaFilippi/CreateOwnUniverse-App/releases) on Android 8.0 or later. Open UniverseBible, create a universe, then enter its studio. Each universe has its own Projects, Characters and Phases tabs. The More tab contains universe settings and the English, Brazilian Portuguese and system language choices.

Create movies, series, specials and shorts, add cast appearances with optional narrative roles, and assign projects to phases. Edit a phase’s order to change its place in the lineup. Within a phase, productions follow release order. Removing a phase makes its projects available for reassignment. Deleting a universe permanently removes its records.

The dashboard uses locally stored records for totals and recent entries. Select images from the system picker for covers, posters and portraits. The app keeps a private copy for offline use.

## Android Studio and command-line builds

Open this directory in Android Studio with Android SDK 37 installed. Use the bundled Gradle wrapper with JDK 17 or newer. On Windows use `gradlew.bat` in place of `./gradlew`.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The connected tests require a running emulator or attached Android device. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. For a distribution build, configure the signing environment variables `UNIVERSE_STORE_FILE`, `UNIVERSE_STORE_PASSWORD`, `UNIVERSE_KEY_ALIAS` and `UNIVERSE_KEY_PASSWORD` and run `:app:assembleRelease`. Keep the keystore and credentials outside the repository.

## Architecture

Kotlin and Material 3 Compose provide the interface. Navigation Compose opens each universe workspace. The universe view model observes repository state with StateFlow. The studio repository combines Room queries into a reactive slate of projects, characters, phases and appearances. Room uses KSP, exports its schemas and migrates the original universe database to version 2. Foreign keys and transactional ownership checks enforce universe boundaries. DataStore holds language preferences and Coil displays local artwork.

## Limitations

Data is private to this installation. Uninstalling the app removes its database and artwork. Backup, import, export and historical character states are future features. The GitHub preview APK uses a debug signing key.

## Roadmap

| Release | Planned work |
| --- | --- |
| 0.2.0 | Sagas and advanced timeline |
| 0.3.0 | Teams, organizations and locations |
| 0.4.0 | Character relationships and narrative connections |
| 0.5.0 | Multiverse, variants and historical character states |
| 0.6.0 | Post-credits scenes and narrative events |
| 0.7.0 | Export, import, backup and cinematic showcase |
| 0.8.0 | Advanced story planning |
| 0.9.0 | Performance, accessibility and beta testing |
| 1.0.0 | Public release |
