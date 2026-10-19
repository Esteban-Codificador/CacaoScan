# Reglas de ofuscacion/reduccion para la build de release (RNF08).

# Room genera implementaciones por reflexion parcial; se conservan sus anotaciones.
-keepattributes *Annotation*
-keepattributes Signature

# Modelos de dominio serializados por Room: se conservan sus nombres de campo.
-keep class com.cacaoscan.data.local.db.** { *; }

# LiteRT / TensorFlow Lite (se activa en la FASE 11).
-keep class org.tensorflow.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn org.tensorflow.**
-dontwarn com.google.ai.edge.litert.**
