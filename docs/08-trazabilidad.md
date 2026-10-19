# Matriz de trazabilidad IEEE830 → código

Estado a la fecha de esta entrega. **Ningún requisito se marca como cumplido sin señalar dónde está
implementado.** Los requisitos que dependen del modelo entrenado se marcan como pendientes, no como
cumplidos.

---

## Requisitos funcionales

| RF | Nombre | Estado | Dónde está |
|---|---|---|---|
| **RF01** | Captura de imagen mediante cámara | Implementado | `presentation/scan/CameraCapture.kt` (CameraX), `ScanScreen.kt`, permiso `CAMERA` en tiempo de ejecución |
| **RF02** | Selección de imagen desde galería | Implementado | `ScanScreen.kt` con `ActivityResultContracts.PickVisualMedia`; sin permisos de almacenamiento |
| **RF03** | Clasificación del estado sanitario | Implementado; **a la espera del modelo** | `ml/PodClassifier.kt`, `preprocessing/BitmapPreprocessor.kt`, `interpretation/OutputInterpreter.kt`, `tflite/TfLitePodClassifier.kt`. Sin el `.tflite` se usa `mock/MockPodClassifier.kt`, señalizado en pantalla |
| **RF04** | Visualización del resultado con nivel de confianza | Implementado | `presentation/result/ResultScreen.kt`: categoría, porcentaje, barra, desglose por clase y tiempo de análisis |
| **RF05** | Clasificación sin conexión a internet | Implementado y **garantizado por la plataforma** | El manifiesto no declara `android.permission.INTERNET`; la inferencia es local |
| **RF06** | Registro y consulta del historial | Implementado | Room (`data/local/db/`), imágenes en `filesDir` (`InternalImageStorage`), pantallas `HistoryScreen` y `HistoryDetailScreen` |
| **RF07** | Información sobre la monilia | Implementado | `presentation/info/InfoContent.kt`, pestaña «La monilia». Contenido local |
| **RF08** | Guía de uso de la aplicación | Implementado | `presentation/info/InfoContent.kt`, pestaña «Usar la app» |

## Requisitos no funcionales

| RNF | Nombre | Métrica del IEEE830 | Estado |
|---|---|---|---|
| **RNF01** | Tiempo de clasificación | ≤ 5 s en gama media | **Pendiente de medir con el modelo real.** La aplicación ya mide el tiempo de inferencia en cada revisión, lo muestra en pantalla y lo guarda en el historial: la evidencia se recogerá, no se estimará |
| **RNF02** | Funcionamiento sin conexión | 100 % de funciones principales | **Cumplido y verificado sobre el manifiesto fusionado.** Los únicos permisos del APK de release son `CAMERA`, `USE_BIOMETRIC` y `USE_FINGERPRINT`: no hay `INTERNET` ni `ACCESS_NETWORK_STATE` |
| **RNF03** | Usabilidad | Usuario nuevo completa una clasificación sin ayuda | **Implementado, con alcance acotado.** Tres destinos en inicio, botones de 72 dp, tipografía ampliada, textos sin jerga, guía de uso. ⚠️ Tras el cambio de alcance del acceso administrativo, el requisito se mide **sobre el flujo de clasificación, con la sesión ya iniciada**. Ver `06-seguridad-y-autorizacion.md`, apartado 1 bis. Validación con productores reales pendiente |
| **RNF04** | Precisión del modelo | Exactitud ≥ 85 % en conjunto de prueba | **NO evaluado.** No existe modelo entrenado. La aplicación no afirma en ningún punto tener una exactitud determinada |
| **RNF05** | Persistencia local | Los registros sobreviven al cierre | **Cumplido.** Room con base en disco; probado en `ScanDaoTest` (instrumentado) y `ScanRepositoryImplTest` (JVM) |
| **RNF06** | Privacidad de los datos | Cero transmisión externa | **Cumplido y verificable.** Sin permiso de red en el manifiesto fusionado; almacenamiento interno; `allowBackup="false"`; sin `FileProvider` |
| **RNF07** | Portabilidad | Android 8.0 (API 26) o superior | **Cumplido.** `minSdk = 26`. Decodificación con submuestreo y modelo mapeado en memoria para contener el uso de RAM |
| **RNF08** | Tamaño de la aplicación | ≤ 100 MB incluido el modelo | **Cumplido con holgura.** APK de release medido: **17,1 MB** (debug sin minificar: 38,7 MB). Quedan unos 83 MB de margen para el `.tflite`, muy por encima de lo que ocupa un clasificador móvil típico |

---

## Historias de usuario

| HU | Cubierta por |
|---|---|
| HU01 — Captura con cámara | RF01 |
| HU02 — Selección desde galería | RF02 |
| HU03 — Clasificación | RF03 |
| HU04 — Nivel de confianza | RF04 |
| HU05 — Sin conexión | RF05 |
| HU06 — Consulta del historial | RF06 |
| HU07 — Información sobre monilia | RF07 |
| HU08 — Uso de la aplicación | RF08 |

> **Defecto detectado en el documento IEEE830:** el texto de HU02 se titula «Selección de imagen
> desde galería» pero su descripción repite la de HU03 («quiero que la aplicación analice la vaina
> durante el escaneo…»). Debe corregirse en el documento. El código implementa la funcionalidad
> correcta según RF02.

---

## Fuera de alcance: verificación de que NO se implementó

| *Won't have* del IEEE830 | ¿Se respetó? |
|---|---|
| Diagnóstico de otras enfermedades | Sí. `PodCondition` tiene exactamente dos valores |
| Recomendación de tratamientos o dosis | Sí. `InfoContent` es descriptivo; incluye un apartado explícito «Qué NO hace esta aplicación» |
| Localización de la lesión sobre la vaina | Sí. Clasificación de la imagen completa; no hay detección ni segmentación |
| Gestión de múltiples usuarios con cuentas | **Parcial: cambio de alcance declarado.** Se añadió un acceso administrativo con contraseña única, a solicitud posterior del curso. NO hay registro, ni varios usuarios, ni perfiles, ni roles, ni servidor, ni base de datos de credenciales. Documentado en `06-seguridad-y-autorizacion.md`, apartado 1 bis |
| Servidores o bases de datos remotas | Sí. Sin permiso de INTERNET; ninguna dependencia de red en el proyecto |

---

## Otros defectos detectados en el IEEE830

1. **La sección «Diagrama de Casos de Uso» no contiene ninguna figura.** El archivo `.docx` solo
   incluye dos imágenes: el logotipo de UTS y el logotipo de CacaoScan.
2. **Inconsistencia sobre las imágenes del historial.** RF06 lista como entradas solo «categoría,
   nivel de confianza y fecha», pero RNF06 y el Alcance dan por hecho que las imágenes se almacenan.
   Se resolvió almacenándolas (reescaladas) y documentándolo.
3. **RNF08 es ambiguo:** «tamaño total de la aplicación» no distingue entre el tamaño del APK y el
   crecimiento de los datos del usuario. Se interpretó como tamaño del APK, y además se acotó el
   crecimiento de los datos reescalando cada imagen a 1024 px de lado mayor.
