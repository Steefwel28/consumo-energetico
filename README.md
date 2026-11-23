# ⚡ Consumo Energético (Panamá)

Aplicación nativa para **Android 17** diseñada con estética **Google Pixel (Material 3 Expressive)** que funciona **100 % offline**. Permite calcular el consumo eléctrico real de aires acondicionados (Inverter y convencionales) leyendo la etiqueta amarilla oficial **RTCA 23.01.78:20**, cruzar los datos con los pliegos tarifarios escalonados de **ENSA** y **Naturgy**, y recomendar la capacidad exacta según las condiciones reales de cada espacio en Panamá.

## ✨ Novedades en v0.5.0-alpha
- **Gráfica de Ondas Pixel (ENSA vs. Naturgy):** Barras sinusoidales animadas con colores diferenciados (Cian y Ámbar), tarifas marginales a **4 decimales** (B/. 0.0000/kWh) y píldora dinámica de ahorro directo.
- **Proyección por Periodo y Horas Reales:** Selector desplegable (Día, Quincena, Mes con calendario real hasta 2030+) y ajuste instantáneo de horas diarias de uso (1 h a 24 h) que se heredan al guardar en *Mi Casa*.
- **Soporte hasta 60,000 BTU/h:** Validación y ficha eléctrica calibradas para equipos residenciales y comerciales reales de hasta 5 toneladas (incluyendo placas de 36,679 BTU/h).
- **Identidad Material 3 Expressive:** Ícono adaptativo vectorial con capa monocromática, pantalla de bienvenida de 2 segundos con mutación de formas Pixel y háptica progresiva de 3 segundos con liberación tipo *pop*.
- **Multi-idioma y Apoyo Directo:** Traducción nativa en **Español (Latinoamérica)**, **English** y **中文 (简体)** —incluyendo las láminas PDF/PNG— y acceso rápido de apoyo vía **Yappy**.

## 🚀 Estado del Desarrollo
- [x] **Bloque 0:** Arquitectura Android 17, firma `steefwel28` y verificador de releases en GitHub.
- [x] **Bloque 1:** Motor térmico RTCA, tarifas escalonadas ENSA/Naturgy, conversor de unidades y narrativa conversacional.
- [x] **Bloque 2:** Persistencia local en Room v3 (Grupos de Propiedades, Ubicaciones dinámicas y horas reales), alertas cada 90 días y exportación PDF/PNG.
- [x] **Bloque 3:** UI Material 3 Expressive, escáner OCR offline, ilustraciones a plumilla, respaldos SAF e internacionalización trilingüe.