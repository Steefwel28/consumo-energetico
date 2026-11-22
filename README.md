# ⚡ Consumo Energético (Panamá)

Aplicación nativa para **Android 17** diseñada con estética **Google Pixel (Material You)** que funciona **100 % offline**. Permite calcular el consumo eléctrico real de aires acondicionados (Inverter y convencionales) leyendo la etiqueta amarilla oficial **RTCA 23.01.78:20**, cruzar los datos con los pliegos tarifarios escalonados de **ENSA** y **Naturgy**, y recomendar la capacidad exacta según las condiciones reales de cada espacio en Panamá.

---

### ✨ Características principales

* **Escáner de Etiqueta Amarilla (OCR Offline):** Lectura directa de `BTU/h`, potencia eléctrica (`W`) y `REEE / SEER` mediante cámara en el dispositivo (ML Kit), más conversor de placa (`Toneladas`, `kW`, `Amperios`).
* **Motor Tarifario de Panamá:** Pliegos escalonados precargados para **ENSA** y **Naturgy (EDEMET / EDECHI)**, interruptor de **Costo Manual** y guía visual ilustrada de ambos recibos.
* **Simulador de Control Remoto (`18 °C` – `26 °C`):** Visualiza en tiempo real cuánto sube o baja tu factura mensual según la temperatura elegida.
* **Recomendador Inteligente de Espacios:** Cálculo tropicalizado para Panamá con panel de detalles extra (ventanales/sol, ocupación, techo y distancia de tubería en `m` o `ft`), explicación conversacional y alerta educativa contra el subdimensionamiento (*"Trampa del Aire Pequeño"*).
* **Comparador Cara a Cara:** Enfrenta tu equipo actual contra un modelo Inverter nuevo y calcula el ahorro mensual junto a los meses de retorno de inversión.
* **Cápsulas Educativas Ilustradas a Plumilla:** Guías visuales sobre *SEER*, *el mito de bajar a 17 °C*, *protectores Inverter y Julios (J)*, y *Modo Dry vs. Deshumidificador*.
* **Mi Casa / Mi Local:** Registro local en Room de todos tus equipos, historial de limpiezas, alertas agrupadas cada 3 meses, exportación de diagnósticos en **PDF vectorial** y **PNG** para WhatsApp, y respaldo/restauración en `.json`.

---

### 🛠️ Stack Técnico

* **Plataforma:** Android 17 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`)
* **Lenguaje y UI:** Kotlin + Jetpack Compose + Material 3 (`enableEdgeToEdge()`)
* **Persistencia y Tareas:** Room Database, DataStore Preferences, WorkManager
* **Herramientas Nativas:** ML Kit Text Recognition (on-device) & `android.graphics.pdf.PdfDocument`

---

### 👤 Autor
Desarrollado por **Roberto Franco** (`@steefwel28`).
