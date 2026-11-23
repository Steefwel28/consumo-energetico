# ⚡ Consumo Energético (Panamá)

Aplicación nativa para **Android 17** diseñada con estética **Google Pixel (Material 3 Expressive)** que funciona **100 % offline**. Permite calcular el consumo eléctrico real de aires acondicionados (Inverter y convencionales) leyendo la etiqueta amarilla oficial **RTCA 23.01.78:20**, cruzar los datos con los pliegos tarifarios escalonados de **ENSA** y **Naturgy**, y recomendar la capacidad exacta según las condiciones reales de cada espacio en Panamá.

## ✨ Características Principales
- **Comparador Tarifario en Vivo (ENSA | Naturgy | VS):** Gráfica interactiva con salto real de escalones tarifarios, proyección por **Día, Quincena o Mes** (calendario automático hasta 2030+), ajuste de **horas estimadas de uso** y modo *Costo Manual* desde recibo.
- **Escáner de Etiqueta Amarilla (OCR Offline):** Lectura directa de BTU/h, potencia eléctrica (W) y REEE / SEER mediante cámara (CameraX + ML Kit), más conversor de placa (Toneladas, kW, Amperios).
- **Recomendador Calibrado a Panamá:** Rangos comerciales reales de taller (9k a 36k BTU, incluyendo escalón 28k), instalación estándar a 220V y ficha eléctrica con protección de línea directa (**AVTEK SPC-PEBAS-B230** 2,100 J / **Forza FVP-6630B**).
- **Gestión Multi-Propiedad (Mi Casa):** Grupos independientes (*Mi Casa*, *Trabajo*, *Casa de Mamá*), ubicaciones dinámicas, historial de limpiezas y alertas consolidadas cada 90 días.
- **Exportación Infográfica y Multi-idioma:** Láminas estadísticas en **PDF vectorial** y **PNG** listas para WhatsApp, copias de seguridad .json vía SAF y soporte nativo en **Español (Latinoamérica)**, **English** y **中文 (简体)**.

## 🚀 Estado del Desarrollo
- [x] **Bloque 0:** Arquitectura Android 17, firma `steefwel28` y verificador de releases en GitHub.
- [x] **Bloque 1:** Motor térmico RTCA, tarifas escalonadas ENSA/Naturgy, conversor de unidades y narrativa conversacional.
- [x] **Bloque 2:** Persistencia local en Room v2 (Grupos de Propiedades y Ubicaciones dinámicas), alertas agrupadas cada 3 meses y exportación infográfica PDF/PNG.
- [x] **Bloque 3:** UI Material 3 Expressive (spring + Rich Haptics), escáner OCR, ilustraciones a plumilla, gestor multi-propiedad y respaldos SAF.