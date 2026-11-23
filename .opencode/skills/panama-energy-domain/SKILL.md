---
name: panama-energy-domain
description: Reglas de física térmica, etiquetas RTCA, tarifas ENSA/Naturgy y diseño estilo Google Pixel para Consumo Energético.
---
# Reglas del Proyecto Consumo Energético (Panamá)
- **Estética Google Pixel:** Material 3 con colores dinámicos (`dynamicDarkColorScheme`/`dynamicLightColorScheme`), jerarquía por tonos (`surfaceContainerLow`/`surfaceContainerHigh`), bordes `24.dp`, `enableEdgeToEdge()`, y desenfoque (`Modifier.blur`) solo en hojas modales y diálogos.
- **Ilustraciones:** Las 5 cápsulas educativas (*SEER*, *Mito 17 °C*, *Aire Subdimensionado*, *Protector Inverter y Julios*, *Modo Dry vs. Deshumidificador*) deben dibujarse con estética de boceto a lápiz o plumilla (`Canvas` / `VectorDrawable`) adaptable a modo claro y oscuro.
- **Etiqueta RTCA 23.01.78:20:** Lee `BTU/h`, potencia nominal `W` y `REEE/SEER` (`Wt/We` y `BTU/hW`). Diferencia Inverter (modulación según SEER y temperatura del Simulador Remoto `18°C-26°C`) vs. No Inverter (ciclos On/Off al 100 % W).
- **Tarifas Panamá:** Pliegos escalonados precargados de **ENSA** y **Naturgy** + interruptor `Costo Manual` (tarifa directa o Total B/. ÷ kWh del recibo) con guía visual dibujada en `ModalBottomSheet`.
- **Recomendador y Seguridad:** Base tropicalizada Panamá + botón *"Más detalles del lugar"* (ventanales/sol, personas, techo, distancia de tubería en `m` o `ft` según Ajustes). Alerta contra subdimensionamiento ("Trampa del Aire Pequeño"), ficha eléctrica (`110V/220V`, breaker, calibre de cable, protector Inverter en Julios) y comparador cara a cara.
