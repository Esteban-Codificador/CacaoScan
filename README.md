# CacaoScan

Aplicación móvil Android nativa para clasificar el estado sanitario de vainas de cacao —**sana** o
**afectada por monilia** (*Moniliophthora roreri*)— mediante un modelo de aprendizaje profundo
ejecutado **en el propio dispositivo, sin conexión a internet**.

Proyecto académico · Unidades Tecnológicas de Santander
Esteban Alberto Avila Corredor · Juan Sebastián Ropero Amado

---

## Estado del proyecto

| | |
|---|---|
| Aplicación | **Funcional.** Compila, se instala y recorre el flujo completo |
| Pruebas unitarias | **56, todas en verde** |
| Tamaño del APK (release) | **17,1 MB** sin el modelo — RNF08 fija 100 MB |
| Modelo de IA | **No entrenado todavía.** La app arranca en modo demostración, señalizado en pantalla |
| Exactitud (RNF04 ≥ 85 %) | **No evaluada.** No existe conjunto de prueba |

> **Importante:** mientras no exista el archivo `.tflite`, los resultados que muestra la aplicación
> son **simulados**: `MockPodClassifier` no analiza la imagen. La pantalla de resultado muestra un
> aviso permanente y cada registro del historial queda marcado como simulado. Ver
> [docs/07-integracion-del-modelo.md](docs/07-integracion-del-modelo.md).

---

## Cómo compilar

Requisitos: Android Studio 2025.3.1 o superior, JDK 17+ (el que trae Android Studio sirve), Android
SDK 36.

```bash
gradlew :app:assembleDebug        # compilar
gradlew :app:installDebug         # compilar e instalar en un dispositivo conectado
gradlew :app:testDebugUnitTest    # 56 pruebas unitarias
```

Desde Android Studio: abrir la carpeta del proyecto y pulsar *Run*.

---

## Arquitectura

**MVVM + Repository Pattern**, con una capa de dominio delgada.

```
com.cacaoscan
├── data/          Room, almacenamiento interno de imágenes, repositorio
├── domain/        modelos, interfaces de repositorio, casos de uso
├── ml/            contrato del clasificador, preprocesamiento, interpretación,
│                  implementación TensorFlow Lite e implementación de demostración
├── presentation/  pantallas Compose y ViewModels
├── security/      Keystore, bloqueo biométrico, autorización JWT (demostración)
├── navigation/    grafo de navegación
└── di/            módulos de Hilt
```

Reglas que se cumplen sin excepción: ningún `@Composable` accede a Room ni conoce TensorFlow;
ningún ViewModel abre archivos ni construye SQL.

Detalle en [docs/01-arquitectura.md](docs/01-arquitectura.md).

---

## Navegación

```
Acceso administrativo  (contraseña; se crea en el primer arranque)
   ↓
Inicio
├── Escanear vaina  →  Cámara / Galería  →  Vista previa  →  Analizar  →  Resultado  →  Guardar
├── Historial       →  Detalle de la revisión  ·  Borrar todo (requiere autenticación)
├── Información     →  La monilia · Usar la app
└── Seguridad local →  demostración académica de autenticación y autorización
```

---

## Privacidad y funcionamiento sin conexión

La aplicación **no declara el permiso `android.permission.INTERNET`**. El sistema operativo le impide
abrir cualquier conexión de red: RNF02 y RNF06 dejan de ser una promesa del código y pasan a ser una
garantía verificable de la plataforma. Se comprueba abriendo `AndroidManifest.xml`.

Permisos declarados en el APK de release, verificados sobre el manifiesto fusionado:

| Permiso | Origen | Tipo |
|---|---|---|
| `CAMERA` | RF01, declarado por la app | Peligroso; se pide al entrar en la pantalla de escaneo, nunca al arrancar |
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | `androidx.biometric` | Normales; no muestran diálogo de concesión |

- **No hay `INTERNET` ni `ACCESS_NETWORK_STATE`.** `ACCESS_NETWORK_STATE` lo introducía
  `media3-common` de forma transitiva a través de CameraX y se elimina explícitamente del manifiesto.
- La selección desde galería usa el Selector de Fotos del sistema: **cero permisos de almacenamiento**.
- Las fotografías se guardan en el almacenamiento interno de la app, no en la galería del teléfono.
- `allowBackup="false"`: no se copian a la nube de respaldo.

---

## Documentación

| Documento | Contenido |
|---|---|
| [01 · Arquitectura](docs/01-arquitectura.md) | Por qué MVVM, capas, flujo de datos, Repository Pattern |
| [03 · Decisiones técnicas](docs/03-decisiones-tecnicas.md) | Justificación de cada dependencia y de las versiones elegidas |
| [06 · Seguridad y autorización](docs/06-seguridad-y-autorizacion.md) | Autenticación local, JWT, y por qué OTP/SSO/OAuth quedan fuera |
| [07 · Integración del modelo](docs/07-integracion-del-modelo.md) | Cómo conectar el `.tflite` real, parámetros a confirmar |
| [08 · Trazabilidad](docs/08-trazabilidad.md) | Matriz RF01–RF08 y RNF01–RNF08 |
| [09 · Pruebas](docs/09-pruebas.md) | Qué está cubierto, qué no, y guion de pruebas manuales |

---

## Fuera del alcance

Por decisión del documento de requisitos (IEEE830), la aplicación **no** hace:

- diagnóstico de enfermedades del cacao distintas de la monilia;
- recomendación de tratamientos ni dosis;
- localización de la lesión sobre la vaina (clasifica la imagen completa, no detecta);
- gestión de múltiples usuarios con cuentas de acceso;
- uso de servidores o bases de datos remotas.
