# Seguridad, autenticación y autorización

Este documento responde al requisito académico sobre **seguridad en autenticación y autorización**
y explica, con honestidad, qué se implementó, qué no, y por qué.

---

## 1. El conflicto, planteado con precisión

Los apuntes de clase describen:

- **Autenticación (login):** OTP por SMS, SSO (Single Sign-On).
- **Autorización (comprobación de permisos):** HTTP, autorización por API, OAuth 2.0, JWT.

El IEEE830 de CacaoScan excluye textualmente del alcance (*Won't have*):

- «La gestión de múltiples usuarios con cuentas de acceso.»
- «El uso de servidores o bases de datos remotas.»

Y exige, con prioridad **Alta**:

- **RNF02:** «Las funciones principales deben operar completamente sin conexión a internet.»
- **RNF06:** «Cero transmisión de datos del usuario a servidores externos.»

**El conflicto no es de dificultad, es estructural.** OTP por SMS necesita una pasarela de
mensajería. SSO y OAuth 2.0 necesitan un proveedor de identidad. Los tres exigen un segundo actor en
la red. Implementarlos obligaría a violar dos *Won't have* y dos requisitos no funcionales de
prioridad Alta.

Por eso el requisito se resolvió **separando lo que sí puede implementarse de verdad de lo que solo
puede documentarse**, en lugar de simular un login falso.

---

## 1 bis. Cambio de alcance aprobado: acceso administrativo

> **Declaración explícita.** El IEEE830 original excluye del alcance «la gestión de múltiples
> usuarios con cuentas de acceso». Con posterioridad a la aprobación de ese documento, la dirección
> del curso solicitó añadir **un acceso administrativo**. Se implementó, y este apartado deja
> constancia de la diferencia en lugar de disimularla.

**Qué se implementó:** una **única credencial administrativa local**, solicitada al abrir la
aplicación.

**Qué se sigue sin implementar, y por tanto el *Won't have* se mantiene en lo esencial:**

| Elemento propio de «gestión de usuarios» | ¿Existe en CacaoScan? |
|---|---|
| Registro de usuarios | No |
| Varios usuarios o perfiles | No. Una sola credencial |
| Roles y permisos por usuario | No |
| Servidor de autenticación | No |
| Recuperación por correo o SMS | No. No hay red |
| Base de datos de credenciales | No. Un único registro cifrado en el dispositivo |

La lectura correcta es que CacaoScan tiene un **bloqueo de aplicación con contraseña**, no un sistema
de cuentas. Aun así, es un cambio respecto al documento aprobado y se documenta como tal.

**Impacto sobre RNF03.** El requisito mide que «un usuario nuevo completa una clasificación sin ayuda
externa». El acceso administrativo añade un paso previo a ese flujo. A partir de este cambio, RNF03
se evalúa **sobre el flujo de clasificación**, contado desde después del acceso. La prueba de
usabilidad del guion de pruebas debe ejecutarse con la sesión ya iniciada.

### Cómo está construido

| Aspecto | Decisión | Por qué |
|---|---|---|
| Almacenamiento | **Nunca se guarda la contraseña**, solo su derivación | Si alguien extrae el archivo, no obtiene la contraseña |
| Derivación | **PBKDF2-HMAC-SHA256, 100.000 iteraciones** | Cada intento de fuerza bruta cuesta lo mismo que un login legítimo |
| Sal | 16 bytes aleatorios por instalación | Inutiliza las tablas precalculadas; dos equipos con la misma contraseña no comparten hash |
| Capa adicional | El registro se cifra con **AES/GCM con clave del Android Keystore** | Impide un ataque de diccionario *offline* sobre el hash extraído |
| Intentos fallidos | **5 intentos y bloqueo de 60 s**, persistido | Reiniciar la aplicación no reinicia el bloqueo, que es lo que intentaría un atacante |
| Comparación | Tiempo constante | Una comparación con salida temprana filtra información |
| Recuperación | **No existe** | No hay servidor. Se advierte en pantalla antes de crear la contraseña |

Archivos: `security/credentials/PasswordHasher.kt`, `security/credentials/AdminCredentialStore.kt`,
`security/keystore/KeystoreCipher.kt`, `presentation/auth/`.

Cubierto por **17 pruebas unitarias** (`PasswordHasherTest`, `AdminCredentialStoreTest`), incluida la
persistencia del bloqueo tras reiniciar y la verificación de que la contraseña no aparece en claro en
lo almacenado.

### Relación con el resto del módulo de seguridad

Las tres piezas se reparten el trabajo sin solaparse:

| Pieza | Responde a |
|---|---|
| **Login administrativo** (contraseña) | ¿Quién entra a la aplicación? |
| **AppLock** (biometría) | ¿Es el dueño del dispositivo, para una acción concreta? |
| **JWT con scopes** | Una vez dentro, ¿qué está autorizado a hacer? |

Al iniciar sesión correctamente se abre la `SecuritySession` y se emite el token local, de modo que
el login alimenta directamente al mecanismo de autorización que ya existía.

---

## 1 ter. Segundo cambio de alcance: red para el módulo de administración

Para mostrar los dos microservicios dentro de la aplicación se añadieron los
permisos `INTERNET` y `ACCESS_NETWORK_STATE`. El detalle está en
`docs/12-microservicios-en-la-app.md`. Lo relevante para seguridad:

- **El flujo del productor sigue sin red.** El único código que abre un socket
  está en `data/remote` y solo lo alcanzan las pantallas de `presentation/admin`.
- **La autorización deja de ser solo demostración.** Se añadieron los scopes
  `servicios:leer` y `servicios:escribir`; las pantallas de administración los
  exigen en cada carga y en cada escritura.
- **Al microservicio de calidad solo viajan metadatos** de una foto (nombre,
  dimensiones, peso y clases anotadas), nunca los píxeles.
- **Riesgo asumido:** tráfico HTTP sin cifrar en la red local. Mitigación futura:
  TLS en los servicios y dominio restringido en la política de red.

## 2. Autenticación local real: `AppLock`

**Qué es.** Bloqueo opcional de la aplicación mediante `BiometricPrompt`: huella, rostro o la
credencial del dispositivo (PIN, patrón o contraseña).

**Por qué es autenticación de verdad y no una simulación:**

- Existe un factor de autenticación real (biométrico o conocimiento).
- Quien verifica la identidad es **el sistema operativo**, no CacaoScan.
- La aplicación nunca ve, ni almacena, ni transmite la huella ni el PIN: recibe únicamente un
  resultado de éxito o de fallo.
- La clave asociada vive en el **Android Keystore** y solo se libera tras la autenticación.

**Por qué no rompe el IEEE830:**

| Restricción del IEEE830 | ¿Se respeta? | Motivo |
|---|---|---|
| Sin cuentas de acceso | Sí | No hay registro, ni usuario, ni contraseña propia de la app |
| Sin múltiples usuarios | Sí | Un solo sujeto: el dueño del dispositivo |
| Sin servidores | Sí | Todo ocurre en el sistema operativo local |
| Funciona sin internet | Sí | No hay ninguna llamada de red |

**Es opcional.** El flujo principal (RF01–RF05: escanear, clasificar, ver resultado) **nunca** queda
detrás de esta autenticación. Si el dispositivo no tiene bloqueo configurado, la aplicación funciona
igual.

---

## 3. Autorización mediante JWT: demostración académica

`com.cacaoscan.security.authz.LocalTokenService` emite y verifica un **JSON Web Token real**
(RFC 7519), no una imitación:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 . eyJpc3MiOiJjYWNhb3NjYW4tbG9jYWwiLCJzdWIiOi... . <firma>
      cabecera Base64URL                        payload Base64URL                      HMAC-SHA256
```

- **Algoritmo:** HS256 (HMAC-SHA256).
- **Clave:** generada en el dispositivo y guardada en el **Android Keystore**. No existe como
  literal en el código ni en el APK; es distinta en cada instalación; en dispositivos con hardware
  seguro no puede extraerse.
- **Claims:** `iss`, `sub`, `iat`, `exp` y `scope`.
- **Vigencia:** 15 minutos.

### Qué demuestra este módulo

1. **La estructura real de un JWT** y, en particular, que el payload va **codificado en Base64, no
   cifrado**: cualquiera puede leerlo. Por eso el token no contiene ningún dato sensible.
2. **Verificación de firma.** Alterar un claim invalida la firma y el token se rechaza. Hay una
   prueba automatizada que modifica el `scope` de un token válido y comprueba que se rechaza
   (`LocalTokenServiceTest`).
3. **Expiración** mediante el claim `exp`.
4. **Control de acceso por scopes:** `history:read`, `history:delete`, `history:purge`.
5. **El orden correcto de verificación:** primero la firma, después los claims. Interpretar claims
   de un token sin verificar antes su firma es el error clásico al usar JWT.

### Qué NO resuelve, y hay que decirlo

- **No hay confianza entre partes distintas.** El valor real de JWT es que un servidor emite el
  token y **otro servicio** lo verifica sin consultar al primero. Aquí el emisor y el verificador
  son la misma aplicación, así que la firma no aporta garantía frente a terceros.
- **No hay revocación.** Un token emitido vale hasta que expira.
- **No protege frente a un atacante con acceso físico y privilegios de root.**
- **No es autenticación.** Quien autentica es el sistema operativo; este módulo solo decide *qué*
  puede hacerse una vez autenticado.

### Dónde se usa realmente

Una sola acción de la aplicación está protegida: **«Borrar todo el historial»**, en la pantalla de
Historial. El recorrido completo es autenticación → emisión del token → comprobación del permiso
`history:purge` → borrado. Está cubierto por pruebas: si la autenticación se cancela, o si el
dispositivo no tiene bloqueo configurado, **no se borra nada**; y la sesión se cierra en cuanto
termina la operación, sin dejar una autorización destructiva abierta.

Además hay una pantalla **«Seguridad local»** (icono de escudo en la pantalla de inicio) que muestra
el token realmente emitido —cabecera y payload legibles, firma recortada— y permite comprobar en
vivo la concesión y la denegación de un permiso. Existe para poder explicar el mecanismo en la
sustentación.

### Independencia del flujo principal

Deliberadamente **no existe** un scope para escanear o clasificar. El módulo solo protege acciones
secundarias y destructivas. Si se elimina el paquete `com.cacaoscan.security` completo, CacaoScan
sigue capturando, clasificando, guardando y consultando revisiones.

---

## 4. OTP, SSO y OAuth 2.0: análisis comparativo

| Mecanismo | Qué problema resuelve | Qué infraestructura exige | ¿Aplicable a CacaoScan? |
|---|---|---|---|
| **OTP por SMS** | Segundo factor: demuestra posesión de una línea telefónica | Pasarela SMS + servidor que genere y valide el código | **No.** Exige red y servidor; contradice RNF02 |
| **TOTP (RFC 6238)** | Segundo factor sin red: código derivado de un secreto y del tiempo | Un secreto compartido en el dispositivo | **Técnicamente sí**, pero implica crear un «login» que el alcance excluye. Queda documentado como opción no implementada |
| **SSO** | Una identidad única para acceder a varios sistemas | Proveedor de identidad centralizado | **No.** CacaoScan es un único sistema local, sin cuentas: no hay nada que federar |
| **OAuth 2.0** | Delegar acceso a recursos de un tercero sin ceder credenciales | Servidor de autorización + servidor de recursos | **No.** No hay recursos de terceros ni servidores |
| **Autorización por API / HTTP** | Controlar el acceso a los extremos de un servicio | Un servicio HTTP | **No.** La aplicación no declara ni siquiera el permiso de internet |
| **JWT** | Transportar claims verificables entre partes | Ninguna, si emisor y verificador coinciden | **Sí, como demostración**, con las limitaciones ya explicadas |

La conclusión académica es que entender **cuándo un mecanismo no aplica** es tan importante como
saber implementarlo. Añadir OAuth 2.0 a una aplicación offline de un solo usuario no la haría más
segura: le añadiría una dependencia de red que el propio documento de requisitos prohíbe.

---

## 5. Seguridad local efectivamente implementada

### 5.1 Ausencia del permiso de INTERNET

`AndroidManifest.xml` **no declara** `android.permission.INTERNET`.

Consecuencia: el sistema operativo impide a la aplicación abrir cualquier conexión de red. RNF02 y
RNF06 dejan de ser una promesa del código y pasan a ser una **garantía verificable de la
plataforma**. Se comprueba abriendo el manifiesto.

Coste asumido y consciente: no hay analítica ni reporte remoto de errores.

### 5.2 Permisos mínimos

Lista completa, verificada sobre el **manifiesto fusionado** del APK de release (no sobre el
manifiesto fuente, que no incluiría lo que aportan las dependencias):

| Permiso | Origen | Tipo |
|---|---|---|
| `CAMERA` | RF01, declarado por la app | Peligroso; se pide al abrir la pantalla de escaneo |
| `USE_BIOMETRIC` | `androidx.biometric` | Normal |
| `USE_FINGERPRINT` | `androidx.biometric` | Normal, heredado para versiones antiguas |

No se pide ningún permiso de almacenamiento: la selección desde galería usa el Selector de Fotos del
sistema (`PickVisualMedia`), que no lo requiere.

**Permiso de red eliminado activamente.** CameraX arrastra de forma transitiva
`androidx.media3:media3-common`, que declara `ACCESS_NETWORK_STATE`. Aunque no es un permiso
peligroso, CacaoScan no consulta el estado de la red en ningún punto, así que se elimina del
manifiesto fusionado con `tools:node="remove"`. Comprobar solo el manifiesto fuente no habría
detectado este permiso: hay que revisar el fusionado.

### 5.3 Almacenamiento

- Las imágenes van al **almacenamiento interno** (`filesDir`), aislado por el sandbox de Android.
- **No se expone ningún `FileProvider`**: no hay forma de sacar esos archivos de la aplicación.
- `android:allowBackup="false"`: las fotografías del cultivo no se copian a la nube de respaldo.
- Los nombres de archivo se generan con UUID y se **validan** igualmente (`SafeFileName`), de modo
  que un dato corrupto en la base de datos no pueda alcanzar rutas fuera del directorio.

### 5.4 Registro de información

No se registran en Logcat imágenes, rutas de archivos del usuario ni contenido de tokens. Los
mensajes de error técnicos se conservan en el estado interno pero **nunca se muestran al productor**:
la interfaz muestra mensajes redactados en lenguaje claro.

### 5.5 Validación de entrada

- Se rechazan imágenes menores de 64 px de lado (`ClassifyPodImageUseCase`).
- Se verifica que el archivo seleccionado sea decodificable como imagen antes de usarlo.
- Se corrige la orientación EXIF antes de la inferencia.
- Los valores de confianza fuera de rango se recortan en lugar de propagarse.

---

## 6. Resumen para la sustentación

> CacaoScan implementa **autenticación local real** delegada en el sistema operativo
> (`BiometricPrompt` + Android Keystore) y **autorización real por scopes** representada en un JWT
> firmado con HMAC-SHA256, cuya clave nunca sale del Keystore. OTP, SSO y OAuth 2.0 se analizaron y
> se descartaron porque exigen infraestructura de red que el IEEE830 excluye explícitamente del
> alcance. La demostración JWT está claramente etiquetada como tal, está desacoplada del flujo
> principal, y su documentación indica de forma expresa qué problema resuelve y cuál no.
