# Napkin Runbook — Consumo Energético

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
1. **[2026-11-23] Gradle needs JAVA_HOME set in every PowerShell session**
   Do instead: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` before any `.\gradlew.bat` call.
2. **[2026-11-23] Gradle output mangled without plain console**
   Do instead: always append `--console=plain` and never run two Gradle invocations at once.
3. **[2026-11-23] Full-suite loop before claiming done**
   Do instead: run `.\gradlew.bat testDebugUnitTest assembleDebug --console=plain` until `BUILD SUCCESSFUL` with 0 failures; never report success from a partial run.
4. **[2026-11-23] Room schema bump requires exported JSON**
   Do instead: after changing `@Database(version = N)`, keep `app/schemas/com.steefwel28.consumoenergetico.data.local.db.ConsumoDatabase/N.json` and a `Migration(N-1, N)` in `ConsumoDatabase.kt`.
5. **[2026-11-23] Haze version must match the project Kotlin**
   Do instead: keep `haze = "1.7.3"` (built with Kotlin 2.3.20 stdlib) while `kotlin = "2.3.21"`; Haze 2.x/stdlib 2.4 breaks the 2.3 compiler. Verify with the artifact POM before upgrading.

## Shell & Command Reliability
1. **[2026-11-23] `strings.xml` files are long; full reads burn context**
   Do instead: read only the last 25–30 lines of each `values*/strings.xml` when adding/verifying keys.
2. **[2026-11-23] Main thread limited to 5 direct file reads**
   Do instead: delegate file inspection and line search to a subagent; keep the main thread for edits and build runs.
3. **[2026-11-23] `12 %%` renders literally outside `String.format`**
   Do instead: use a single `%` in non-formatted strings (`calc_sim_filters_hint`); keep `%%` only where the string is passed through `String.format`.
4. **[2026-11-23] Git diff warnings about LF→CRLF are noise**
   Do instead: ignore `LF will be replaced by CRLF` lines in `git diff` output.

## Domain Behavior Guardrails
1. **[2026-11-23] Long-form prose in narrative engines is asserted verbatim by unit tests**
   Do instead: keep Spanish wording in `DiagnosticNarrativeBuilder`, `ElectricalSpecAdvisor`, `AcSpaceRecommender` intact unless the test is updated in the same commit.
2. **[2026-11-23] BTU validation upper bound is 60,000 BTU/h**
   Do instead: keep `canSave` / `RtcaLabelParser` limits at 60,000 to accept real equipment like 36,679 BTU/h.
3. **[2026-11-23] Offline-first constraint**
   Do instead: no Retrofit/OkHttp/Firebase/PDF libs; the only network call allowed is `HttpURLConnection` (4s timeout) to the GitHub releases API.
4. **[2026-11-23] Splash haptics must match the 2 s timeline**
   Do instead: stage 1 `tick(0.25f)`, stage 2 `tick(0.45f)`+`tick(0.52f)`, stage 3 ramp `tick(0.58f..1.0f)` then `pop()` at 2000 ms with `1.0f → 1.12f` bounce.

## Compose UI Guardrails
1. **[2026-11-23] Liquid Glass only blurs inside the main window**
   Do instead: use `LiquidGlassSurface` / `Modifier.liquidGlassEffect()` for cards and bars; inside `ModalBottomSheet`, `Dialog` or `DropdownMenu` wrap with `CompositionLocalProvider(LocalHazeState provides null)` so Haze never samples another window.
2. **[2026-11-23] Glass mode forces a dark scheme**
   Do instead: `ConsumoEnergeticoTheme(liquidGlass = true)` darkens `background/surface` and pins `onBackground/onSurface = #F3FBF8`, `onSurfaceVariant = #B6D8D0`; never let the Día scheme paint black titles over orbs.
3. **[2026-11-23] `Float * Dp` does not compile**
   Do instead: write `dp * float` (e.g. `8.dp * (1f - eased)`), never `(1f - eased) * 8.dp`.
4. **[2026-11-23] `CHART_ITEM_INDEX` must match the LazyColumn item order in `CalculateScreen`**
   Do instead: keep Comparativa at index 3 and Datos del equipo at 4 (both category branches); re-check the constant at the bottom of the file after any reorder.
5. **[2026-11-23] Splash chips need their data class**
   Do instead: keep `SplashChip(alignment, x, y, width, height, color, maxAlpha, revealAtMs)` private in `ExpressiveSplashScreen.kt`; morph keyframes 0→1→2→3 at 0/600/1200/2000 ms.
6. **[2026-11-23] Liquid Glass orbes follow scroll via root `nestedScroll`**
   Do instead: keep `LiquidGlassScrollConnection` in `LiquidGlassBackground.kt` + `.nestedScroll(...)` on `ConsumoApp` content Box; no scroll wiring needed in screens.
7. **[2026-11-23] Lens/refraction only on floating layers, never on content cards**
   Do instead: use `Modifier.liquidGlassLens()` (AGSL `RuntimeShader`, API 33+) via `LiquidGlassSurface(lens = true)` for nav bar / sheets / ☰ menu; inside `ModalBottomSheet`/`Dialog` haze cannot sample other windows, so the lens tint is the only glass there.
8. **[2026-11-23] Turning Liquid Glass off leaves an emerald window + orbs**
   Do instead: keep all three shields — `LiquidGlassBackground` early `return` when `!LocalLiquidGlass.current`, `ConsumoApp` Box `.background(colorScheme.background)` when not glass, and `MainActivity` re-writing `window.setBackgroundDrawable(...)` in `LaunchedEffect(appearance)`.

## User Directives
1. **[2026-11-23] Signature and donation identity**
   Do instead: sign as `Roberto Franco` (`steefwel28`); Yappy number `6328-5079`; keep `BuildConfig.AUTHOR_SIGNATURE` and the manifest `com.steefwel28.signature` meta-data.
2. **[2026-11-23] Placeholders show `Hisense`**
   Do instead: brand/model placeholders always start with `Hisense` (e.g. `Hisense ATR122HA`).
3. **[2026-11-23] Explain gaps at the end of every reply**
   Do instead: close with 1–2 lines on what was skipped and the risk the user should know.
