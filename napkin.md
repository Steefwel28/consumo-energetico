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
4. **[2026-11-28] Room FK migrations cannot use ALTER TABLE**
   Do instead: recreate the table (`*_new`), copy rows with explicit columns, drop, rename and recreate indices; bump `@Database(version)` and commit the exported `N.json`.
5. **[2026-11-23] Haze version must match the project Kotlin**
   Do instead: keep `haze = "1.7.3"` (built with Kotlin 2.3.20 stdlib) while `kotlin = "2.3.21"`; Haze 2.x/stdlib 2.4 breaks the 2.3 compiler. Verify with the artifact POM before upgrading.
6. **[2026-11-28] Backtick test names cannot contain a dot**
   Do instead: JVM method names reject `.`; write `inverter aplica el factor estacional` instead of `87.5 por ciento`, or Gradle fails with "Name contains illegal characters: .".

## Shell & Command Reliability
1. **[2026-11-23] `strings.xml` files are long; full reads burn context**
   Do instead: read only the last 25–30 lines of each `values*/strings.xml` when adding/verifying keys.
2. **[2026-11-23] Main thread limited to 5 direct file reads**
   Do instead: delegate file inspection and line search to a subagent; keep the main thread for edits and build runs.
3. **[2026-11-28] PowerShell `Set-Content -Encoding UTF8` adds a BOM to `strings.xml`**
   Do instead: write translations with `[System.IO.File]::WriteAllText(path, text, UTF8Encoding($false))`.
4. **[2026-11-23] `12 %%` renders literally outside `String.format`**
   Do instead: use a single `%` in non-formatted strings (`calc_sim_filters_hint`); keep `%%` only where the string is passed through `String.format`.
5. **[2026-11-23] Git diff warnings about LF→CRLF are noise**
   Do instead: ignore `LF will be replaced by CRLF` lines in `git diff` output.

## Domain Behavior Guardrails
1. **[2026-11-28] Potencia se estima en un solo lugar**
   Do instead: use `PowerEstimator.fromBtu(btu, seer, isInverter)` (`W = BTU/h ÷ EER`, Inverter `EER = 0.875 × SEER`); never reintroduce `BTU/(SEER*0.293)` or `btu/3.4`.
2. **[2026-11-28] Pliego ASEP vigente 1 sep–31 dic 2026**
   Do instead: ENSA fijo 2.76 / 0.17737 / 0.24666 / 0.27005; Naturgy EDEMET fijo 3.17 / 0.17624 / 0.23177 / 0.25027; EDECHI fijo 3.04 / 0.19175 / 0.25341 / 0.27486; show `validFrom`/`validTo` in UI.
3. **[2026-11-28] `LocaleNumber` must treat a leading zero as decimal**
   Do instead: `"0.180"` → `0.18`, while `"12.000"` stays `12_000`; only group thousands when the integer part is non-zero.
4. **[2026-11-23] Long-form prose in narrative engines is asserted verbatim by unit tests**
   Do instead: keep Spanish wording in `DiagnosticNarrativeBuilder`, `ElectricalSpecAdvisor`, `AcSpaceRecommender` intact unless the test is updated in the same commit.
5. **[2026-11-28] BTU validation upper bound is 60,000 BTU/h**
   Do instead: keep `canSave`, `MyHomeViewModel.saveUnit` and the edit dialog at `MAX_BTU_PER_HOUR` to accept real equipment like 36,679 BTU/h.
6. **[2026-11-23] Offline-first constraint**
   Do instead: no Retrofit/OkHttp/Firebase/PDF libs; the only network call allowed is `HttpURLConnection` (4s timeout) to the GitHub releases API.
7. **[2026-11-23] Physical coherence advisories must never touch kWh/B/.**
   Do instead: audit BTU/W/SEER only via `DiagnosticNarrativeBuilder.coherenceWarnings` (domain + tests). Per user directive (2026-11-28) the Calculate UI card and `CalculateUiState.coherenceWarnings` were removed; do not reintroduce them into UI.
8. **[2026-11-28] Backup JSON must carry `nominal_watts`**
   Do instead: write `"nominal_watts": null` when absent and read it with `optionalDouble` so v1–v4 backups still restore; never require the key.

## Compose UI Guardrails
1. **[2026-11-28] Haze only on floating layers, never on content cards**
   Do instead: `hazeSource` on the scrolling content Box and `LiquidGlassSurface`/`liquidGlassEffect` only on nav bar, menu and sheets; cards use solid `surfaceContainerLow`.
2. **[2026-11-28] Distributor/brand colors come from `LocalExtendedColors`, never `isSystemInDarkTheme()`**
   Do instead: `ExtendedColors(ensa, edemet, edechi, warningContainer, onWarningContainer)` provided by `ConsumoEnergeticoTheme`; screens read `LocalExtendedColors.current` (waves, duel bars, AboutCard uses M3 `primaryContainer` roles).
3. **[2026-11-28] Haptics use the system engine**
   Do instead: `LocalHapticFeedback.current` with `Confirm`, `Reject`, `SegmentTick`, `ToggleOn`/`ToggleOff`; no direct `Vibrator` and no `VIBRATE` permission.
4. **[2026-11-28] Splash haptics still follow the 2 s timeline**
   Do instead: stage 1 `SegmentTick`, stage 2 two `SegmentTick`, stage 3 ramp ticks, `Confirm` at 2000 ms with `1.0 → 1.12` bounce.
5. **[2026-11-23] `Float * Dp` does not compile**
   Do instead: write `dp * float` (e.g. `8.dp * (1f - eased)`), never `(1f - eased) * 8.dp`.
6. **[2026-11-23] `CHART_ITEM_INDEX` must match the LazyColumn item order in `CalculateScreen`**
   Do instead: keep Comparativa at index 3 and Datos del equipo at 4; re-check the constant after any reorder.
7. **[2026-11-23] Splash chips need their data class**
   Do instead: keep `SplashChip(...)` private in `ExpressiveSplashScreen.kt`; morph keyframes 0→1→2→3 at 0/600/1200/2000 ms.
8. **[2026-11-23] Fase 1 hides unsupported categories**
   Do instead: render chips from `EquipmentCategory.available` (only `AIR_CONDITIONER`); keep the full enum intact for v0.7.0.
9. **[2026-11-28] `LocalLiquidGlass` gates the Haze blur**
   Do instead: `LiquidGlassSurface` and `liquidGlassEffect()` return the solid M3 surface when `LocalLiquidGlass.current` is false or power save is on; never remove the toggle from Ajustes.
10. **[2026-11-28] ViewModels persist user inputs via `SavedStateHandle`**
   Do instead: constructor `(savedStateHandle, container)`; `AppViewModelProvider` builds it with `createSavedStateHandle()` inside `initializer`; write scalar inputs on every `update()`, restore in the initial `MutableStateFlow(...)`; DataStore/Room stay owners of `utility`/`baseMonthlyKwh`/groups. Duel AC vs. AC logic is pure in `AcDuelUiState` (no Android imports) and tested there.

## User Directives
1. **[2026-11-23] Signature and donation identity**
   Do instead: sign as `Roberto Franco` (`steefwel28`); Yappy number `6328-5079`; keep `BuildConfig.AUTHOR_SIGNATURE` and the manifest `com.steefwel28.signature` meta-data.
2. **[2026-11-23] Placeholders show `Hisense`**
   Do instead: brand/model placeholders always start with `Hisense` (e.g. `Hisense ATR122HA`).
3. **[2026-11-23] Explain gaps at the end of every reply**
   Do instead: close with 1–2 lines on what was skipped and the risk the user should know.
