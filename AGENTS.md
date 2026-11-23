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
