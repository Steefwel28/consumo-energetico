# AGENTS.md — Consumo Energético (Android 17)

## Estilo de trabajo (skill `ponytail`, nivel full)
- El cambio más pequeño que resuelve la tarea completa, incluyendo llamadores y pruebas que rompa.
- Orden de preferencia: no escribirlo > ya existe en el repo > stdlib/plataforma > dependencia instalada > una línea > código mínimo.
- Sin abstracciones, opciones, config ni código "para después". Borrar gana a añadir. Nunca recortar validación, seguridad, accesibilidad ni manejo de errores.
- Lógica nueva no trivial deja una prueba o auto-chequeo pequeño; atajo con límite conocido lleva `shortcut: <límite>, <cuándo mejorar>`.
- Cerrar cada respuesta con una o dos líneas: qué se omitió y qué riesgo debe conocer el usuario.

## Identidad y Firma (`steefwel28`)
- Repo: `Steefwel28/consumo-energetico` | Package: `com.steefwel28.consumoenergetico`
- Firma `steefwel28` obligatoria en:
  1. `BuildConfig.AUTHOR_SIGNATURE = "steefwel28"`
  2. `<meta-data android:name="com.steefwel28.signature" android:value="steefwel28" />` en `AndroidManifest.xml`
  3. Anotación `@CraftedBy("steefwel28")` en motores de cálculo, Room, exportadores y verificador GitHub.
  4. Metadatos `"crafted_by": "steefwel28"` en PDF y respaldo `.json`, más Easter Egg al tocar 5 veces la versión en Ajustes.

## Stack y Reglas Offline-First
- Android 17 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`), Kotlin + Jetpack Compose + Material 3.
- Dependencias: Room (KSP), WorkManager, DataStore Preferences, Navigation Compose, ML Kit Text Recognition (on-device).
- Prohibido Retrofit, OkHttp, Firebase o librerías PDF externas.
- Único acceso de red permitido: `HttpURLConnection` nativo (timeout 4s) a `https://api.github.com/repos/Steefwel28/consumo-energetico/releases/latest` (ignora pre-releases). Si hay versión nueva no omitida, muestra modal (`Actualizar` -> descarga directa `.apk` / `Omitir`) y activa un `BadgedBox` rojo con la letra `"A"` en Ajustes.

## Módulos Fase 1 (Aires Acondicionados)
- Unidades globales configurables en Ajustes: `m/ft`, `cm/in`, `p³/L`, `°C/°F`.
- Escáner OCR de etiqueta RTCA + ingreso manual + conversor de placa (`Ton`, `kW`, `A` -> `BTU/h`).
- Selector `ENSA` | `Naturgy` + switch `Costo Manual` con guía visual de facturas.
- Simulador de Control Remoto (`18°C` a `26°C`), switch de filtros limpios, comparador cara a cara y explicación conversacional.
- Mi Casa / Mi Local (Room): registro de equipos, historial de limpiezas, alerta agrupada cada 3 meses (WorkManager), exportación PDF vectorial + PNG para WhatsApp y respaldo `.json`.

## Skills Activas y Roles de Ejecución
- `panama-energy-domain`: Física térmica, etiquetas RTCA 23.01.78:20, pliegos ENSA/Naturgy y seguridad eléctrica en Panamá.
- `ponytail`: Arquitectura moderna en Kotlin/Android, flujo unidireccional (`StateFlow`), Room y rendimiento sin recomposiciones innecesarias.
- `impeccable`: Diseño visual estilo Google Pixel (Material You), jerarquía tipográfica, espaciado equilibrado, bordes `24.dp`, desenfoque sutil y calidad estética en las 5 ilustraciones a lápiz/plumilla.

## Organización por Grupos de Propiedades y Exportación Contextual
- **Grupos / Propiedades (`PropertyGroupEntity`):** Al guardar un equipo desde Calcular o desde Mi Perfil, el usuario selecciona o crea un grupo/propiedad (ej. *"Mi Casa"*, *"Trabajo"*, *"Casa de Mamá"*), asignándole su propia distribuidora (`ENSA` o `Naturgy`) y consumo base del recibo para que cada grupo calcule su propio salto de escalón tarifario.
- **Ubicaciones Dinámicas (`CustomLocationEntity`):** Selector táctil con ubicaciones base (`Habitación`, `Sala`, `Cocina`, `Comedor`, `Otros`). Si elige `Otros`, escribe un nombre personalizado que se persiste en Room para aparecer automáticamente en futuras selecciones.
- **Marca y Modelo:** El texto de ejemplo (placeholder) en los campos de marca/modelo debe ser siempre `Hisense` (ej. `Hisense ATR362MA`).
- **Exportación Contextual (`ENSA` | `Naturgy` | `VS`):** Al tocar el botón de compartir (PDF o PNG), la infografía generada refleja exactamente el modo activo en pantalla: solo ENSA, solo Naturgy, o la comparativa visual dual `VS` con ambas empresas y su diferencia en dólares.

## Reglas de Taller Panamá (Metraje, 220V y Protectores Reales)
- **Rangos comerciales por metraje base (Panamá):**
  - `9,000 BTU`: hasta `10 m²`
  - `12,000 BTU`: `11` a `16 m²`
  - `18,000 BTU`: `17` a `22 m²`
  - `24,000 BTU`: `23` a `32 m²`
  - `28,000 BTU` (referencia tipo Panasonic): `33` a `38 m²`
  - `36,000 BTU`: `39` a `45 m²` (tope máximo `50 m²` bien aislado).
- **Voltaje y Protección Eléctrica:** Todos los Splits modernos en Panamá operan a **220V** (110V queda solo como nota excepcional para equipos viejos No-Inverter). Para equipos Inverter, recomendar como prioridad protector de **línea directa (borneras) específico para Inverter** citando modelos reales: **AVTEK `SPC-PEBAS-B230`** (o `PBAS-B230-LED`, 220V, 30A, **2,100 J**, retardo 10s) y como alternativa **Forza `FVP-6630B`** (220V, 30A, 880 J, retardo ajustable 15s/30s/3m). Para equipos No-Inverter, indicar protector convencional de línea directa (ej. **AVTEK `SPC-PABB-B230`** 525 J / 3 min) o de enchufe según sea 220V o 110V.
- **Animaciones Globales:** `PixelSpringExpressive` y `rememberPixelHaptics()` deben aplicarse en TODAS las pantallas (`Calcular`, `Recomendar`, `Mi Casa` y `Ajustes`), en el cambio de pestañas del `NavigationBar` y en cada diálogo o hoja modal.

## Actualización v0.5.0-alpha (BTU reales, Periodo + Horas de Uso, Háptica 3s y Multi-idioma)
- **Rango válido de BTU/h:** Soportar hasta `60,000 BTU/h` en validación (`canSave` y `RtcaLabelParser`) para equipos reales como `36,679 BTU/h`.
- **Diferenciación ENSA vs. Naturgy:** Tarifas marginales distintas con 4 decimales (`B/. 0.0000/kWh`) y lectura dual en modo `VS`.
- **Periodo y Horas Estimadas de Uso:** Selector desplegable (`Día` | `Quincena` | `Mes` con `java.time.YearMonth.now()` contemplando días reales y bisiestos hasta 2030+) acompañado del control de **horas estimadas de uso diario (1h a 24h)** directamente en la tarjeta principal de cálculo.
- **Háptica Crescendo + Pop (3s):** Vibración progresiva estilo Duolingo/reacción IG al mantener presionados botones o ítems del `NavigationBar`, culminando en una liberación "pop" a los 3 segundos.
- **Internacionalización (i18n):** Soporte nativo completo en **Español Latinoamericano (`es`)**, **Inglés (`en`)** y **Chino Simplificado (`zh-CN`)** con selector de idioma en `Ajustes` (`AppCompatDelegate.setApplicationLocales`).

## Mapa Compacto del Proyecto (max 25 líneas)
- `app/build.gradle.kts` -> `android {}` -> `versionName/versionCode` y dependencias (Room, Compose, ML Kit)
- `MainActivity.kt` -> `MainActivity` -> splash 2s, tema, edge-to-edge, chequeo de actualización
- `ui/navigation/ConsumoApp.kt` -> `ConsumoApp` -> Scaffold, 3 pestañas y menú flotante ☰
- `ui/navigation/AppDestination.kt` -> `AppDestination` -> rutas Calcular/Recomendar/MiCasa
- `ui/screens/splash/ExpressiveSplashScreen.kt` -> `ExpressiveSplashScreen` -> morph óvalo→diamante→squircle→cookie + háptica
- `ui/screens/calculate/*` -> `CalculateScreen/ViewModel/UiState` -> entrada de equipo, tarifa, gráfica
- `ui/screens/recommend/*` -> `RecommendScreen/ViewModel/UiState` -> recomendación por m²
- `ui/screens/myhome/*` -> `MyHomeScreen/ViewModel/UiState` -> grupos, equipos, facturas, gráfica mensual
- `ui/screens/settings/*` -> `SettingsScreen/ViewModel/UiState` -> acordeones, apariencia, idioma, Acerca
- `ui/theme/*` -> `Theme/Color/Shape/Type` -> Material 3, OLED, Liquid Glass
- `ui/components/LiquidGlassBackground.kt` -> `LiquidGlassBackground` -> orbes + borde especular
- `ui/components/SectionCard.kt` -> `SectionCard` -> tarjeta acordeón reutilizable
- `ui/components/PixelEmblem.kt` + `PixelMotion.kt` -> emblema y springs/háptica (`PixelSpringExpressive`, `PixelChargeVibrator`)
- `domain/calculator/*` -> `AcConsumptionCalculator`, `PanamaTariffEngine` -> kWh y tarifa
- `domain/recommender/*` -> `AcSpaceRecommender`, `ElectricalSpecAdvisor` -> BTU/protección 220V
- `domain/narrative/DiagnosticNarrativeBuilder.kt` -> `DiagnosticNarrativeBuilder` -> prosa en español (tests)
- `domain/scanner/RtcaLabelParser.kt` -> `RtcaLabelParser` -> parseo OCR RTCA (≤60,000 BTU/h)
- `domain/settings/Appearance.kt` -> `Appearance` -> Día/Noche/Sistema + flags OLED/Glass
- `data/local/db/*` -> `ConsumoDatabase` v4 -> Room, migración 3→4, DAOs
- `data/datastore/UserPreferencesRepository.kt` -> repositorio -> unidades, tarifa, apariencia persistida
- `data/export/ReportExporter.kt` -> `ReportExporter` -> PDF vectorial y PNG
- `data/backup/LocalBackupManager.kt` -> `LocalBackupManager` -> respaldo/restauración JSON
- `core/updater/*` -> `GitHubReleaseChecker` -> única red permitida (API releases)
- `app/src/test/java/...` -> pruebas unitarias -> calculadora, tarifa, parsers, ui state
