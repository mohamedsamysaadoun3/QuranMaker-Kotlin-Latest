# QuranMaker (NurMontage) — Kotlin Rewrite

> تطبيق أندرويد لإنشاء فيديوهات قرآنية احترافية لمشاركتها على وسائل التواصل الاجتماعي — مع محرر متعدد الكيانات، خط زمني تفاعلي، ومحرك تصدير FFmpeg.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![compileSdk](https://img.shields.io/badge/compileSdk-35-0086FB?logo=android-studio&logoColor=white)](https://developer.android.com/about/versions/15)
[![minSdk](https://img.shields.io/badge/minSdk-24-FF6F00?logo=android&logoColor=white)](https://developer.android.com/about/versions/marshmallow)
[![License](https://img.shields.io/badge/license-Proprietary-lightgrey)](#-license--credits)
[![Version](https://img.shields.io/badge/version-6.7.1--QuranMaker-blue)](#-version-info)

---

## 📖 Table of Contents

1. [Overview](#1-overview)
2. [Features](#2-features)
3. [Version Info](#3-version-info)
4. [Tech Stack](#4-tech-stack)
5. [Project Structure](#5-project-structure)
6. [Architecture](#6-architecture)
7. [Core Packages](#7-core-packages)
8. [Models & Entities](#8-models--entities)
9. [Custom Views](#9-custom-views)
10. [Editor & Timeline Engine](#10-editor--timeline-engine)
11. [Export Pipeline (FFmpeg)](#11-export-pipeline-ffmpeg)
12. [Audio System](#12-audio-system)
13. [Quran Data](#13-quran-data)
14. [Localization](#14-localization)
15. [Resources & Assets](#15-resources--assets)
16. [Build & Signing](#16-build--signing)
17. [Permissions & Manifest](#17-permissions--manifest)
18. [Network & Security](#18-network--security)
19. [ProGuard Rules](#19-proguard-rules)
20. [Naming Quirks (Important)](#20-naming-quirks-important)
21. [For AI / Developers — Quick Map](#21-for-ai--developers--quick-map)
22. [License & Credits](#22-license--credits)

---

## 1. Overview

**QuranMaker** (also known internally as **NurMontage**) is an Android application that lets users create visually polished videos of the Holy Quran for social-media sharing. The user picks a background (image or video), selects a Quran passage and reciter, optionally adds a translation, then arranges all elements on a multi-touch timeline canvas. The app renders the final composition to an MP4 file via FFmpeg.

This repository is a **faithful Kotlin rewrite** of the original Java/JADX-decompiled APK. Comments throughout the codebase explicitly preserve the lineage, e.g. *"Originally: `hazem.nurmontage.videoquran.Base`"* and *"Converted from `SmartExportManager.java` — logic preserved, reflection removed"*. The original Java codebase called FFmpegKit via reflection; this rewrite uses the direct API.

The **Pro / billing / ads layer has been stripped** (explicitly noted in `AndroidManifest.xml`) — this is the **free, clean fork** named "QuranMaker".

### What this app does, end-to-end

1. **Splash screen** routes the user to either the projects gallery (`WorkUserActivity`) or directly into the editor (`EngineActivity`) if a saved project exists.
2. The user **adds a background** (image, video, or stock image from Pixabay).
3. The user **adds a Quran block**: selects a surah, a range of ayahs (up to 50 at a time), and a reciter. Audio is downloaded/streamed from the reciter's audio URL.
4. The user can **add translations** (10 languages supported), **bismillah**, **surah-name** headers, **progress bars**, and **free-form image layers**.
5. Each element on the canvas is fully **draggable, resizable, rotatable**, with **fade-in/fade-out** and **transition** effects.
6. The user can apply **audio effects** (reverb/masjid mode, echo, pitch correction) to the reciter's audio.
7. The user **exports** the result to MP4 (720p/1080p, 30 fps, with optional iPad-style frames, gradient overlays, cinematic color grade).

---

## 2. Features

### Creative features
- 🎬 **Multi-entity timeline editor** — Quran, translation, bismillah, surah-name, audio, image, video, and progress-bar blocks; each independently positioned/scaled/rotated.
- 📖 **Bundled Quran text** — Uthmani-simple Arabic text, plus 9 translations (English, French, German, Urdu, Persian, Turkish, Indonesian, Bengali, Arabic tafsir Muyassar).
- 🔍 **Quran search** — search by surah name or ayah text; tashkeel-insensitive matching (`RemoveTashkeel`).
- 🖼️ **Pixabay image search** — fetch stock background images directly.
- 🎵 **Reciter audio** — supports local audio files, remote URLs (incl. `commondatastorage.googleapis.com` cleartext), and audio extracted from videos via FFmpeg.
- 🎚️ **Audio effects** — `MasjidReverbFilter` (reverb simulating mosque acoustics), `PitchCorrector`, echo, fade, volume, speed.
- 📐 **Aspect ratios** — Story, Square, Landscape, plus iPad-frame overlays with neumorphic/glass styling.
- 🎨 **Cinematic color grade** — `CinematicProcessor` applies `ColorMatrix` + `RadialGradient` (vignette) and glass-rect overlays.
- 🔤 **70+ Arabic calligraphy fonts** — including Uthmani, Kufi, Naskh, Thuluth, Qalam, Mishaf, and more (see `assets/fonts/`).
- 🎆 **Konfetti celebration animations** — used on success screens.
- 🌐 **9-language UI** — full app locale switching via Android 13+ per-app language API.

### Technical features
- ⚡ **Baseline profile** (`assets/dexopt/baseline.prof` + `baseline.profm`) for faster cold start.
- 🛡️ **Custom crash handler** — `CrashHandler.kt` writes timestamped stack traces to external files for user-shared bug reports.
- 🧠 **Undo / redo** — `StackEntity` snapshots for every entity on the canvas.
- 🪟 **Splash Screen API** (AndroidX `core-splashscreen:1.0.1`).
- 📱 **Edge-to-edge UI**, immersive system bars (`BaseActivity.hideSystemBars()`).
- 🔄 **Auto-save** — `EngineActivity.onPause()` triggers `ExportPipeline.saveTemplateTmp()`.
- 📤 **Inbound share** — `ShareWithMeActivity` accepts `image/*`, `audio/*`, `video/*` intents.
- 🌗 **Dark mode** — `values-night` + `color-night` resource qualifiers.
- 📐 **Multi-screen support** — 21 `values-*` qualifier directories (h320dp-port, sw600dp, xlarge, v27, v31, …) and `layout-sw600dp` for tablets.

---

## 3. Version Info

| Field | Value |
|---|---|
| Application ID | `hazem.nurmontage.videoquran` |
| Version name | `6.7.1-QuranMaker` |
| Version code | `21000200` |
| compileSdk | 35 (Android 15) |
| minSdk | 24 (Android 7.0) |
| targetSdk | 35 |
| Java version | 17 |
| Kotlin version | 1.9.25 |
| AGP version | 8.7.3 |
| App name (default) | `NurMontage` |
| App name (Arabic) | `NurMontage` |

---

## 4. Tech Stack

### Languages & Runtime
- **Kotlin 1.9.25** (stdlib 1.9.25, coroutines 1.9.0)
- **Java 17** (source/target compatibility)
- **JVM target**: `17`

### AndroidX
| Library | Version |
|---|---|
| `androidx.core:core-ktx` | 1.15.0 |
| `androidx.appcompat:appcompat` | 1.7.0 |
| `androidx.activity:activity-ktx` | 1.9.3 |
| `androidx.fragment:fragment-ktx` | 1.8.5 |
| `androidx.constraintlayout` | 2.2.0 |
| `androidx.recyclerview` | 1.3.2 |
| `androidx.viewpager2` | 1.1.0 |
| `androidx.preference:preference-ktx` | 1.2.1 |
| `androidx.lifecycle:lifecycle-{runtime,viewmodel,livedata}-ktx` | 2.8.7 |
| `androidx.core:core-splashscreen` | 1.0.1 |
| `androidx.emoji2:emoji2` | 1.4.0 |
| `androidx.profileinstaller` | 1.4.1 |
| `androidx.window:window` | 1.3.0 |

### Media
| Library | Version | Purpose |
|---|---|---|
| `com.arthenica:ffmpeg-kit-full` | 6.0-2 | Video compositing, audio extraction, codec conversion |
| `androidx.media3:media3-{exoplayer,ui,common}` | 1.5.1 | In-app video preview |

### UI & Visuals
| Library | Version | Purpose |
|---|---|---|
| `com.google.android.material:material` | 1.12.0 | Material 3 components |
| `com.github.bumptech.glide:glide` | 4.16.0 | Image loading |
| `jp.wasabeef:glide-transformations` | 4.3.0 | Image transformations |
| `nl.dionsegijn:konfetti-{core,xml}` | 2.0.4 | Confetti animations |

### Utilities
| Library | Version |
|---|---|
| `com.google.code.gson:gson` | 2.10.1 |
| `commons-io:commons-io` | 2.16.1 |
| `com.google.android.gms:play-services-{base,tasks}` | 18.5.0 / 18.2.0 |

### Local Maven Repository
The project ships a local Maven repo at `local-maven-repo/` containing:
- `com.arthenica:ffmpeg-kit-full:6.0-2` (AAR — ~50 MB, GitHub LFS recommended)
- `nl.dionsegijn:konfetti-{core,xml}:2.0.4` (AARs)

These are referenced in `settings.gradle.kts` via:
```kotlin
maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
```

### Build Features
- **View Binding**: ✅ enabled
- **Data Binding**: ✅ enabled
- **Minify (release)**: ❌ disabled (`isMinifyEnabled = false`)
- **Lint abortOnError**: ❌ false (per `lint { abortOnError = false }`)

---

## 5. Project Structure

```
QuranMaker-Kotlin-main/
├── .gitignore                       # Android + project-specific ignores
├── README.md                        # This file
├── build.gradle.kts                 # Root: AGP 8.7.3, Kotlin 1.9.25
├── settings.gradle.kts              # Repositories + local Maven
├── gradle.properties                # JVM args, AndroidX, JDK 21 path
├── gradlew / gradle/wrapper/        # Gradle wrapper
├── local-maven-repo/                # FFmpeg + Konfetti AARs
│   ├── com/arthenica/ffmpeg-kit-full/6.0-2/
│   └── nl/dionsegijn/konfetti-{core,xml}/2.0.4/
└── app/
    ├── build.gradle.kts             # App module config
    ├── proguard-rules.pro           # Keep rules for models + FFmpeg
    └── src/main/
        ├── AndroidManifest.xml      # 18 activities, FileProvider, permissions
        ├── assets/
        │   ├── dexopt/              # Baseline profile (baseline.prof, .profm)
        │   ├── fonts/               # 12 Latin + 62 Arabic calligraphy fonts
        │   │   ├── ReadexPro_Bold.ttf
        │   │   ├── Alegreya-Regular.ttf
        │   │   ├── NotoSans.ttf
        │   │   ├── Poppins-Regular.ttf
        │   │   └── arabic/          # 62 files — Uthmani, Kufi, Naskh, Thuluth, …
        │   └── quran/               # 10 text files (~13 MB) — see §13
        ├── java/hazem/nurmontage/videoquran/   # 271 Kotlin files
        │   ├── core/                # App, CrashHandler, BaseActivity, Constants
        │   │   ├── App.kt
        │   │   ├── CrashHandler.kt
        │   │   ├── base/BaseActivity.kt
        │   │   └── common/{Constants.kt, Common.kt, StackEntity.kt}
        │   ├── common/              # DataDimension.kt
        │   ├── constant/            # 7 enums/sealed classes
        │   ├── model/               # 35 serializable entity classes
        │   ├── entity_timeline/     # 5 runtime canvas entities
        │   ├── multitouch/          # 5 gesture detectors
        │   ├── audio/               # MasjidReverbFilter, PitchCorrector
        │   ├── export/              # SmartExportManager, CodecOptimizer
        │   ├── fragment/            # 27 + 9 audio_effect fragments
        │   ├── adapter/             # 31 RecyclerView adapters
        │   ├── utils/               # 51 + 10 utility files (see §7)
        │   ├── views/               # 24 + 15 custom views (see §9)
        │   └── ui/                  # 41 activities across 11 subpackages
        │       ├── splash/          # FullscreenActivity (LAUNCHER)
        │       ├── home/            # WorkUserActivity, YoutuberActivity
        │       ├── engine/          # 15 files — the editor (see §10)
        │       ├── editor/          # 10 files — text/audio/crop editors
        │       ├── render/          # ProgressViewActivity + ExportCommandBuilder
        │       ├── gallery/         # GalleryPickerVideo
        │       ├── gallery_photos/  # GalleryPickerOneImage
        │       ├── search/          # QuranSearchActivity, PixabaySearchActivity
        │       ├── settings/        # 4 settings screens
        │       ├── share/           # ShareWithMeActivity
        │       └── provider/        # MyProvider (FileProvider subclass)
        └── res/
            ├── anim/ animator/ color/ color-night/ color-v31/
            ├── drawable/ drawable-anydpi/ drawable-{hdpi,xhdpi,xxhdpi,ldrtl-xxhdpi}/
            ├── font/ interpolator/ menu/ raw/ xml/
            ├── layout/ (86) layout-ar/ (20) layout-sw600dp/
            ├── mipmap-{anydpi-v26,hdpi,mdpi,xhdpi,xxhdpi,xxxhdpi}/
            └── values/ values-ar/ values-night/ values-port/ values-land/
                values-{small,large,xlarge,sw600dp,h320dp-port,h360dp-land,
                        h480dp-land,h550dp-port,h720dp,w320dp-land,w360dp-port,
                        w400dp-port,w600dp-land,v27,v28,v31}/
```

### File counts (summary)

| Category | Count |
|---|---:|
| Kotlin source files (`.kt`) | **271** |
| Layout XMLs (`layout/` + `layout-ar/` + `layout-sw600dp/`) | **107** |
| Drawables (`drawable/`) | 349 |
| Fonts in `assets/fonts/` (Latin + Arabic) | 12 + 62 = **74** |
| Quran text files in `assets/quran/` | **10** |
| `values*` qualifier directories | **21** |
| Total files in repository | **1,305** |

---

## 6. Architecture

This codebase is **NOT Clean Architecture, MVVM, or MVI**. The original README tag "Clean Architecture" is a misnomer — the codebase is a faithful Kotlin port of a JADX-decompiled APK and uses the classic **Activity + Fragment + Adapter + Plain-Serializable-Model** pattern (sometimes called "MVC with Fragments").

### Architectural characteristics

| Aspect | Reality |
|---|---|
| ViewModels | ❌ None |
| Use cases / Interactors | ❌ None |
| Repositories | ❌ None |
| Room / SQLite / ORM | ❌ None |
| DI framework (Hilt/Dagger/Koin) | ❌ None |
| Persistence | `LocalPersistence` (Java `ObjectOutputStream` to internal storage) + `QuranPreference`/`MyPreferences` (SharedPreferences) |
| Networking | `HttpURLConnection` in `utils/audio/AudioUtils.kt` |
| Image loading | Glide 4.16.0 |
| Async work | `kotlinx-coroutines` + single-thread `ExecutorService` in audio utils |
| God-object mitigation | Engine classes split into **14+ extension-function files** instead of decomposition into ViewModels |

### Why the "Clean Architecture" tag?

The original Java APK used a single ~3,000-line `EngineActivity`. The Kotlin rewrite decomposes that god-class into 14 extension-function modules in `ui/engine/` (see §10). This is decomposition, not Clean Architecture — but it does make the engine code reviewable.

### Layering (informal)

```
┌─────────────────────────────────────────────────────────┐
│ UI Layer (Activities + Fragments + Adapters + Views)    │
│   ui/splash, ui/home, ui/engine, ui/editor,             │
│   ui/render, ui/search, ui/settings, ui/share, …        │
├─────────────────────────────────────────────────────────┤
│ Logic Layer (objects + extension functions)             │
│   ui/engine/Engine*Manager.kt, ExportPipeline.kt,       │
│   FfmpegCommandBuilder.kt, TimelineEngine.kt,           │
│   export/SmartExportManager.kt, audio/MasjidReverbFilter│
├─────────────────────────────────────────────────────────┤
│ Data Layer (plain Serializable models + assets)         │
│   model/*.kt, entity_timeline/*.kt,                     │
│   assets/quran/*.txt, assets/fonts/*                    │
├─────────────────────────────────────────────────────────┤
│ Persistence (SharedPreferences + Java Serialization)    │
│   utils/LocalPersistence.kt, QuranPreference.kt,        │
│   MyPreferences.kt                                      │
└─────────────────────────────────────────────────────────┘
```

---

## 7. Core Packages

### `core/` — Application bootstrap & base classes (6 files)

| File | Role |
|---|---|
| `App.kt` | `Application` subclass. Calls `CrashHandler.install(this)` in `onCreate()`. Nothing else. |
| `CrashHandler.kt` | Installs `Thread.setDefaultUncaughtExceptionHandler`. Writes timestamped crash logs (device, Android version, app version, stack trace) to `getExternalFilesDir(null)/crash_log.txt`. |
| `base/BaseActivity.kt` | Abstract parent of every activity. Provides `hideSystemBars()` (API 30+ uses `WindowInsetsControllerCompat`, below uses legacy `systemUiVisibility` flags), `setStatusBarColor(Int)`, `setNavigationBarColor(Int)`, `setLightStatusBar(Boolean)`, `wakeLockAcquire()` (`FLAG_KEEP_SCREEN_ON`). |
| `common/Constants.kt` | Single source-of-truth for fonts (`FONT_QURAN`, `FONT_SURAH_NAME`, …), colors (`COLOR_AYA`, `COLOR_TRANSLATION`, …), layout ratios (`BLOCK_HEIGHT`, `IPAD_W`, `SQUARE_H`, …), file/folder names, and enum holders (`EntityAction`, `AyaTextPreset`, `IpadType`, `TransitionType`). |
| `common/Common.kt` | Shared legacy helpers (kept from `Common.java`). |
| `common/StackEntity.kt` | Snapshot of an entity's rectangle for undo/redo. |

### `constant/` — Enums & sealed classes (7 files)

| Enum | Purpose |
|---|---|
| `AyaTextPreset` | Pre-defined ayah text size/style presets. |
| `EffectAudioType` | Audio effect categories (reverb, echo, pitch, etc.). |
| `EntityAction` | Action types for entity manipulation (add, delete, transform, …). |
| `IpadType` | iPad frame variants (neumorphic, glass, etc.). |
| `ResizeType` | Output aspect ratios (story, square, landscape, …). |
| `SurahNameStyle` | Surah-name header visual styles. |
| `TransitionType` | Fade, slide, etc. for entity transitions on the timeline. |

### `model/` — Plain serializable entities (35 files)

All model classes implement `java.io.Serializable` and use Java-style mutable `var` fields. There is no `data class` pattern except for `EntityQuranTemplate`. Key classes:

| Class | Role |
|---|---|
| `Template.kt` | **Root serializable document** for a saved project. Holds dimensions (`width`, `height`), `duration`, `scale_timeline`, `resolution` ("720p"), `fps` (30), `resizeType`, `uri_bg`, `uri_bg_ffmpeg`, `uri_video`, `frame_bg`, `ipad_type`, `gradient`, `isGlass`, `isVideoSquare`, `squareBitmapModel`, `mDrawingTranslationX/Y`, `mTimeModel`, `entityProgressTemplate`, `entityBismilahTemplate`, `entityIsti3adaTemplate`, `entitySurahTemplate`, plus the runtime lists `entityMediaList`, `quranEntityList`, `translationTemplateList`. Supports `duplicate()` via Java serialization round-trip. |
| `EntityMedia.kt` | Audio/video/image clip on the timeline. Fields: `path_ffmpeg`, `video_path`, `paths_https`, `start/end/offset/max`, `posX/posY/x/y/w/h`, `mScale`, `duration_fade_in/out`, `volume`, `isSoundEnable`, `effectAudio: EffectAudio?`, `index_start/end_thumbnail`. Has 4 overloaded constructors + `duplicate()`. |
| `QuranEntity.kt` | **Largest model file (64 KB).** `class QuranEntity : EntityView, Serializable`. The runtime/drawable Quran block. Owns color state (`clrAya`, `clrTrsl`), `StaticLayout` for ayah text, draws itself to a `Canvas`, integrates `BlurredImageView`/`TrackEntityView`, supports icon (hafes/others), spans (`EndOfAyaSpan`), font presets. |
| `EntityQuranTemplate.kt` | `data class` snapshot of a Quran ayah block for serialization. Fields: `transition`, `start/end`, `aya`/`complete_aya`, `translation`/`translation_complete`, `number`, `color`, `colorTrsl`, `name_font`, `preset`, plus runtime `x/y/scale/factor_size/height/icon/startWord_index/endWord_index/rectF/file/file_in/file_out`. |
| `BismilahEntity.kt` | Bismillah block state. |
| `TranslationQuranEntity.kt` | Translation block state. |
| `TextEntity.kt` | Free-form text element. |
| `EntityBismilahTemplate.kt`, `EntityTranslationTemplate.kt`, `EntitySurahTemplate.kt`, `EntityProgressTemplate.kt` | Template snapshots of bismillah / translation / surah-name / progress-bar blocks. |
| `EntitySelectTool.kt` | Selection-tool state. |
| `EffectAudio.kt` | Audio effect descriptor (reverb/echo/pitch parameters). |
| `RenderManager.kt` + `RenderTask.kt` | FFmpeg render queue and individual render tasks. |
| `Gradient.kt`, `IpadItem.kt`, `BgItem.kt`, `PhotoItem.kt`, `VideoItem.kt`, `SquareBitmapModel.kt`, `MRectF.kt` | Visual/background descriptors. |
| `RecitersModel.kt`, `YoutuberModel.kt`, `ExploreItem.kt`, `ModelFeatures.kt`, `GallerySelected.kt`, `ItemDimension.kt`, `ItemQuranSearch.kt`, `SurahNameEntity.kt`, `TimeModel.kt`, `Transition.kt`, `WordModel.kt`, `EntityView.kt`, `FreeElement.kt` | Various supporting entities. |

### `entity_timeline/` — Runtime canvas entities (5 files)

| Class | Role |
|---|---|
| `Entity.kt` | Abstract base — defines the timeline-entity contract. |
| `EntityAudio.kt` | Audio clip on the timeline. |
| `EntityQuranTimeline.kt` | Quran block as it appears on the timeline canvas. |
| `EntityTrslTimeline.kt` | Translation block on the timeline. |
| `EntityBismilahTimeline.kt` | Bismillah block on the timeline. |

### `multitouch/` — Gesture detectors (5 files)

In-house multi-touch detectors used by `TrackEntityView` for canvas manipulation:
- `RotateGestureDetector` — two-finger rotation.
- `ShoveGestureDetector` — vertical shove (pan).
- `MoveGestureDetector` — drag.
- `TwoFingerGestureDetector` — base class for two-finger gestures.
- `BaseGestureDetector` — abstract base.

### `audio/` — Audio effect generators (2 files)

| File | Role |
|---|---|
| `MasjidReverbFilter.kt` | Generates FFmpeg filter chain to simulate mosque (masjid) reverb acoustics. |
| `PitchCorrector.kt` | Generates FFmpeg pitch-correction filter chain. |

### `export/` — Export runtime (2 files)

| File | Role |
|---|---|
| `SmartExportManager.kt` (219 lines) | Background FFmpeg export with a foreground notification (`CHANNEL_ID = "video_export_channel"`, `IMPORTANCE_LOW`), `StatisticsCallback` for progress %, `@Volatile` cancellation, main-thread `ExportCallback` (`onExportProgress/onExportComplete/onExportError/onExportCancelled`). Uses the **direct FFmpegKit API** (original Java used reflection). |
| `CodecOptimizer.kt` | Codec/bitrate selection helpers. |

### `fragment/` — Dialog & bottom-sheet fragments (27 + 9 = 36 files)

Bottom-sheet and dialog fragments for editing entities, colors, fonts, audio effects. The `fragment/audio_effect/` subpackage (9 files) contains the audio-effect editor fragments (reverb, echo, pitch, fade, speed, volume, enhance, replace, equalizer).

### `adapter/` — RecyclerView adapters (31 files)

RecyclerView/Spinner adapters. Note: the original codebase consistently spells "Adapter" as **"Adabter"** (typo preserved from JADX — see §20). Each adapter binds a specific model to a specific layout (e.g. `AyaAdabter`, `RecitersAdabter`, `WorkUserAdabter`, `YoutuberAdabter`, etc.).

### `utils/` — Utilities (51 + 10 = 61 files)

#### Top-level utilities

| File | Role |
|---|---|
| `QuranReader.kt` | Reads `quran/quran-simple.txt` (and translation files) line-by-line, splits on `\|` to resolve `getAyahText(surah, ayah)` / `getTranslationAyahText(file, surah, ayah)`. |
| `LocalPersistence.kt` | Java-serialization (`ObjectOutputStream`) to internal storage — used to save/load `Template` projects. |
| `QuranPreference.kt`, `MyPreferences.kt` | SharedPreferences wrappers. |
| `LocaleHelper.kt` | Per-app locale override via `attachBaseContext` (Android 13+ per-app language API). |
| `FontProvider.kt`, `FontUtils.kt`, `TypefaceCache.kt` | Font loading & caching. |
| `BitmapCropper.kt`, `BitmapSaver.kt`, `UtilsBitmap.kt`, `JavaBM.kt` | Bitmap operations. |
| `FileUtils.kt`, `UtilsFile.kt`, `UtilsFileLast.kt`, `MFileUtils.kt`, `FileHelper.kt`, `QuranFileUtils.kt`, `FileMediaScanner.kt` | File system helpers. |
| `ColorUtils.kt`, `ColorSchemeGenerator.kt`, `CreateGradient.kt` | Color & gradient. |
| `CanvasUtils.kt`, `EndOfAyaSpan.kt`, `RemoveTashkeel.kt`, `WordProcessor.kt`, `CustomTypefaceSpan.kt` | Text/canvas spans & tashkeel-stripping for Arabic search. |
| `ScreenUtils.kt`, `AspectRatioCalculator.kt`, `TimeFormatter.kt`, `PriceFormatter.kt` | Formatting helpers. |
| `AudioUploadHelper.kt`, `AppSettingsHelper.kt`, `AppUtils.kt`, `NetworkUtils.kt`, `MyVibrationHelper.kt`, `DrawableHelper.kt`, `ImageLoader.kt`, `StoryCropTransformation.kt`, `TranslationExtractor.kt`, `Feadback.kt`, `MItemAdabterJson.kt`, `NonScrollableLinearLayoutManager.kt` | Misc helpers. |

#### `utils/animator/` (2 files)
- `TimelineAnimator.kt` — drives timeline playback animation.
- `SmoothTimelineAnimator.kt` — smoother variant used by `EngineActivity`.

#### `utils/audio/` (2 files)
- `AudioUtils.kt` — `object` with single-thread `ExecutorService`. `copyToLocalAsync(context, source, destDir, callback)` dispatches to `copyFromUri` (ContentResolver) or `downloadFile` (`HttpURLConnection`). Delivers callback on main thread.
- `FfmpegCodecChecker.kt` — codec availability check.

#### `utils/video/` (2 files)
- `CinematicProcessor.kt` — `object` applying color-grade + vignette via `ColorMatrix`/`RadialGradient` (`applyCinematicEffect`), and `createGlassRect` for rounded-rect glass frames.
- `SmoothVideoAnimator.kt` — frame-by-frame video preview animator.

#### `utils/waveform/` (4 files + 5 root-level variants)
- `FastWaveformExtractor.kt`, `AmplitudeExtract.kt`, `WaveformExtractor.kt`, `WaveformBitmapRenderer.kt`.
- Root-level variants (iterative performance work): `FastWaveformExtractorOptimized.kt`, `FastWaveformExtractorPro.kt`, `UltraFastWaveform.kt`, `UltraFastWaveformOptimized.kt`, `PCMWaveformExtractor.kt`, `WaveformRendererPro.kt`.

---

## 8. Models & Entities

### Class diagram (simplified)

```
Serializable (Java)
   │
   ├── Template (root project document)
   │       ├── EntityProgressTemplate
   │       ├── EntityBismilahTemplate
   │       ├── EntityIsti3adaTemplate
   │       ├── EntitySurahTemplate
   │       ├── ArrayList<EntityMedia>          entityMediaList
   │       ├── ArrayList<EntityQuranTemplate>  quranEntityList
   │       └── ArrayList<EntityTranslationTemplate> translationTemplateList
   │
   ├── EntityMedia  (audio/video/image clip)
   │       └── EffectAudio
   │
   ├── EntityView (interface)
   │       └── QuranEntity  (runtime drawable Quran block)
   │              ├── StaticLayout  ayahStaticLayout
   │              ├── BlurredImageView
   │              └── TrackEntityView
   │
   ├── BismilahEntity
   ├── TranslationQuranEntity
   ├── TextEntity
   ├── FreeElement
   ├── SurahNameEntity
   ├── TimeModel
   ├── Gradient
   ├── IpadItem
   ├── BgItem, PhotoItem, VideoItem
   ├── SquareBitmapModel
   ├── MRectF
   ├── RenderManager → RenderTask
   └── RecitersModel, YoutuberModel, ExploreItem, ModelFeatures, GallerySelected,
       ItemDimension, ItemQuranSearch, Transition, WordModel

entity_timeline.Entity (abstract)
   ├── EntityAudio
   ├── EntityQuranTimeline
   ├── EntityTrslTimeline
   └── EntityBismilahTimeline

StackEntity (snapshot for undo/redo)
```

### Persistence

- **Projects** — serialized via `LocalPersistence.serializeTemplate()` to `getFilesDir()/templates/<uuid>.dat` (Java `ObjectOutputStream`).
- **Auto-save** — `EngineActivity.onPause()` calls `ExportPipeline.saveTemplateTmp()` writing `<uuid>.tmp.dat`.
- **Settings** — `QuranPreference` and `MyPreferences` use `SharedPreferences`.
- **No Room, no SQLite, no JSON.** The choice of Java serialization over JSON is intentional — it preserves the exact reference graph of `Template`, including cyclic back-references between `QuranEntity` and its `BlurredImageView`/`TrackEntityView` parents (transient fields skipped).

---

## 9. Custom Views

### `views/` — Top-level (24 files)

| View | Role |
|---|---|
| `TrackEntityView.kt` (1,265 lines + 4 split modules) | **The heart of the editor canvas.** A `View` that renders the timeline with all entities, handles multitouch (rotate/scale/pan via `multitouch/` detectors), supports selection/deselection/multi-select, and runs enter/exit animations via `ObjectAnimator` + `Scroller`. Uses `DashPathEffect`, `Path`, custom `Paint`, Arabic-aware time formatting (`formatTimeLabelArabicExt`). |
| `BlurredImageView.kt` | Background image with realtime blur (rendered by `views/blurred/BlurredRenderer`). |
| `CassetteView.kt` | Decorative cassette-tape animation. |
| `CropView.kt` + `CropViewHint.kt` | Image cropper UI. |
| `BeforeAfterView.kt` | Before/after comparison slider. |
| `NeumorphicView.kt`, `NeumorphicRectView.kt` | Soft-UI panels (neumorphic shadows). |
| `GradientProgressBar.kt`, `SquareOutlineProgressBar.kt` | Custom progress bars. |
| `VideoFrameSelectorView.kt` | Frame selector scrubber for video preview. |
| `ArrowOverlayDecoration.kt`, `ScrollFadeDecoration.kt` | Decorations. |
| `CustomDiscreteSeekBar.kt` | Custom discrete seek bar. |
| `EyeView.kt`, `EyeOpenView.kt` | Password-visibility toggles. |
| `WaveformView.kt` (95 lines) | Amplitude bar chart; bars before `progress` are white, after are dark gray. Touch/drag seeks via `OnWaveformClickListener.onProgressChanged(Float)`. `setAmplitudes(IntArray)` / `setProgress(Float)`. |
| `TextCustumFont.kt`, `TextCustumFontBold.kt`, `EditTextCustumFont.kt`, `ButtonCustumFont.kt`, `CheckboxCustumFont.kt` | Custom-font widget family. |

### `views/track/` — TrackEntityView extension modules (4 files)

| File | Role |
|---|---|
| `TrackEntityManager.kt` | Selection, hit-testing, multi-select helpers. |
| `TrackEntityAnimation.kt` | Enter/exit translate animations (`translateToRightExt`, `translateFromStartExt`, … 12+ variants). |
| `TrackEntityRenderer.kt` | Drawing (`drawAllEntitiesExt`, `drawBasmalaExt`, `drawMarkerExt`, `drawTimeBarExt`, `drawItemBtnExt`, `drawIconDrawableExt`). |
| `TrackEntityTouchHandler.kt` | Touch dispatch to entities, gesture routing. |

### `views/blurred/` — Blur render pipeline (4 files)

- `BlurredRenderer.kt`, `BlurredEntityRenderer.kt`, `BlurredIpadRenderer.kt`, `BlurredRectBuilder.kt` — the render pipeline behind `BlurredImageView`.

### `views/text/` — Arabic-aware text widgets (4 files)

- `AyaCustumFont.kt` — ayah rendering widget.
- `NurMontageFont.kt` — font applier.
- `TextCustumFontAR.kt` — Arabic text widget.
- `AyaCircleBg.kt` — circle background for ayah-end markers.

### `views/image/` — Image variants (2 files)
- `SquareImageView.kt`, `SquareImageViewSimple.kt` — square-cropped image views.

### `views/widget/` (1 file)
- `RadioBtnCustumFont.kt` — custom-font radio button.

---

## 10. Editor & Timeline Engine

The editor is the heart of the app. It is split across **15 files** in `ui/engine/`:

| File | Lines (approx) | Role |
|---|---:|---|
| `EngineActivity.kt` | 1,000+ | Lifecycle, view binding, companion (`FPS = 25`). Hosts `TrackEntityView`, `BlurredImageView`, undo/redo, play/pause, activity launcher. Extends `BaseActivity`. |
| `EngineAudioManager.kt` | — | Audio playback state, reciter selection. |
| `EngineEntityManager.kt` | — | Add/remove/update entities on the canvas. |
| `EngineUIHelper.kt` | — | UI wiring (buttons, dialogs, toasts). |
| `EngineCallbacks.kt` | — | Activity-result callbacks, permission callbacks. |
| `EngineTimelineManager.kt` | — | Timeline play/pause/seek, frame callbacks. |
| `FfmpegCommandBuilder.kt` | — | Per-entity FFmpeg filter builder (used by export). |
| `EngineSaveHelper.kt` | — | Save/load helpers. |
| `BackgroundManager.kt` | — | Background image/video lifecycle. |
| `ExportPipeline.kt` | 776 | Engine "Save" entry point. Prepares export-resolution bitmaps → crop → iPad frames → `serializeTemplate()` → launch `ProgressViewActivity`. Exposes `save()`, `saveTemplate()`, `saveTemplateTmp()`, `isExporting`. |
| `AudioEffectProcessor.kt` | — | Apply reverb/echo/pitch via FFmpeg. |
| `AudioLoadingManager.kt` | — | Async audio download/copy. |
| `TimelineEngine.kt` | — | Core timeline math (time-to-frame, frame-to-time). |
| `VideoPlayerController.kt` | — | ExoPlayer wrapper for background-video preview. |
| `TemplateRestorer.kt` | — | Rehydrate a saved `Template` back into live canvas state. |

### Why 15 files?

The original Java `EngineActivity` was ~3,000 lines. The Kotlin rewrite keeps `EngineActivity.kt` as a thin shell (lifecycle + property declarations) and pushes **all non-lifecycle methods into Kotlin extension functions** in the 14 sibling files. This is documented in `EngineActivity.kt`:

```kotlin
// Extension function modules (split files) — provide implementations for all
// non-lifecycle methods. Class members take precedence over extensions,
// so removing a member function here makes the extension version active.
// See: EngineAudioManager.kt, EngineEntityManager.kt, EngineUIHelper.kt,
//      EngineCallbacks.kt, EngineTimelineManager.kt, FfmpegCommandBuilder.kt,
//      EngineSaveHelper.kt, BackgroundManager.kt, ExportPipeline.kt,
//      AudioEffectProcessor.kt, AudioLoadingManager.kt, TimelineEngine.kt,
//      VideoPlayerController.kt, TemplateRestorer.kt
```

### Editor FPS

The editor runs at **25 FPS** (`companion object { const val FPS = 25 }` in `EngineActivity`). Export uses 30 FPS by default (in `Template.fps`).

---

## 11. Export Pipeline (FFmpeg)

Export is split across **four locations**:

### Stage 1 — `ExportPipeline.kt` (`ui/engine/`, 776 lines)
Engine "Save" entry point. Constructor takes:
- `context: Context`
- `executor: Executor`
- `bitmapLoader: (url: String) -> Bitmap` (Glide)
- progress / success / error callbacks

Flow:
1. Prepare export-resolution bitmaps (upscale from canvas resolution to `720p` / `1080p`).
2. Crop background to chosen aspect ratio (`ResizeType`).
3. Apply iPad-frame overlays if enabled (`IpadType`).
4. `serializeTemplate()` — walks `TrackEntityView.entities` and writes a `Template` with all `EntityQuranTemplate`, `EntityTranslationTemplate`, `EntityBismilahTemplate`, `EntitySurahTemplate`, `EntityProgressTemplate`, `EntityMedia`.
5. Launch `ProgressViewActivity` with the serialized template.

Exposes `save()`, `saveTemplate()`, `saveTemplateTmp()` (auto-save on `onPause()`), and `isExporting` guard.

### Stage 2 — `ExportCommandBuilder.kt` (`ui/render/`, 1,446 lines)
`object` that builds the FFmpeg `filter_complex` strings. Documented stages:
1. **Pre-render video segments** — crop/scale/masks for each video clip on the timeline.
2. **`drawtext` timer overlay** — render the on-screen timer.
3. **Overlay all entities** — Quran, translation, bismillah, surah-name, image, video, progress bar.
4. **Per-entity fade/slide transitions** — `mFadeFilter`, `mSlideFilter`, etc. (preserved verbatim from the original Java).
5. **`amix` audio** — mix reciter audio + background-video audio + audio effects.
6. **Final encode** — H.264 video + AAC audio.

Stores `lastOverlayFilter` for crash reports.

### Stage 3 — `SmartExportManager.kt` (`export/`, 219 lines)
Background FFmpeg export with:
- Foreground notification (`CHANNEL_ID = "video_export_channel"`, `IMPORTANCE_LOW`).
- `StatisticsCallback` for progress %.
- `@Volatile` cancellation flag.
- Main-thread `ExportCallback` (`onExportProgress`, `onExportComplete`, `onExportError`, `onExportCancelled`).

Uses the **direct FFmpegKit API** (original Java used reflection because JADX couldn't resolve the classpath).

### Stage 4 — `ProgressViewActivity.kt` (`ui/render/`)
Full-screen progress UI that ties stages 1–3 together. Shows progress bar, percentage, cancel button, and a "please don't lock the screen" warning (`R.string.prgress_hint`).

---

## 12. Audio System

### Sources
1. **Local files** — picked via `GalleryPickerVideo` (audio extracted from video via FFmpeg) or system audio picker (`ActivityResultContracts`). Copied to internal storage by `utils/audio/AudioUtils.kt`.
2. **Remote URLs** — downloaded by `AudioUtils.downloadFile()` via `HttpURLConnection`. Cleartext HTTP allowed only for `commondatastorage.googleapis.com` (see §18).
3. **Reciter audio from sister app** — the manifest declares a `<queries>` entry for `hazem.tuffah.quranaudio` (the developer's other Quran-audio app), suggesting inter-app audio exchange.

### Audio effects (`audio/` package + `fragment/audio_effect/`)
- **`MasjidReverbFilter.kt`** — generates FFmpeg `aecho` + `lowpass` filter chain to simulate mosque (masjid) reverb.
- **`PitchCorrector.kt`** — generates FFmpeg `asetrate` + `atempo` filter chain for pitch correction.
- **9 audio-effect fragments** — UI editors for: reverb, echo, pitch, fade, speed, volume, enhance, replace, equalizer.

### Audio loading flow
```
User picks reciter + surah/ayah range
   ↓
EngineActivity → AudioLoadingManager.copyToLocalAsync()
   ↓
AudioUtils.dispatch → copyFromUri (ContentResolver) OR downloadFile (HttpURLConnection)
   ↓
Audio file lands in internal storage
   ↓
EngineAudioManager loads into ExoPlayer for preview
   ↓
On export: ExportCommandBuilder → FFmpeg `-i` + amix + effects chain
```

---

## 13. Quran Data

The Quran text is bundled in `app/src/main/assets/quran/` as **10 plain-text files** (total ~13 MB). There is **no JSON, no database, no Room**.

| File | Size | Language / Source |
|---|---:|---|
| `quran-simple.txt` | 1.34 MB | Arabic (Uthmani-simple) — master text |
| `ar.muyassar.txt` | 2.54 MB | Arabic tafsir (Muyassar) |
| `en.hilali.txt` | 1.15 MB | English (Hilali-Khan) |
| `fr.hamidullah.txt` | 948 KB | French (Hamidullah) |
| `de.bubenheim.txt` | 982 KB | German (Bubenheim) |
| `ur.maududi.txt` | 1.56 MB | Urdu (Maududi) |
| `fa.fooladvand.txt` | 1.34 MB | Persian (Fooladvand) |
| `tr.ozturk.txt` | 932 KB | Turkish (Öztürk) |
| `id.indonesian.txt` | 1.16 MB | Indonesian |
| `bn.bengali.txt` | 2.16 MB | Bengali |

### Format

Pipe-delimited `surah|ayah|text`:

```
1|1|بِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ
1|2|الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ
1|3|الرَّحْمَـٰنِ الرَّحِيمِ
1|4|مَالِكِ يَوْمِ الدِّينِ
1|5|إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ
```

### Reading code

`utils/QuranReader.kt` reads files line-by-line and splits on `\\|`:
```kotlin
fun getAyahText(surah: Int, ayah: Int): String {
    // reads assets/quran/quran-simple.txt
    // splits each line on "|"
    // returns the text part where parts[0]==surah.toString() && parts[1]==ayah.toString()
}

fun getTranslationAyahText(fileName: String, surah: Int, ayah: Int): String {
    // same logic but reads assets/quran/<fileName> (e.g. "en.hilali.txt")
}
```

### Search

`QuranSearchActivity` scans the master text file line-by-line, applying `RemoveTashkeel` to both query and text for accent-insensitive matching. The hint string in `values/strings.xml` is:

```
<string name="hint_search_quran">بحث : بإسم السورة "هود 10" أو بآية "سبح"</string>
```

(Users can search by surah name like `"هود 10"` or by ayah text like `"سبح"`.)

### Ayah limit

`R.string.error_limit = "You can select up to 50 Ayahs at a time"` — the editor enforces a 50-ayah cap per project to keep export time bounded.

---

## 14. Localization

### Supported locales

`res/xml/locales_config.xml` declares **9 supported locales** (matches the 9 translation files in `assets/quran/`):

```xml
<locale-config>
  <locale android:name="en"/>   <!-- default -->
  <locale android:name="ar"/>
  <locale android:name="fr"/>
  <locale android:name="de"/>
  <locale android:name="id"/>
  <locale android:name="ur"/>
  <locale android:name="fa"/>
  <locale android:name="tr"/>
  <locale android:name="bn"/>
</locale-config>
```

### String resources

| Directory | Notes |
|---|---|
| `values/` | Default (English). `app_name = "NurMontage"`. |
| `values-ar/` | Arabic UI translation. |

Note: only **English** and **Arabic** UI strings are bundled. The other 7 languages (fr, de, id, ur, fa, tr, bn) are supported at the **Quran-translation layer only** (the user can pick which translation text appears in their video). The app UI does not auto-translate to those 7 languages — `ChoiceLangActivity` switches both app locale via `LocaleHelper.onAttach` *and* which translation text is loaded from assets.

### Key strings (sample)

```xml
<string name="app_name">NurMontage</string>
<string name="export">Export</string>
<string name="cancel">Cancel</string>
<string name="done">Done</string>
<string name="quran">Quran</string>
<string name="bg">Bg</string>
<string name="ipad">Ipod</string>            <!-- note: "Ipod", not "iPad" — preserved typo -->
<string name="delete">Delete</string>
<string name="cut">Cut</string>
<string name="edit">Edit</string>
<string name="color">Color</string>
<string name="duplicate">Duplicate</string>
<string name="font">Font</string>
<string name="icon">Icon</string>
<string name="animtion">Effect</string>       <!-- preserved typo: "animtion" -->
<string name="echo">Echo</string>
<string name="reverb">Reverb</string>
<string name="fade">Fade</string>
<string name="speed">Speed</string>
<string name="volume">Volume</string>
<string name="enhance">Enhance</string>
<string name="replace">Replace</string>
<string name="audio">Audio</string>
<string name="video">Video</string>
<string name="image">Image</string>
<string name="from_the_start">from the start</string>
<string name="from_now">from now</string>
<string name="until_now">until now</string>
<string name="until_the_end">until the end</string>
<string name="applyall">ApplyAll</string>
<string name="search">Search</string>
<string name="settings">Settings</string>
<string name="about">About</string>
<string name="create_video">Create Video</string>
<string name="extract_audio">Extract audio</string>
<string name="error_limit">You can select up to 50 Ayahs at a time</string>
<string name="prgress_hint">Please don\'t lock the screen or switch to other apps.</string>  <!-- preserved typo -->
```

---

## 15. Resources & Assets

### `assets/`

| Path | Contents |
|---|---|
| `assets/dexopt/baseline.prof` (3.9 KB) | Android Baseline Profile — speeds up cold start. |
| `assets/dexopt/baseline.profm` (276 B) | Baseline profile metadata. |
| `assets/fonts/` (12 files) | Latin fonts: `ReadexPro_Bold.ttf`, `ReadexPro-Regular.ttf`, `ReadexPro_Medium.ttf`, `Poppins-Regular.ttf`, `Alegreya-Regular.ttf`, `NotoSans.ttf`, `NotoNaskhArabic.ttf`, `surah_name.otf`, `خط الاستعاذه.ttf`, plus 3 more. |
| `assets/fonts/arabic/` (62 files) | Arabic calligraphy fonts: Uthmani (`عثماني.otf`), Kufi (`كوفي.ttf`), Naskh (`نسخ.ttf`), Thuluth (`الثلث مزخرف.ttf`), Qalam (`القلم.ttf`), Mishaf (`المصحف.ttf`), Hafs (`خط حفص.ttf`), Warsh (`خط ورش.ttf`), Farsi (`الفارسي.ttf`), … |
| `assets/quran/` (10 files) | Quran text + 9 translations (see §13). |

### `res/`

| Directory | Count | Notes |
|---|---:|---|
| `layout/` | 86 | Default layouts |
| `layout-ar/` | 20 | RTL-specific layouts |
| `layout-sw600dp/` | — | Tablet (7"+) layouts |
| `drawable/` | 349 | Vector + raster drawables |
| `drawable-anydpi/`, `drawable-hdpi/`, `drawable-xhdpi/`, `drawable-xxhdpi/`, `drawable-ldrtl-xxhdpi/` | — | Density-specific & RTL variants |
| `mipmap-{anydpi-v26,hdpi,mdpi,xhdpi,xxhdpi,xxxhdpi}/` | — | App launcher icons |
| `values/` | — | Default strings, colors, dimensions, themes, styles |
| `values-ar/` | — | Arabic UI strings |
| `values-night/` | — | Dark-mode overrides |
| `values-port/`, `values-land/` | — | Portrait / landscape overrides |
| `values-small/`, `values-large/`, `values-xlarge/` | — | Screen-size overrides |
| `values-sw600dp/` | — | 600dp smallest-width (tablets) |
| `values-h320dp-port/`, `values-h360dp-land/`, `values-h480dp-land/`, `values-h550dp-port/`, `values-h720dp/` | — | Height-specific overrides |
| `values-w320dp-land/`, `values-w360dp-port/`, `values-w400dp-port/`, `values-w600dp-land/` | — | Width-specific overrides |
| `values-v27/`, `values-v28/`, `values-v31/` | — | API-level overrides (v31 = Android 12 Material You) |
| `color/`, `color-night/`, `color-v31/` | — | Color state lists |
| `anim/`, `animator/`, `interpolator/` | — | Animations |
| `font/` | — | XML font families |
| `menu/` | — | Options menus |
| `raw/` | — | Raw resources |
| `xml/` | — | `file_pathes.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `locales_config.xml`, `network_security_config.xml` |

---

## 16. Build & Signing

### `app/build.gradle.kts` highlights

```kotlin
android {
    namespace = "hazem.nurmontage.videoquran"
    compileSdk = 35

    defaultConfig {
        applicationId = "hazem.nurmontage.videoquran"
        minSdk = 24
        targetSdk = 35
        versionCode = 21000200
        versionName = "6.7.1-QuranMaker"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("/home/z/my-project/download/quranmaker-release.jks")
            storePassword = "quran123"
            keyAlias = "quranmaker"
            keyPassword = "quran123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false                          // ⚠️ ProGuard disabled
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES", "META-INF/LICENSE",
                "META-INF/LICENSE.txt", "META-INF/license.txt",
                "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
        jniLibs { useLegacyPackaging = true }
    }
}
```

### ⚠️ Build caveats

1. **Signing config is hardcoded to a specific path** (`/home/z/my-project/download/quranmaker-release.jks`). Anyone forking this project **must** change this path or move it to `local.properties` / environment variables. The keystore password (`quran123`) and alias (`quranmaker`) are committed in plain text — **do not use this keystore for production**.
2. **`gradle.properties` sets `org.gradle.java.home=/home/z/jdk/jdk-21.0.2`** — also hardcoded to a specific developer machine. Override this in `~/.gradle/gradle.properties` or remove the line to use the system JDK.
3. **`isMinifyEnabled = false`** for release — APK size is larger than necessary. ProGuard rules (`proguard-rules.pro`) are present but unused.
4. **`ffmpeg-kit-full-6.0-2.aar`** is **50.7 MB** — above GitHub's recommended 50 MB file size. Consider Git LFS or fetching from Maven Central.
5. **Java 17 required** for AGP 8.7.3.

### How to build

```bash
# 1. Clone
git clone https://github.com/mohamedsamysaadoun3/QuranMaker-Kotlin-Latest.git
cd QuranMaker-Kotlin-Latest

# 2. (Optional) Override JDK path
echo "org.gradle.java.home=$(/usr/libexec/java_home -v 17)" >> gradle.properties

# 3. (Required for release builds) Move keystore or edit signingConfigs
# Edit app/build.gradle.kts: change storeFile path

# 4. Build debug APK
./gradlew assembleDebug

# 5. Output
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 17. Permissions & Manifest

### Permissions (`AndroidManifest.xml`)

| Permission | Notes |
|---|---|
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | Pre-Android 13 storage access |
| `WRITE_EXTERNAL_STORAGE` (maxSdk 32) | Pre-Android 13 storage access |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Android 14+ partial-access media picker |
| `READ_MEDIA_VIDEO` | Android 13+ |
| `READ_MEDIA_IMAGES` | Android 13+ |
| `READ_MEDIA_AUDIO` | Android 13+ |
| `INTERNET` | Required for Pixabay search + remote audio URLs |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |
| `POST_NOTIFICATIONS` | Android 13+ export-progress notifications |
| `VIBRATE` | Haptic feedback |
| `hazem.nurmontage.videoquran.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Custom signature-protected permission for dynamic broadcast receivers |

### `<queries>` (inter-app intents)

```xml
<queries>
    <intent> <action android:name="android.intent.action.SENDTO"/> <data android:scheme="mailto"/> </intent>
    <package android:name="com.google.android.gm"/>             <!-- Gmail -->
    <package android:name="com.google.android.youtube"/>        <!-- YouTube -->
    <package android:name="com.zhiliaoapp.musically"/>          <!-- TikTok -->
    <package android:name="com.whatsapp"/>                      <!-- WhatsApp -->
    <package android:name="com.instagram.android"/>             <!-- Instagram -->
    <package android:name="hazem.tuffah.quranaudio"/>           <!-- Sister Quran-audio app -->
</queries>
```

### Activities (18 total)

| Category | Activities |
|---|---|
| **Launcher / splash** | `ui.splash.FullscreenActivity` (LAUNCHER), `ui.splash.SplashscreenActivity` |
| **Project gallery / home** | `ui.home.WorkUserActivity` (exported), `ui.home.YoutuberActivity` |
| **Main editor** | `ui.engine.EngineActivity` |
| **Render / export** | `ui.render.ProgressViewActivity` (exported) |
| **Media preview** | `ui.editor.VideoViewActivity` (exported), `VideoPlayerActivity`, `PlayVideoActivity`, `ChoiceBgFromVideoActivity` |
| **Media pickers** | `ui.gallery.GalleryPickerVideo`, `ui.gallery_photos.GalleryPickerOneImage` |
| **Text / name / crop editors** | `ui.editor.text.TextEditActivity`, `EditSNameActivity`, `EditTrslTxtActivity`, `ui.editor.audio.AddReaderNameActivity`, `CropBitmapActivity` |
| **Free-form layers** | `ui.editor.FreeLayerActivity` |
| **Search** | `ui.search.QuranSearchActivity`, `ui.search.PixabaySearchActivity` |
| **Inbound share** | `ui.share.ShareWithMeActivity` (exported; accepts `image/*`, `audio/*`, `video/*`) |
| **Settings & info** | `ui.settings.SeettingActivity`, `AboutActivity`, `ChoiceLangActivity`, `ThanksYouActivity` |

### Other manifest components

- **`androidx.core.content.FileProvider`** with authority `@string/file_provider` and paths from `@xml/file_pathes` (external-path, external-cache-path, external-files-path).
- **`androidx.startup.InitializationProvider`** auto-initializes `EmojiCompatInitializer`, `ProcessLifecycleInitializer`, `ProfileInstallerInitializer`.
- **`AppLocalesMetadataHolderService`** for per-app locale storage (`autoStoreLocales=true`).
- **`ProfileInstallReceiver`** for baseline-profile installation.
- **Application class**: `hazem.nurmontage.videoquran.core.App`.
- **Theme**: `@style/Theme.NurMontage` (default), `@style/Theme.NurMontage.Starting` (splash), `@style/App.VideoPlayer` (video player), `@style/Theme.App.Fullscreen` (render progress).
- **`android:enableOnBackInvokedCallback="true"`** — Android 13+ predictive back.
- **`android:usesCleartextTraffic="true"`** at application level (but tightened by `network_security_config.xml` — see §18).
- **`android:preserveLegacyExternalStorage="true"`** + **`android:requestLegacyExternalStorage="true"`** — for backward compatibility with Android 10/11 storage.
- **`android:extractNativeLibs="true"`** — required for FFmpegKit native libs.
- **`android:supportsRtl="false"`** — RTL is handled manually via `layout-ar/` and `LocaleHelper`.

### Billing / Pro / Ads — REMOVED

A manifest comment explicitly states:

```xml
<!-- ═══════════════════════════════════════════════════ -->
<!-- Billing / Pro / Ads — REMOVED (free, clean app)    -->
<!-- ═══════════════════════════════════════════════════ -->
```

This fork has the entire monetization layer stripped.

---

## 18. Network & Security

### `res/xml/network_security_config.xml`

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">commondatastorage.googleapis.com</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

- **HTTPS required by default** (`cleartextTrafficPermitted="false"` in `base-config`).
- **Cleartext HTTP allowed only for `commondatastorage.googleapis.com`** (Firebase Storage / Google Cloud Storage) — used for reciter audio URLs.
- Note: the manifest declares `android:usesCleartextTraffic="true"` at the application level, but the `network_security_config.xml` overrides this with a stricter policy.

### `res/xml/backup_rules.xml` & `data_extraction_rules.xml`

Both include all SharedPreferences and exclude `billing_prefs.xml` (a leftover from the removed billing layer — kept for backward compatibility).

```xml
<full-backup-content>
    <include domain="sharedpref" path="."/>
    <exclude domain="sharedpref" path="billing_prefs.xml"/>
</full-backup-content>
```

### `res/xml/file_pathes.xml` (FileProvider paths)

```xml
<paths>
    <external-path name="external_files" path="." />
    <external-cache-path name="external_cache_files" path="." />
    <external-files-path name="external_files_files" path="." />
</paths>
```

---

## 19. ProGuard Rules

`app/proguard-rules.pro`:

```proguard
# QuranMaker ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class hazem.nurmontage.videoquran.model.** { *; }
-keep class hazem.nurmontage.videoquran.entity_timeline.** { *; }
-keep class hazem.nurmontage.videoquran.common.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
-dontwarn com.pairip.**
```

Key points:
- **Models and timeline entities are kept** — required because they're serialized via Java `ObjectOutputStream` (reflection-free, but class names/fields must match).
- **FFmpegKit warnings suppressed** — the AAR is included via local Maven repo; some of its transitive classes may not be present at compile time.
- **`com.pairip.**` warnings suppressed** — Pairip is a Google Play app-integrity library; the original APK was protected with it, but it's not a dependency in this clean fork.
- **Release builds do not actually run ProGuard** (`isMinifyEnabled = false`), so these rules are inactive. They are present for when minification is enabled.

---

## 20. Naming Quirks (Important)

The original APK was reverse-engineered via **JADX**, which preserved several typos from the original Java source. These are **intentionally kept** in the Kotlin rewrite for fidelity:

| Quirk | Where | Note |
|---|---|---|
| `SeettingActivity` | `ui/settings/SeettingActivity.kt` | Typo for "Setting" |
| `*Adabter` suffix | `adapter/AyaAdabter.kt`, `WorkUserAdabter.kt`, `YoutuberAdabter.kt`, … (31 files) | Typo for "Adapter" — used consistently |
| `Feadback` | `utils/Feadback.kt` | Typo for "Feedback" |
| `animtion` | `<string name="animtion">Effect</string>` | Typo for "animation" |
| `prgress_hint` | `<string name="prgress_hint">…` | Typo for "progress" |
| `CustumFont` | `TextCustumFont`, `EditTextCustumFont`, `ButtonCustumFont`, … | Typo for "Custom" |
| `Ipod` (string) | `<string name="ipad">Ipod</string>` | iPad referred to as "Ipod" in default strings |
| `Hint_search_quran` Arabic | `بحث : بإسم السورة "هود 10" أو بآية "سبح"` | Arabic UI text in default (English) strings |

When renaming or refactoring, **search for these typos first** — they appear in many `R.layout.*`, `R.id.*`, and `R.string.*` references.

---

## 21. For AI / Developers — Quick Map

If you're an AI or a new developer looking at this codebase for the first time, here's where to start:

### "Where is X?" cheat sheet

| Looking for | Look at |
|---|---|
| App entry point | `core/App.kt` (Application) → `ui/splash/FullscreenActivity.kt` (Launcher) |
| Project save format | `model/Template.kt` + `utils/LocalPersistence.kt` |
| Main editor | `ui/engine/EngineActivity.kt` (shell) + 14 sibling extension files |
| Canvas drawing | `views/TrackEntityView.kt` + `views/track/TrackEntityRenderer.kt` |
| Touch / gestures | `views/track/TrackEntityTouchHandler.kt` + `multitouch/` |
| FFmpeg command builder | `ui/render/ExportCommandBuilder.kt` (1,446 lines) |
| FFmpeg runner | `export/SmartExportManager.kt` |
| Export flow orchestrator | `ui/engine/ExportPipeline.kt` (776 lines) |
| Quran text loading | `utils/QuranReader.kt` + `assets/quran/quran-simple.txt` |
| Quran search | `ui/search/QuranSearchActivity.kt` + `utils/RemoveTashkeel.kt` |
| Audio effects (reverb/echo) | `audio/MasjidReverbFilter.kt`, `audio/PitchCorrector.kt`, `fragment/audio_effect/` |
| Reciter audio download | `utils/audio/AudioUtils.kt` |
| Settings storage | `utils/QuranPreference.kt`, `utils/MyPreferences.kt` |
| Locale switching | `utils/LocaleHelper.kt` + `ui/settings/ChoiceLangActivity.kt` |
| Constants (fonts/colors/dims) | `core/common/Constants.kt` |
| Crash logs | `core/CrashHandler.kt` → `getExternalFilesDir(null)/crash_log.txt` |
| Baseline profile | `assets/dexopt/baseline.prof{,m}` + `androidx.profileinstaller` |
| Pixabay image search | `ui/search/PixabaySearchActivity.kt` |
| Inbound share (videos/images/audio) | `ui/share/ShareWithMeActivity.kt` |
| List adapters (typos!) | `adapter/*Adabter.kt` (note: "Adabter" with 'b') |

### Key constants to know

- **Editor FPS**: `25` (`EngineActivity.companion.FPS`)
- **Export FPS**: `30` (default in `Template.fps`)
- **Export resolution**: `720p` default (`Template.resolution`)
- **Max ayahs per project**: `50` (enforced in `AddQuranFragment`, message `R.string.error_limit`)
- **Aspect ratios** (`constant/ResizeType.kt`): Story, Square, Landscape, …
- **iPad frame types** (`constant/IpadType.kt`): Neumorphic, Glass, Plain, …

### What's intentionally NOT here

- No `ViewModel`, `UseCase`, `Repository` — this is not MVVM/Clean.
- No `Room`, `SQLite`, `Realm`, or any database.
- No DI framework (no Hilt, no Dagger, no Koin).
- No networking library (no Retrofit, no OkHttp) — uses raw `HttpURLConnection`.
- No JSON parsing for project files (uses Java `Serializable`).
- No billing, no ads, no analytics, no crashlytics (all stripped).

### Common debugging entry points

1. **App crashes on launch** → check `getExternalFilesDir(null)/crash_log.txt` (written by `CrashHandler`).
2. **Export fails** → `SmartExportManager` logs to logcat with tag `SmartExport`. The last FFmpeg command is in `ExportCommandBuilder.lastOverlayFilter`.
3. **Audio won't load** → `AudioUtils.copyToLocalAsync` callback receives the error; check network connectivity and URL validity (HTTPS enforced except for `commondatastorage.googleapis.com`).
4. **Quran text missing** → `assets/quran/quran-simple.txt` must be present; `QuranReader.getAyahText` returns empty string if the line isn't found.
5. **Layout looks wrong on tablet** → check the 21 `values-*` qualifier directories; the sw600dp / xlarge / w600dp-land variants override phone defaults.

### Suggested refactoring priorities (if you intend to modernize)

1. **Move signing config** out of `build.gradle.kts` into `local.properties` or env vars.
2. **Remove hardcoded JDK path** from `gradle.properties`.
3. **Enable R8/ProGuard** (`isMinifyEnabled = true`) — rules already exist.
4. **Migrate `LocalPersistence`** from Java `Serializable` to `kotlinx.serialization` (JSON) or `Parcelable` — current approach crashes if any model field is renamed.
5. **Introduce a `ViewModel`** for `EngineActivity` — the extension-function split helps but a proper `ViewModel` would survive configuration changes.
6. **Replace `HttpURLConnection`** with OkHttp or Retrofit for Pixabay + audio downloads.
7. **Add unit tests** — currently only stub test dependencies are declared.
8. **Fix the typos** (`Adabter` → `Adapter`, `Seetting` → `Setting`, `Feadback` → `Feedback`) if you don't need to preserve JADX fidelity.
9. **Move FFmpeg AAR** to Maven Central or Git LFS (it's 50.7 MB).
10. **Add an issue template & CONTRIBUTING.md** — currently none exist.

---

## 22. License & Credits

### License
This is a **proprietary** codebase. The original `NurMontage` app is © Hazem (developer of `hazem.tuffah.quranaudio` and related apps). This repository is a Kotlin rewrite/port for personal/educational use. No explicit license file is included.

### Credits
- **Original app**: NurMontage by Hazem (`hazem.nurmontage.videoquran`)
- **Kotlin rewrite**: This fork strips billing/ads and ports the JADX-decompiled Java to idiomatic Kotlin where possible (while preserving the original class structure for fidelity).
- **Quran text**: Bundled under `assets/quran/` — Uthmani-simple Arabic, plus 9 translations (Muyassar, Hilali-Khan, Hamidullah, Bubenheim, Maududi, Fooladvand, Öztürk, Indonesian, Bengali).
- **Fonts**: 74 fonts bundled under `assets/fonts/` — Uthmani, Kufi, Naskh, Thuluth, and many more Arabic calligraphy styles.
- **Libraries**: FFmpegKit, ExoPlayer (Media3), Glide, Konfetti, Material 3, AndroidX (core, appcompat, activity, fragment, lifecycle, splashscreen, emoji2, profileinstaller, window, preference, recyclerview, viewpager2, constraintlayout).

### Sister app
The manifest queries `hazem.tuffah.quranaudio` — the developer's Quran-audio app. QuranMaker can integrate with it for reciter audio.

### Repository
- **GitHub**: [mohamedsamysaadoun3/QuranMaker-Kotlin-Latest](https://github.com/mohamedsamysaadoun3/QuranMaker-Kotlin-Latest)
- **Source archive date**: 2026-06-01
- **Total files**: 1,305
- **Kotlin source files**: 271
