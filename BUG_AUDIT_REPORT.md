# QuranMaker-Kotlin — Bug Audit Report

**Date**: 2026-07-06
**Scope**: Full codebase (271 Kotlin files, 1305 total files)
**Methodology**: Android Lint (298 errors + 2687 warnings) + grep pattern scan (5042 hits) + AI deep audit (5 agents × ~12 files each = ~60 files reviewed in depth)

## Executive Summary

- **Total bugs found**: **106** (across 5 audit passes)
- **P0 (critical)**: 11 bugs — crashes, ANR, data corruption
- **P1 (high)**: 36 bugs — leaks, likely-crashes, wrong behavior
- **P2 (medium)**: 41 bugs — edge cases, jank, fragile code
- **P3 (low)**: 18 bugs — code smells, dead code, lint

**Project builds successfully** (`./gradlew assembleDebug` OK on JDK 17 + AGP 8.7.3 + Kotlin 1.9.25 + Android SDK 35).

---

## P0 — Critical (must fix)

### Engine (1 P0)

#### BUG-E01 · `EngineActivity.onRequestPermissionsResult` missing `super` call
- **File**: `ui/engine/EngineActivity.kt:315`
- **Class**: LifecycleBug
- **Lint**: `MissingSuperCall`
```kotlin
override fun onRequestPermissionsResult(i: Int, strArr: Array<String>, iArr: IntArray) {
    handleRequestPermissionsResult(i, strArr, iArr)
    // missing: super.onRequestPermissionsResult(...)
}
```
- **Why**: Fragment permission callbacks never fire → fragment-based permission requests silently fail.
- **Fix**: Add `super.onRequestPermissionsResult(i, strArr, iArr)`.

### Render/Export (0 P0)

### Views (5 P0)

#### BUG-V01 · `BlurredRenderer.onDrawExt` canvas state leaks via swallowed exception
- **File**: `views/blurred/BlurredRenderer.kt:139-263`
- **Class**: Canvas state leak → eventual crash
- **Why**: `canvas.save()` at line 145, `canvas.restore()` at line 249. If anything between throws (NPE on `rectFProgress!!`, `selectTool!!`, `iViewCallback!!`, `entity_select!!`), the catch swallows it and `restore()` is skipped. Save-count grows by 1 per dropped frame until `IllegalStateException` (save count > 255) crashes the view.
- **Fix**:
```kotlin
val sc = canvas.save()
try { /* draw */ } finally { canvas.restoreToCount(sc) }
```

#### BUG-V02 · `BlurredRenderer.onDrawExt` double `onDrawFinish()` callback per frame
- **File**: `views/blurred/BlurredRenderer.kt:251-263`
- **Class**: Callback leak
```kotlin
// inside try:
if (isPlaying() && this.iViewCallback != null) {
    this.iViewCallback!!.onDrawFinish()      // call #1
}
} catch (e: Exception) { … }
finally {
    if (isPlaying() && this.iViewCallback != null) {
        this.iViewCallback!!.onDrawFinish()  // call #2 — always runs
    }
}
```
- **Why**: `finally` runs even on successful try → host receives `onDrawFinish()` twice per frame. Downstream code that advances playback state double-steps.
- **Fix**: Remove the call from inside the try block.

#### BUG-V03 · `TrackEntityView` pinch gesture dead-locks after `ACTION_CANCEL`
- **File**: `views/track/TrackEntityTouchHandler.kt:17-27`
- **Class**: Touch event state machine
```kotlin
if (isScaleListener) {
    if (motionEvent.action == MotionEvent.ACTION_UP) isScaleListener = false
    return true  // swallows all events until ACTION_UP
}
```
- **Why**: `isScaleListener` latched `true` by `onScaleBegin`, only cleared on `ACTION_UP`. If parent intercepts (system gesture, multi-window), gesture ends with `ACTION_CANCEL` (no branch) → latch stays `true` → every subsequent `ACTION_DOWN` returns `true` without reaching `gestureDetector` → **timeline editor becomes unresponsive to touch** until user pinches again.
- **Fix**:
```kotlin
if (motionEvent.action == MotionEvent.ACTION_UP ||
    motionEvent.action == MotionEvent.ACTION_CANCEL) {
    isScaleListener = false
    isMove = false; isAutoScroll = false; isAutoMove = false
    isPassScroll = true; eventX = 0f; eventY = 0f
}
```

#### BUG-V04 · `CropView.setBitmapLast` matrix never assigned + trivial scale
- **File**: `views/CropView.kt:146-171`
- **Class**: State assignment + math
```kotlin
this.mWidth = mCanvasWidth              // line 154
val cropScale = mCanvasWidth / mWidth   // = 1.0f
val m = Matrix()
m.postScale(cropScale, cropScale)
m.postTranslate(0f, mDrawingY)
invalidate()                            // `this.matrix` NEVER assigned `m`!
```
- **Why**: (a) New matrix `m` computed but `this.matrix` not assigned → `onDraw` uses stale matrix. (b) `cropScale = mCanvasWidth / mWidth` is trivially 1.0 (just-assigned field) → likely intended `mCanvasWidth / bmp.width`.
- **Fix**: `this.matrix = m`. Change divisor to `bmp.width.toFloat()`.

#### BUG-V05 · `VideoFrameSelectorView.loadFrames()` runs `MediaMetadataRetriever` on main thread → ANR
- **File**: `views/VideoFrameSelectorView.kt:47-85`
- **Class**: Main-thread I/O
```kotlin
fun setVideoUri(uri: Uri?) {
    this.videoUri = uri
    loadFrames()      // main thread
    invalidate()
}
private fun loadFrames() {
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, videoUri)             // blocking I/O
    val dur = mmr.extractMetadata(9)                  // may be null
    val parseLong = Long.parseLong(dur) / frameCount  // NPE if dur==null
    for (i in 0 until frameCount) {
        mmr.getFrameAtTime(durMs, 2)                  // multi-hundred-ms decode ×7
    }
}
```
- **Why**: 7× `getFrameAtTime` on a 1080p video = multiple seconds of main-thread block → ANR. Null `dur` → `NumberFormatException` swallowed → empty selector, no error.
- **Fix**: Move to coroutine/Executor. Null-check `extractMetadata`. Surface error callback.

### Models (6 P0)

#### BUG-M01 · `QuranEntity` non-serializable fields not marked `@Transient`
- **File**: `model/QuranEntity.kt:54-71`
- **Class**: Serialization bug
```kotlin
internal var objectAnimator: ObjectAnimator? = null
private val paintAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
private val paintAyaOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
internal val paintTranslationAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
internal var staticLayout: StaticLayout? = null
internal var staticLayoutOutline: StaticLayout? = null
internal var staticLayoutTranslation: StaticLayout? = null
internal var staticLayoutTranslationOutline: StaticLayout? = null
private val typefaceNumber: Typeface?
internal var vectorDrawable: VectorDrawable? = null
```
- **Why**: `QuranEntity : Serializable`. `TextPaint`, `StaticLayout`, `Typeface`, `VectorDrawable`, `ObjectAnimator` are NOT Serializable. Any `writeObject(quranEntity)` throws `NotSerializableException`. Today the project uses Gson (which reflects and may produce garbage), but if any code path uses Java serialization, it crashes.
- **Fix**: Mark all listed `@Transient`. Add `restore()` hook to rebuild paints/staticLayouts after deserialization.

#### BUG-M02 · `BismilahEntity` same as M01
- **File**: `model/BismilahEntity.kt:36-46`
- **Why**: Same Paint/StaticLayout/ObjectAnimator pattern.
- **Fix**: Same as M01.

#### BUG-M03 · `TranslationQuranEntity` same as M01
- **File**: `model/TranslationQuranEntity.kt:44-57`
- **Why**: Same pattern. Author even declared `serialVersionUID = 1L` so intent was Java serialization.
- **Fix**: Same as M01.

#### BUG-M04 · `EntityView` base `RectF` and timeline refs leak into every subclass
- **File**: `model/EntityView.kt:9, 22, 24-25`
```kotlin
open var rect: RectF = RectF()                    // not Serializable
var copyRect: RectF? = null                       // not Serializable
open var entityQuran: EntityQuranTimeline? = null // not Serializable
var entityTrslTimeline: EntityTrslTimeline? = null
```
- **Why**: Every Serializable subclass (QuranEntity, BismilahEntity, TranslationQuranEntity) inherits these non-transient fields. Even after fixing M01-M03, `writeObject` throws `NotSerializableException: android.graphics.RectF`.
- **Fix**: Mark all `@Transient`. Persist `rect` as 4 floats. Re-init in `restore()`.

#### BUG-M05 · `EntityAudio.mediaPlayer` & friends not transient
- **File**: `entity_timeline/EntityAudio.kt:47, 50-56, 59, 61`
```kotlin
internal var mediaPlayer: MediaPlayer? = null   // not Serializable
internal var paintLine: Paint? = null
internal var paintPath: Paint? = null
internal var path: Path? = null
internal var renderer: WaveformBitmapRenderer? = null
internal var uri: Uri? = null                    // not Serializable
internal var amps: FloatArray? = null            // large
var waveformValues: ByteArray? = null            // large
```
- **Fix**: Mark all `@Transient`. Persist `uri` as `String`. Reinit in `restore()`.

#### BUG-M06 · `EntityMedia.duplicate()` drops render-critical state
- **File**: `model/EntityMedia.kt:139-157`
```kotlin
fun duplicate(): EntityMedia {
    return EntityMedia(
        uri!!, start_original, start, end, time,
        x, y, w, h, offset, isSoundEnable, max,
        duration_fade_in, duration_fade_out, posXFFmpeg
    )
}
```
- **Why**: 15-arg constructor sets only 15 of ~25 fields. Dropped: `path_ffmpeg` (renders fail!), `path_ffmpeg_effect`, `video_path`, `paths_https`, `posX`, `posY`, `mScale`, `volume`, `effectAudio` (audio effects lost!), `name`, `id_raw`, thumbnails. Also `uri!!` NPE if `uri` null.
- **Fix**: Field-by-field copy or serialize round-trip.

---

## P1 — High (likely crash / leak / wrong behavior)

### Engine (12 P1)

| ID | File:Line | Bug |
|---|---|---|
| E02 | `EngineActivity.kt:115, 302-309` | `executor` (single-thread) never shut down in `onDestroy` → thread + Activity leak |
| E03 | `EngineUIHelper.kt:1120-1124` | `id_ffmpeg.clear()` orphans running FFmpeg sessions (no `cancel()`) |
| E04 | `EngineSaveHelper.kt:269-501` + `EngineActivity.kt:286-294` | `saveTemplateTmp()` runs on main thread in `onPause` (heavy Gson serialization) |
| E05 | `EngineSaveHelper.kt:42-266` | UI mutations + `startActivity`/`finish` from `executor` thread |
| E06 | `EngineUIHelper.kt:228-405` | `initTypeVideo` UI mutations from `executor` + AudioUtils callback thread |
| E07 | `EngineUIHelper.kt:254, 1126` | FFmpeg callbacks fire after Activity destroy → `Glide.with(activity)` crash |
| E08 | `EngineAudioManager.kt:59, 88, 285, 400, 444, 488` | `mPlayer = MediaPlayer()` overwrites without `release()` → native leak |
| E09 | `EngineAudioManager.kt:417, 465, 512` | `entityAudio.mediaPlayer` overwritten without `release()` |
| E10 | `EngineAudioManager.kt:312-316` | `Executors.newSingleThreadExecutor()` per call → thread leak |
| E11 | `EngineEntityManager.kt:1031-1034` | `clearCallback()` is a no-op; fragment singletons retain Activity |
| E12 | `EngineSaveHelper.kt:33-266` | `oneExport = true` never reset on failure → infinite spinner |
| E13 | `EngineAudioManager.kt:1253-1334` | Inverted null check: `downloadFile = ...!!` then `if (downloadFile == null) downloadFile!!.replace(...)` → NPE; concat.txt always empty |

### Render/Export (5 P1)

| ID | File:Line | Bug |
|---|---|---|
| X01 | `ExportCommandBuilder.kt:512, 571, 590, 608, 651` | Missing `-loop 1` for PNG background inputs → 1-frame output (IPAD/HEART/BATTERY/CASSET branches) |
| X02 | `ProgressViewActivity.kt:100, 438, 756` | `id_ffmpeg` mutated concurrently → `ConcurrentModificationException` |
| X03 | `ProgressViewActivity.kt:633` | `dismissCancelDialog()` called from FFmpeg callback thread, not main |
| X04 | `RenderManager.kt:3-45` | `tasks`/`currentTaskIndex` not synchronized; concurrent `nextTask()` vs `getCurrentStepDuration()` |
| X05 | `SmartExportManager.kt:161` | Progress formula assumes 10s total: `time / 100f` → pinned at 99% for >10s videos |

### Audio/Utils (4 P1)

| ID | File:Line | Bug |
|---|---|---|
| A01 | `utils/QuranReader.kt:56-61` | `getTranslationAyahText` returns `"|In the Name..."` (leading pipe — off-by-one in substring) |
| A02 | `utils/audio/FfmpegCodecChecker.kt:23-34` | `detectCodecsAsync` delivers callback on FFmpeg thread, not main → UI crash |
| A03 | `utils/audio/AudioUtils.kt:123-152` | `copyFromUri` leaks `InputStream` if `getFileName` throws |
| A04 | `core/CrashHandler.kt:30-45` | `getExternalFilesDir(null)` may return null → `File(null, "crash_log.txt")` → FileNotFoundException; no fallback to `filesDir` |

### Views (11 P1)

| ID | File:Line | Bug |
|---|---|---|
| V06 | `views/track/TrackEntityRenderer.kt:23-58` | Canvas save/restore leak (same as V01, on timeline canvas) |
| V07 | `views/TrackEntityView.kt:1065` | No `onDetachedFromWindow` → animator + Handler runnables leak |
| V08 | `views/BlurredImageView.kt:750-760` | `reset()` recycles bitmaps but doesn't null fields → use-after-recycle crash |
| V09 | `views/BlurredImageView.kt:1061-1076` | `Paint()` + `Color.parseColor` per onDraw |
| V10 | `views/CassetteView.kt:36-133` | 4× `Path` + 4× `RectF` per onDraw (Lint `DrawAllocation`) |
| V11 | `views/track/TrackEntityRenderer.kt:345` | `RectF` alloc per onDraw |
| V12 | `views/track/TrackEntityRenderer.kt:81-115` | `String.format` per marker per frame |
| V13 | `views/track/TrackEntityRenderer.kt:142-261` | `ContextCompat.getDrawable` per frame ×5 |
| V14 | `views/VideoFrameSelectorView.kt:115-122` | `Rect` alloc per frame ×7 |
| V15 | `views/track/TrackEntityTouchHandler.kt:29-107` | `ACTION_UP` cleanup gated on `selectedEntity != null` → NPE later if entity was deleted mid-gesture |
| V16 | `views/track/TrackEntityRenderer.kt:295-467` | Host callbacks fired from inside `onDraw` |

### Models (4 P1)

| ID | File:Line | Bug |
|---|---|---|
| M07 | `model/Template.kt:81-96` | `duplicate()` swallows `NotSerializableException`, returns null → caller NPE on `!!` |
| M08 | `core/common/StackEntity.kt:5-15` | `index_start_thumbnail`/`index_end_thumbnail` not in constructor → default 0 → undo of trim silently corrupts thumbnail strip |
| M09 | `entity_timeline/Entity.kt:131-143, 272-300` | `setCurrentRect()` early-returns if `currentStackEntity != null` → stale snapshot after abandoned gesture |
| M10 | `entity_timeline/Entity.kt:290-312` | `undo()` pops 2 entries; if stack has 1 → `EmptyStackException` swallowed → stack off-by-one |

---

## P2 — Medium (41 bugs)

### Engine (10 P2)
- E14: `id_ffmpeg` `MutableList` race → CME
- E15: `updateFrame` modulo by zero if `duration_video_media==0`
- E16: `nameReaderResult` NPE on missing intent extras
- E17: `editSurahNameResult` NPE on null `surahNameEntity`
- E18: `editTrslResult` `ClassCastException` on `as TranslationQuranEntity`
- E19: `releaseWakeLock` clears `0x400` not `0x80` (FLAG_KEEP_SCREEN_ON)
- E20: Inconsistent field updates across `applyffect`/`applyffectAll`/`applyffectPlayAuto`
- E21: Possible 1000× unit error in `entityAudio.duration`
- E22: Editor FPS=25 hardcoded, export FPS=30 user-configurable — possible mismatch
- E23: `saveTemplateTmp` empty catch hides auto-save failures

### Render/Export (11 P2)
- X06: `isCancel` flag not `@Volatile`
- X07: Double `nextTask()` + semaphore over-release on `InterruptedException`
- X08: `prepareAllMedia` swallows download failures silently
- X09: Output framerate `-framerate` instead of `-r`
- X10: `clearFFmpeg()` calls global `FFmpegKit.cancel()` (kills all sessions)
- X11: `onDestroy` deletes template folder while FFmpeg may still be writing
- X12: `isApplyEffectInPreview` hardcoded false → dead branch
- X13: Timer countdown off by one second
- X14: `BLACK_LAYER`/`GRADIENT`/`MASK_BRUSH` with missing bg → undefined `[ov0]` label
- X15: `insertToGallery` uses deprecated `ACTION_MEDIA_SCANNER_SCAN_FILE` broadcast
- X16: `SmartExportManager.cancel()` cancels all FFmpeg sessions globally

### Audio/Utils (5 P2)
- A05: `QuranReader.getAyahText` re-reads entire 6236-line file per call (O(N²))
- A06: `LocalPersistence.readObjectFromFile` does Gson + SharedPrefs on main thread
- A07: `CrashHandler.readCrashLog` declares `String?` but can throw `IOException`
- A08: `FileUtils.getFileFromUri` uses deprecated `_data` column (broken on SDK 35)
- A09: `FileUtils.resolveDocumentUri` `split[1]` IOBE if docId has no `:`

### Views (9 P2)
- V17: `WaveformView.onTouchEvent` no `performClick` / `ACTION_CANCEL`
- V18: `CropView.onTouchEvent` no `performClick`; useless `actionIndex == 0`
- V19: `VideoFrameSelectorView.onTouchEvent` no `performClick` / `ACTION_CANCEL`
- V20: `VideoFrameSelectorView.loadFrames` unreachable catch; silent failure on null metadata
- V21: `TrackEntityView.computeScroll` doesn't abort scroller when clamped → wasted invalidate
- V22: `TrackEntityView.ScaleListener.onScale` redundant assignment; no `currentPosition` clamp
- V23: `TrackEntityManager` swallows exceptions in undo/redo/delete → state corruption
- V24: `calculMaxTimeExt` float `!=` fragility + potential divide-by-zero
- V25: `TrackEntityView` `Handler()` deprecated no-arg constructor + double init

### Models (7 P2)
- M11: `RenderManager.addTask` LIFO insertion → reversed task order
- M12: `RenderManager.computeWeights` early-returns when total=0 → progress stuck at 0
- M13: `EffectAudio` raw FFmpeg cmd strings unvalidated
- M14: `EntityAudio.split()` drops path, waveform, and effect state
- M15: `EntityQuranTemplate.copy()` shallow-copies `Transition?` and `MRectF?`
- M16: `EntityQuranTemplate.equals/hashCode` include mutable refs → broken HashSet semantics
- M17: `QuranEntity.getDuration_fade()` NPE on null `entityQuran`

---

## P3 — Low (18 bugs)

### Engine (4 P3)
- E24: `MissingSuperCall` (already P0 E01)
- E25: Empty catch blocks (3 sites in EngineActivity)
- E26: `clearFFmpeg` doesn't `clear()` the list
- E27: 7 refactored classes are dead code

### Render/Export (8 P3)
- X17: `amix` then `volume=2` likely clips
- X18: `BLUE_TYPE` overlay chain references shifted input indices
- X19: `getOrCreateMaskCircle`/`createTransparentBg` caching broken
- X20: `preRenderVideoHueArgs` lacks `-stream_loop -1`
- X21: `SmartExportManager.callback` not `@Volatile`
- X22: Notification auto-dismiss can cancel newer export's notification
- X23: `CodecOptimizer` unused, hardcodes `frameRate = 30`
- X24: `qtrle` pre-renders are lossless and huge (1GB+ for 60s 1080p)

### Audio/Utils (5 P3)
- A10: `CrashHandler.getStackTraceString` no cycle detection (infinite loop on cyclic cause)
- A11: `FfmpegCodecChecker.cachedCodecs` unsynchronized
- A12: `AudioUtils.downloadFile` filename collision risk
- A13: `LocaleHelper.onAttach` re-persists same language on every Activity attach
- A14: Dead code in `MasjidReverbFilter`, `PitchCorrector`, `TimeFormatter.timeToString`, `RemoveTashkeel.removeTashkeelAndPoint`, `FileUtils.getFileFromUri`

### Views (4 P3)
- V26: `drawIconDrawable` dead code with latent allocations
- V27: `VideoFrameSelectorView` uses `SupportMenu.CATEGORY_MASK` (restricted API)
- V28: `TrackEntityRenderer.drawItemBtnExt` duplicates Trsl block
- V29: `TrackEntityView.onDraw` calls `super.onDraw` AFTER extension

### Models (4 P3)
- M18: `EntityAudio.amps`/`waveformValues` should be transient
- M19: `RenderManager` has no thread synchronization
- M20: `EffectAudio` field-type inconsistency (`volume_echo: Int` vs `volume: Float`)
- M21: `QuranEntity.calculateOptimalTextSize` 33,000 iterations of StaticLayout builds

---

## Build Lint Errors (297 errors)

- **175** `ExtraTranslation` — Arabic strings overriding non-existent defaults (mostly abc_*/mtrl_*/exo_* from libraries)
- **85** `MissingDefaultResource` — dimens in qualifier dirs without base declaration
- **22** `WrongConstant` — wrong constants passed to `WindowInsets.Type`, `InputMethodManager.showSoftInput`, `setVisibility`
- **9** `MissingTranslation` — `app_name`, `dest_title`, `file_provider`, `post`, `language_changed` not in Arabic
- **3** `NewApi` — `CoreComponentFactory` requires API 28, `WindowInsets.Type.systemBars` API 30, `getLongVersionCode` API 28
- **2** `RestrictedApi` — `SupportMenu.CATEGORY_MASK` in VideoFrameSelectorView
- **1** `MissingSuperCall` — `EngineActivity.onRequestPermissionsResult`

---

## Fix Priority (recommended order)

1. **BUG-V01, V02, V03, V04, V05** — P0 view crashes (canvas leak, dead touch, broken crop, ANR)
2. **BUG-M06** — `EntityMedia.duplicate()` broken (blocks undo/redo of media)
3. **BUG-E13** — Inverted null check in `addAudioRecitersTemplateRunnable` (NPE + broken retry)
4. **BUG-A01** — Translation text returned with leading `|` (corrupt video output)
5. **BUG-E07, E08, E09** — MediaPlayer leaks (compounding over editing session)
6. **BUG-E01** — Missing `super.onRequestPermissionsResult`
7. **BUG-V07, V08** — View lifecycle leaks (animators not cancelled)
8. **BUG-E12, E04** — Export failure leaves infinite spinner; main-thread auto-save
9. **BUG-X01, X05** — FFmpeg `-loop 1` for PNG; progress math for >10s videos
10. **BUG-M01-M05** — `@Transient` annotations (latent today, triggers on first Java-serialization use)
