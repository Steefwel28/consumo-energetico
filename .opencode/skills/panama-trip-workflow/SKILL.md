---
name: panama-trip-workflow
description: Orquesta el flujo completo de desarrollo de cualquier funcionalidad de Panama-Trip desde el cuestionamiento inicial hasta la verificación y reescritura final.
---
Cuando el desarrollador pida construir una nueva pantalla o funcionalidad para Panama-Trip, ejecuta estrictamente estas 4 fases en orden:
- Fase 1 (Cuestionamiento): Activa abogado-del-diablo. Detente y espera a que el desarrollador responda las objeciones antes de programar.
- Fase 2 (Construcción): Una vez aprobado el flujo, usa senior-android-architect para implementar dominio, datos, ViewModel y UI en Compose Material 3.
- Fase 3 (Auditoría y Reescritura): Corre auditor-refactor sobre todos los archivos creados y reescribe cualquier defecto detectado.
- Fase 4 (Verificación): Compila el proyecto con ./gradlew assembleDebug para confirmar que no hay errores.
