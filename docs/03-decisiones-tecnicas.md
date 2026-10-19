# Decisiones técnicas

Cada decisión con su motivo. Donde hubo una alternativa razonable, se indica y se explica por qué se
descartó.

---

## 1. Dependencias: por qué está cada una

| Dependencia | Razón técnica | Alternativa descartada |
|---|---|---|
| **Jetpack Compose + Material 3** | UI declarativa; el estado de pantalla se expresa como datos, no como mutaciones de vistas | XML + View system: más código y estados implícitos |
| **Navigation Compose** | Seis destinos con pila de retroceso y un subgrafo | Gestión manual del back stack: propensa a errores |
| **Lifecycle ViewModel** | Supervivencia del estado ante recreación de la Activity | `rememberSaveable`: no sobrevive a la muerte del proceso ni encaja bien con corrutinas largas |
| **Room + KSP** | RF06 y RNF05; SQL verificado en tiempo de compilación | SQLite a mano: sin verificación, más código repetido |
| **Hilt** | Sustituir el clasificador por un doble en pruebas sin tocar la UI | Contenedor manual: viable, pero mucho `ViewModelProvider.Factory` repetido en seis pantallas |
| **CameraX** | RF01; abstrae Camera2 y la fragmentación de la gama media | Camera2 directo: mucho más código; `ACTION_IMAGE_CAPTURE`: depende de la app de cámara del fabricante |
| **PickVisualMedia** (activity-compose) | RF02 **sin ningún permiso de almacenamiento** | `READ_MEDIA_IMAGES`: pediría un permiso evitable |
| **ExifInterface** | Sin corregir la rotación EXIF, el modelo recibe vainas acostadas | Ninguna: es un problema real de las cámaras |
| **Coil** | Miniaturas del historial con caché y sin agotar memoria | `BitmapFactory` manual en la lista: riesgo de `OutOfMemory` al desplazar |
| **TensorFlow Lite** | RF03/RF05: inferencia local | Ver sección 3 |
| **androidx.biometric** | Autenticación local real sin servidor | Ninguna equivalente |
| **org.json** (solo pruebas) | En Android está en el sistema, pero el *stub* del SDK no sirve en pruebas JVM | Escribir un serializador propio: más frágil que usar la misma API |

**No se incluyeron**, pese a ser habituales: Retrofit/OkHttp (no hay red), SQLCipher (peso alto para
datos que no son sensibles y cuyo aislamiento ya garantiza el sandbox), y ninguna librería de JWT: la
firma HMAC se implementa con `javax.crypto`, unas 40 líneas, lo que además la hace explicable en la
sustentación.

---

## 2. Cadena de compilación: por qué estas versiones

La elección **no** fue «lo más nuevo». Fue «lo más nuevo que el Android Studio del equipo puede
abrir», que es un criterio distinto y más útil.

| Componente | Versión | Motivo |
|---|---|---|
| Android Gradle Plugin | 9.0.1 | Es el AGP que soporta el Android Studio instalado (2025.3.1) |
| Gradle | 9.2.1 | Versión máxima que ese Studio conoce y aprovisiona |
| Kotlin | 2.4.10 | Última estable |
| KSP | 2.3.11 | Compatible con la anterior |
| compileSdk / targetSdk | 36 | Máximo recomendado para AGP 9.0 |
| minSdk | 26 | RNF07. Además habilita `java.time` de forma nativa, sin *desugaring* |

**Consecuencia asumida:** las versiones más recientes de AndroidX (core-ktx 1.19, Compose 1.12,
lifecycle 2.11) exigen AGP 9.1+ y `compileSdk 37`, y AGP 9.1 exige Gradle 9.3.1, que ese Studio no
provisiona. Por eso las librerías están fijadas una versión por debajo:

```
core-ktx 1.18.0 · compose-bom 2026.06.01 (Compose 1.11.4) · lifecycle 2.10.0
activity-compose 1.12.4 · navigation-compose 2.9.8 · hilt-navigation-compose 1.3.0
```

Cuando el equipo actualice Android Studio, subir todo el bloque es un cambio localizado en
`gradle/libs.versions.toml`.

**Nota sobre AGP 9:** ya no debe aplicarse el plugin `org.jetbrains.kotlin.android`; AGP lo aplica
por su cuenta y declararlo provoca un error de configuración.

---

## 3. TensorFlow Lite en lugar de LiteRT

LiteRT es el sucesor oficial de TensorFlow Lite y era la primera opción. **No se pudo usar**, y el
motivo está verificado, no supuesto:

```
Namespace 'com.google.ai.edge.litert' is used in multiple modules and/or libraries:
com.google.ai.edge.litert:litert:2.2.0, com.google.ai.edge.litert:litert-api:2.2.0
```

Los dos artefactos publicados declaran el mismo espacio de nombres y AGP 9 rechaza esa duplicidad al
fusionar el manifiesto. Se comprobó con las versiones **2.1.6** y **2.2.0**: el fallo es idéntico.

Se usa `org.tensorflow:tensorflow-lite:2.17.0`, cuya API de programación es **la misma**
(`org.tensorflow.lite.Interpreter`). Migrar a LiteRT cuando se corrija el empaquetado será cambiar
una línea.

---

## 4. Decisiones de diseño de la aplicación

**El resultado simulado no puede confundirse con uno real.** `ClassificationOrigin` viaja con el
resultado hasta la interfaz y hasta la base de datos. La pantalla de resultado muestra un aviso no
descartable, y el registro del historial queda marcado como simulado de forma permanente.

**El porcentaje se rotula siempre como «confianza del modelo».** Nunca como probabilidad de acierto
ni como diagnóstico. Va acompañado de una frase que recuerda que es una orientación. Presentarlo
como verdad absoluta sería engañoso, más aún con un modelo cuya exactitud todavía no se ha evaluado.

**Sin color dinámico (Material You).** El significado de «sana» y «afectada» no puede depender del
fondo de pantalla del usuario. Los colores de estado están fijados y separados de los de marca.

**Botones de 72 dp de alto**, muy por encima del mínimo de 48 dp, y tipografía ampliada respecto a la
escala por defecto de Material 3. El contexto de uso es el campo, con luz solar directa y usuarios no
necesariamente habituados a interfaces móviles (RNF03).

**Imágenes reescaladas a 1024 px de lado mayor, JPEG al 85 %.** Guardar la fotografía original haría
crecer el almacenamiento sin límite tras unas decenas de revisiones.

**Marcas de tiempo en UTC, presentadas en la zona del dispositivo.** Guardarlas en hora local
desordenaría el historial al cambiar la hora del teléfono.

---

## 5. Nota sobre el entorno de compilación

Durante el desarrollo se encontró un fallo del entorno que conviene dejar documentado, porque puede
reaparecer: en máquinas cuyo directorio temporal se resuelve en formato corto 8.3 de Windows
(por ejemplo `C:\Users\ESTEBA~1\...`, habitual cuando el nombre de usuario lleva espacios), la JVM no
puede crear los *sockets* de dominio Unix que usa internamente `Selector.open()`, y Gradle falla con:

```
java.io.IOException: Unable to establish loopback connection
```

Se resuelve apuntando `TMP` y `TEMP` a una ruta larga. Android Studio no se ve afectado porque usa su
propio directorio temporal.

---

## 6. Estado de lint

`gradlew :app:lintDebug` termina con **0 errores y 16 avisos**. Los avisos que quedan son
deliberados o externos, y conviene conocerlos:

| Aviso | Nº | Por qué sigue ahí |
|---|---|---|
| `GradleDependency` / `AndroidGradlePluginVersion` | 10 | Lint señala que hay versiones más nuevas de AndroidX y de AGP. Es exactamente la decisión explicada en la sección 2: se fijan por debajo para que el proyecto abra en el Android Studio del equipo |
| `Aligned16KB` | 3 | Las bibliotecas nativas de TensorFlow Lite (`libtensorflowlite_jni.so`) no están alineadas a 16 KB. **Es un problema real de compatibilidad futura**: Android 15 y posteriores empujan hacia páginas de 16 KB. No se puede corregir desde este proyecto; depende de que Google publique el runtime realineado. Conviene revisarlo antes de publicar |
| `Recycle` | 1 | Falso positivo: `BitmapLoader` sí cierra el `InputStream` con `use { }`; lint no rastrea bien `use` sobre un receptor anulable |
| `DataExtractionRules` | 1 | Aviso sobre `allowBackup`, que se conserva a propósito para cubrir Android 11 y anteriores, además de las reglas de extracción ya declaradas |

No se creó una *baseline* de lint. Ocultar avisos sin haberlos leído es peor que tenerlos a la vista.
