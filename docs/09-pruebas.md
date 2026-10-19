# Estrategia de pruebas

## 1. Qué se prueba automáticamente hoy

**56 pruebas unitarias, todas en verde**, ejecutables en la JVM sin emulador:

```
gradlew :app:testDebugUnitTest
```

| Clase de prueba | Nº | Qué cubre |
|---|---|---|
| `OutputInterpreterTest` | 8 | Softmax, sigmoide, logits sin normalizar, **orden de etiquetas**, recorte de valores fuera de rango, salidas malformadas |
| `LocalTokenServiceTest` | 9 | Estructura del JWT, firma válida, **payload alterado**, clave distinta, expiración, scopes, cadenas no válidas |
| `ScanRepositoryImplTest` | 7 | Guardado, orden del historial, borrado en cascada de la imagen, imagen huérfana tras fallo de escritura, marca de resultado simulado |
| `ConfidenceTest` | 4 | Redondeo a porcentaje, recorte, `NaN` |
| `SafeFileNameTest` | 4 | Rechazo de recorridos de ruta y separadores |
| `HistoryViewModelTest` | 5 | Estado vacío ≠ error; mapeo de registros a filas; **borrado total denegado si la autenticación falla o el dispositivo no tiene bloqueo**; borrado efectivo tras autenticarse; cierre de la sesión autorizada |
| `ScanDateFormatterTest` | 2 | Zona horaria y cambio de día |
| `AdminCredentialStoreTest` | 10 | Creación, intentos restantes, **bloqueo tras 5 fallos**, **persistencia del bloqueo tras reiniciar**, expiración, registro corrupto |
| `PasswordHasherTest` | 7 | Verificación correcta e incorrecta, **sal aleatoria**, reproducibilidad, **la contraseña no aparece en lo almacenado** |

Las dos pruebas que más valor aportan son deliberadas:

- **Orden de etiquetas** (`OutputInterpreterTest`): un `labelOrder` invertido no produce ningún
  error visible, solo diagnósticos equivocados. Es el fallo silencioso más peligroso del proyecto.
- **Payload alterado** (`LocalTokenServiceTest`): modifica el `scope` de un token válido y comprueba
  que se rechaza. Es exactamente lo que JWT debe garantizar.

### Prueba instrumentada escrita, aún no ejecutada

`ScanDaoTest` (5 pruebas) valida las consultas SQL reales contra una base Room en memoria. **No se ha
ejecutado todavía** porque requiere un dispositivo o emulador conectado:

```
gradlew :app:connectedDebugAndroidTest
```

---

## 2. Qué NO está cubierto por pruebas automáticas, y por qué

| Área | Motivo | Cómo se verifica |
|---|---|---|
| CameraX | Necesita hardware real; los emuladores simulan la cámara de forma poco fiel | Prueba manual, guion en la sección 3 |
| `BitmapLoader`, `InternalImageStorage` | Dependen de `Bitmap` y del sistema de archivos de Android | Prueba instrumentada pendiente + verificación manual |
| `ScanViewModel` | Su flujo requiere instancias reales de `Bitmap`, que no existen en la JVM | Requeriría añadir Robolectric o una librería de dobles; se descartó por peso. Las reglas que sí son puras (validación de tamaño de imagen) están en `ClassifyPodImageUseCase` |
| `KeystoreHmacSigner` | El Android Keystore solo existe en el dispositivo | La lógica del token se prueba con un firmante HMAC equivalente en la JVM; lo que no se prueba es el almacenamiento de la clave, que es responsabilidad del sistema |
| Pantallas Compose | Se priorizaron las pruebas de lógica | Pendiente: `androidx.compose.ui.test` ya está declarado |

---

## 3. Guion de pruebas manuales

### 3.1 Cámara (RF01)

1. Instalar con la aplicación sin permisos concedidos.
2. Inicio → «Escanear vaina». **Esperado:** no se pide el permiso al abrir la app, sino aquí.
3. Denegar el permiso. **Esperado:** la pantalla sigue siendo utilizable y «Elegir de la galería»
   funciona. *No debe haber un callejón sin salida.*
4. Conceder el permiso, tomar una foto. **Esperado:** aparece la vista previa con la orientación
   correcta (no acostada).

### 3.2 Galería (RF02)

1. «Elegir de la galería» → seleccionar una foto. **Esperado:** no se pide ningún permiso de
   almacenamiento.
2. Elegir una foto tomada en vertical. **Esperado:** se muestra en vertical.
3. Cancelar el selector. **Esperado:** se vuelve a la pantalla sin errores.

### 3.3 Funcionamiento sin conexión (RF05, RNF02)

1. Activar **modo avión**.
2. Recorrer el flujo completo: escanear → analizar → resultado → guardar → historial → información.
   **Esperado:** todo funciona igual.
3. Verificación adicional y más fuerte: abrir `AndroidManifest.xml` y comprobar que **no aparece**
   `android.permission.INTERNET`. Sin ese permiso la app no puede conectarse aunque quisiera.

### 3.4 Persistencia (RNF05)

1. Guardar dos revisiones.
2. Cerrar la aplicación por completo (deslizarla fuera de recientes).
3. Reabrir → Historial. **Esperado:** las dos revisiones siguen ahí, con su fecha, categoría,
   confianza y fotografía.
4. Abrir una y eliminarla. **Esperado:** desaparece de la lista y su fotografía deja de mostrarse.

### 3.5 Tiempo de clasificación (RNF01)

**Solo tiene sentido con el modelo real instalado.**

1. En un dispositivo de gama media, realizar 10 clasificaciones.
2. Anotar el valor «Tiempo de análisis: X ms» de la pantalla de resultado (también queda guardado en
   cada registro del historial).
3. **Criterio:** las 10 por debajo de 5000 ms. Registrar el peor caso, no la media.

### 3.6 Modo demostración

1. Sin el archivo `.tflite` en `assets`: **debe** aparecer el aviso «MODO DEMOSTRACIÓN» en la
   pantalla de resultado, y los registros del historial deben mostrar «Resultado simulado».
2. Con el `.tflite` presente: el aviso **debe** desaparecer.
3. Con un `.tflite` corrupto: debe volver al modo demostración, no fallar.

### 3.7 Usabilidad (RNF03)

Sentar a una persona que no haya visto la aplicación, sin darle indicaciones, y pedirle que averigüe
si una vaina está sana. **Criterio del IEEE830:** completa la clasificación sin ayuda externa.
Anotar dónde duda.

### 3.8 Rotación de pantalla

1. Iniciar un análisis y girar el teléfono mientras aparece «Analizando la vaina…».
2. **Esperado:** el análisis continúa y el resultado aparece. No debe reiniciarse ni perderse.
   *Es la prueba que justifica la elección de MVVM.*

---

## 4. Comandos

```bash
# Pruebas unitarias (JVM, sin emulador)
gradlew :app:testDebugUnitTest

# Pruebas instrumentadas (requiere dispositivo o emulador)
gradlew :app:connectedDebugAndroidTest

# Compilar e instalar
gradlew :app:installDebug
```
