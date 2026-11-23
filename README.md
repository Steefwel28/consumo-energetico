# âš¡ Consumo EnergÃ©tico (PanamÃ¡)

AplicaciÃ³n nativa para **Android 17** diseÃ±ada con estÃ©tica **Google Pixel (Material You)** que funciona **100 % offline**. Permite calcular el consumo elÃ©ctrico real de aires acondicionados (Inverter y convencionales) leyendo la etiqueta amarilla oficial **RTCA 23.01.78:20**, cruzar los datos con los pliegos tarifarios escalonados de **ENSA** y **Naturgy**, y recomendar la capacidad exacta segÃºn las condiciones reales de cada espacio en PanamÃ¡.

---

### âœ¨ CaracterÃ­sticas principales

* **EscÃ¡ner de Etiqueta Amarilla (OCR Offline):** Lectura directa de `BTU/h`, potencia elÃ©ctrica (`W`) y `REEE / SEER` mediante cÃ¡mara en el dispositivo (ML Kit), mÃ¡s conversor de placa (`Toneladas`, `kW`, `Amperios`).
* **Motor Tarifario de PanamÃ¡:** Pliegos escalonados precargados para **ENSA** y **Naturgy (EDEMET / EDECHI)**, interruptor de **Costo Manual** y guÃ­a visual ilustrada de ambos recibos.
* **Simulador de Control Remoto (`18 Â°C` â€“ `26 Â°C`):** Visualiza en tiempo real cuÃ¡nto sube o baja tu factura mensual segÃºn la temperatura elegida.
* **Recomendador Inteligente de Espacios:** CÃ¡lculo tropicalizado para PanamÃ¡ con panel de detalles extra (ventanales/sol, ocupaciÃ³n, techo y distancia de tuberÃ­a en `m` o `ft`), explicaciÃ³n conversacional y alerta educativa contra el subdimensionamiento (*"Trampa del Aire PequeÃ±o"*).
* **Comparador Cara a Cara:** Enfrenta tu equipo actual contra un modelo Inverter nuevo y calcula el ahorro mensual junto a los meses de retorno de inversiÃ³n.
* **CÃ¡psulas Educativas Ilustradas a Plumilla:** GuÃ­as visuales sobre *SEER*, *el mito de bajar a 17 Â°C*, *protectores Inverter y Julios (J)*, y *Modo Dry vs. Deshumidificador*.
* **Mi Casa / Mi Local:** Registro local en Room de todos tus equipos, historial de limpiezas, alertas agrupadas cada 3 meses, exportaciÃ³n de diagnÃ³sticos en **PDF vectorial** y **PNG** para WhatsApp, y respaldo/restauraciÃ³n en `.json`.

---

### ðŸ› ï¸ Stack TÃ©cnico

* **Plataforma:** Android 17 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`)
* **Lenguaje y UI:** Kotlin + Jetpack Compose + Material 3 (`enableEdgeToEdge()`)
* **Persistencia y Tareas:** Room Database, DataStore Preferences, WorkManager
* **Herramientas Nativas:** ML Kit Text Recognition (on-device) & `android.graphics.pdf.PdfDocument`

---

### ðŸ‘¤ Autor
Desarrollado por **Roberto Franco** (`@steefwel28`).

---

### ðŸš€ Estado del Desarrollo (Alpha v0.3.0-alpha)
- [x] **Bloque 0:** Arquitectura Android 17, firma `steefwel28` y verificador de releases en GitHub.
- [x] **Bloque 1:** Motor tÃ©rmico RTCA, tarifas escalonadas ENSA/Naturgy, conversor de unidades y narrativa conversacional.
- [x] **Bloque 2:** Persistencia local en Room v2 (Grupos de Propiedades y Ubicaciones dinÃ¡micas), alertas agrupadas cada 3 meses y exportaciÃ³n infogrÃ¡fica PDF/PNG.
- [x] **Bloque 3A:** Pantalla `Calcular` (GrÃ¡fica `ENSA | Naturgy | VS`, escÃ¡ner OCR offline, simulador desplegable) y `Recomendar` con fÃ­sicas de resorte (`spring`) y `Rich Haptics`.
- [x] **Bloque 3B:** Ilustraciones educativas a plumilla, gestor multi-propiedad en `Mi Casa`, `Ajustes` con respaldo SAF y modal de actualizaciones GitHub.


### ✨ Características Completadas (Fase 1 — Aires Acondicionados)
- **Comparador Tarifario en Vivo (`ENSA` | `Naturgy` | `VS`):** Gráfica interactiva con salto real de escalones tarifarios y modo *Costo Manual* desde recibo.
- **Escáner OCR Offline:** Lectura de etiquetas amarillas RTCA 23.01.78:20 mediante CameraX y ML Kit sin internet.
- **Recomendador Calibrado a Panamá:** Rangos comerciales de taller (`9k` a `36k BTU`, incluyendo escalón `28k`), instalación a `220V` y ficha de protección de línea directa (**AVTEK `SPC-PEBAS-B230`** 2,100 J / **Forza `FVP-6630B`**).
- **Gestión Multi-Propiedad (`Mi Casa`):** Grupos independientes (*Mi Casa*, *Trabajo*, *Casa de Mamá*), ubicaciones dinámicas, historial de limpiezas y alertas consolidadas cada 90 días.
- **Exportación Infográfica y Respaldos:** Láminas estadísticas en **PDF vectorial** y **PNG** listas para WhatsApp, más copias de seguridad `.json` vía SAF.
