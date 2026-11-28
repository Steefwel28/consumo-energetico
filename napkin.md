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
5. **[2026-11-28] Updater tests need real org.json on the JVM**
   Do instead: keep `testImplementation("org.json:json")` — Android stubs org.json in unit tests.
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
   Do instead: ENSA fijo 2.76 / 0.17737 / 0.24666 / 0.27005; Naturgy EDEMET fijo 3.17 / 0.17624 / 0.23177 / 0.25027; EDECHI fijo 3.04 / 0.19175 / 0.25341 / 0.27486; show `validFrom`/`validTo` in UI. Por diseño EDEMET ≤ ENSA y EDECHI es la más cara en cada escalón: en tests de comparativa el mínimo siempre es EDEMET y el máximo EDECHI; ENSA jamás es el mínimo.
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
1. **[2026-11-28] Liquid Glass and the haze dependency are gone for good**
   Do instead: never reintroduce `LocalLiquidGlass`/haze blur; floating layers (nav bar, menu, sheets) use solid `surfaceContainer` with 24.dp shapes.
2. **[2026-11-28] Distributor/brand colors come from `LocalExtendedColors`, never `isSystemInDarkTheme()`**
   Do instead: `ExtendedColors(ensa, edemet, edechi, warningContainer, onWarningContainer)` provided by `ConsumoEnergeticoTheme`; screens read `LocalExtendedColors.current` (waves, duel bars, AboutCard uses M3 `primaryContainer` roles).
3. **[2026-11-28] Haptics: system engine, but never on sliders or long-press**
   Do instead: `LocalHapticFeedback.current` with `Confirm`, `Reject`, `SegmentTick`, `ToggleOn`/`ToggleOff` for taps and switches; no direct `Vibrator`, no `VIBRATE` permission. `pixelChargeHaptics` was deleted in v0.7.2-beta — sliders drag silently and long-presses carry no crescendo.
4. **[2026-11-28] Splash haptics still follow the 2 s timeline**
   Do instead: stage 1 `SegmentTick`, stage 2 two `SegmentTick`, stage 3 ramp ticks, `Confirm` at 2000 ms with `1.0 → 1.12` bounce.
5. **[2026-11-23] `Float * Dp` does not compile**
   Do instead: write `dp * float` (e.g. `8.dp * (1f - eased)`), never `(1f - eased) * 8.dp`.
6. **[2026-11-28] `HOURS_STEP_INDEX` must match the LazyColumn order in `CalculateScreen`**
   Do instead: constant is `5` since v0.7.2-beta (items: 0 título, 1 categoría, 2 paso 1 BTU, 3 paso 2 Watts, 4 paso 3 SEER, 5 paso 4 horas, 6 opcional, 7 conversor, 8 guardar/compartir, 9 footer); re-check after any reorder.
7. **[2026-11-28] `rememberSaveable` se importa desde `runtime.saveable`**
   Do instead: `import androidx.compose.runtime.saveable.rememberSaveable`; la ruta `androidx.compose.runtime.rememberSaveable` no existe y rompe `compileDebugKotlin`.
8. **[2026-11-28] Calcular flows 0 → 4; extras live in one collapsed card**
   Do instead: paso 0 (cabecera fija: costo, chips, periodo, botón "Comparación"), pasos 1-4 como tarjetas numeradas; consumo base, costo manual, temperatura, espacio/marca y "Guardar en Mi Casa" plegados en `OptionalInputsCard`. Quick chips leen `CalculateUiState.BTU_QUICK_VALUES` (testado).
9. **[2026-11-28] Updater hits the releases LIST, prereleases included**
   Do instead: `GITHUB_RELEASES_API` ends in `/releases`; `GitHubReleaseChecker.selectLatestRelease(payload, skipped)` ignores drafts, considers pre-releases and falls through skipped versions; header `User-Agent: ConsumoEnergetico-App`.
10. **[2026-11-28] ViewModels persist user inputs via `SavedStateHandle`**
   Do instead: constructor `(savedStateHandle, container)`; `AppViewModelProvider` builds it with `createSavedStateHandle()` inside `initializer`; write scalar inputs on every `update()`, restore in the initial `MutableStateFlow(...)`; DataStore/Room stay owners of `utility`/`baseMonthlyKwh`/groups. Duel AC vs. AC logic is pure in `AcDuelUiState` (no Android imports) and tested there.

## User Directives
1. **[2026-11-23] Signature and donation identity**
   Do instead: sign as `Roberto Franco` (`steefwel28`); Yappy number `6328-5079`; keep `BuildConfig.AUTHOR_SIGNATURE` and the manifest `com.steefwel28.signature` meta-data.
2. **[2026-11-23] Placeholders show `Hisense`**
   Do instead: brand/model placeholders always start with `Hisense` (e.g. `Hisense ATR122HA`).
3. **[2026-11-23] Explain gaps at the end of every reply**
   Do instead: close with 1–2 lines on what was skipped and the risk the user should know.
