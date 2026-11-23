# Reglas de ofuscación — Consumo Energético (steefwel28)

# Conserva los nombres de la firma del autor y las anotaciones de trazabilidad.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature
-keep @com.steefwel28.consumoenergetico.core.signature.CraftedBy class * { *; }
-keep class com.steefwel28.consumoenergetico.core.signature.** { *; }

# Room genera implementaciones por reflexión en tiempo de compilación.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# ML Kit Text Recognition (modelo on-device).
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
