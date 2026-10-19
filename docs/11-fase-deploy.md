# Fase DEPLOY — implementación en CacaoScan

Documento de exposición de la fase asignada al equipo dentro del ciclo DevOps.

---

## 1. Qué significa «desplegar» una aplicación móvil

En un sistema web, desplegar es copiar el código a un servidor y reiniciar el
servicio. **CacaoScan no tiene servidor.** El IEEE830 lo excluye del alcance y
la aplicación ni siquiera declara permiso de red.

Desplegar aquí significa otra cosa:

> **Hacer que un APK firmado, versionado y trazable llegue al teléfono de alguien
> que pueda instalarlo, sin que nadie copie archivos a mano.**

Esa traducción es el punto de la fase. Si se despliega pasando el APK por
WhatsApp, no hay despliegue: hay un archivo compartido. Faltan versionado,
firma, trazabilidad, control de quién lo recibió y capacidad de revertir.

### Por qué desplegar no contradice el funcionamiento sin conexión

Es la pregunta que surge siempre, y conviene responderla antes de que la hagan:

| Momento | ¿Hay red? | ¿Dónde ocurre? |
|---|---|---|
| Compilar y firmar el APK | Sí | Servidor de CI (GitHub Actions) |
| Subir el APK a la plataforma | Sí | Servidor de CI |
| Descargar el APK | Sí | Navegador del tester, **antes de instalar** |
| **Usar la aplicación** | **No** | **El teléfono, sin ninguna conexión** |

**El teléfono necesita Internet una vez, para recibir la aplicación. La
aplicación no lo necesita nunca.** Son dos cosas distintas. El manifiesto
fusionado del APK de release sigue declarando únicamente `CAMERA`,
`USE_BIOMETRIC` y `USE_FINGERPRINT`.

---

## 2. Herramienta elegida: Firebase App Distribution

### Cómo la implementamos

El despliegue está automatizado en `.github/workflows/deploy.yml` y se dispara
**al publicar una etiqueta de versión**:

```
git tag v0.2.0
git push origin v0.2.0
```

A partir de ahí, sin intervención manual:

```
Etiqueta v0.2.0
   │
   ├─ 1. Deriva versionName=0.2.0 de la etiqueta
   │     (imposible que el APK y el repositorio tengan versiones distintas)
   │
   ├─ 2. Reconstruye el keystore desde un secreto en base64
   │
   ├─ 3. Compila y FIRMA el APK de release
   │
   ├─ 4. Verifica que el APK quedó firmado; si no, falla el pipeline
   │
   ├─ 5. Crea una GitHub Release con el APK adjunto  ← histórico y rollback
   │
   ├─ 6. Distribuye a los testers con la CLI de Firebase
   │
   └─ 7. Borra las credenciales del runner
```

**Decisión técnica a destacar:** usamos la **CLI oficial de Firebase**
(`firebase appdistribution:distribute`) y **no** una acción de terceros de la
comunidad. Reduce la dependencia de un proyecto que puede quedar sin
mantenimiento, y convierte una eventual migración a otra plataforma en el cambio
de un solo paso del workflow.

### Lo que NO se añadió a la aplicación

| Elemento | ¿Está en la app? |
|---|---|
| SDK de Firebase | No |
| `google-services.json` | No |
| Permiso `INTERNET` | No |
| Cualquier llamada de red | No |

Solo se registró el proyecto en la consola de Firebase para obtener un **App
ID**, que viaja como parámetro de la CLI dentro del pipeline.

> **Prohibición explícita documentada:** el SDK de App Distribution existe y
> sirve para avisar de actualizaciones dentro de la app y recoger comentarios de
> testers. **No se usa.** Añadirlo exigiría permiso de red y rompería RNF06,
> que es el argumento de privacidad más fuerte del proyecto.

---

## 3. Ventajas frente a otras herramientas

| Herramienta | Ventajas | Desventajas |
|---|---|---|
| **Firebase App Distribution** ✅ | Gratuita sin límite de aplicaciones · **No exige SDK en la app** · Grupos de testers gestionados · Avisa automáticamente de versiones nuevas · Acepta APK y AAB · Sin revisión previa · Integra con cualquier CI | Depende de una cuenta de Google · El tester debe aceptar la invitación e instalar *Firebase App Tester* · No sirve para público general · Riesgo de dependencia de proveedor |
| **Google Play — pruebas internas** | Canal oficial · Actualizaciones automáticas · Play Protect · Android vitals sin permiso de red | **Cuota de 25 USD** · Verificación de identidad del desarrollador · Revisión previa de cada versión · Demasiado peso para un piloto académico |
| **GitHub Releases** | Cero configuración adicional · Ya vive en el repositorio · Versión atada a la etiqueta | El usuario debe habilitar «orígenes desconocidos» · Sin grupos ni notificaciones · No se sabe quién instaló qué |
| **Visual Studio App Center** | Era el estándar del sector | ⚠️ **Retirado el 31 de marzo de 2025** |
| **APK por WhatsApp o Drive** | Inmediato, cero configuración | Sin versionado, sin firma controlada, sin trazabilidad, sin rollback. **No es despliegue** |

### Las tres razones de la elección

1. **Coste cero y sin barrera de entrada.** Google Play exige 25 USD y
   verificación de identidad; para distribuir a un grupo de prueba y al docente
   es desproporcionado.
2. **No toca la aplicación.** Es la única opción con gestión de testers que no
   obliga a integrar nada dentro del APK. Preserva RNF02 y RNF06 intactos.
3. **Gestión real de testers.** Grupos, invitaciones, aviso automático de nueva
   versión y registro de quién instaló qué versión. GitHub Releases no da nada
   de eso.

### Sobre el riesgo de proveedor

**Visual Studio App Center era la herramienta estándar para esto y Microsoft la
retiró el 31 de marzo de 2025.** Es el motivo por el que la elección se hizo
evaluando el riesgo, no copiando un tutorial:

- Usamos la CLI oficial, no una acción comunitaria de terceros.
- **GitHub Releases actúa como respaldo:** el APK firmado queda adjunto a cada
  etiqueta. Si Firebase App Distribution desapareciera mañana, el histórico
  completo de versiones sigue disponible y el despliegue degrada a descarga
  directa. La fase no depende de un solo proveedor.

---

## 4. Desventajas, dichas por nosotros

Ninguna herramienta es gratis en consecuencias:

1. **No llega al productor real.** Firebase App Distribution sirve para la fase
   de pruebas piloto. Un productor de cacao en zona rural no va a aceptar una
   invitación por correo ni instalar *Firebase App Tester*. Para distribución
   real haría falta Google Play o entrega directa del APK.
2. **Dependencia de una cuenta de Google.** Mitigada por el respaldo en GitHub
   Releases, pero existe.
3. **Fricción de instalación.** El tester necesita permitir orígenes
   desconocidos o pasar por la aplicación de Firebase.
4. **El secreto de firma es un punto único de fallo.** Si se pierde el keystore,
   **no se pueden publicar actualizaciones** de esa aplicación: Android rechaza
   un APK firmado con otra clave. Por eso está fuera del repositorio, en
   `.gitignore`, y respaldado aparte.
5. **Requiere Internet en el momento de la entrega**, aunque no después. En una
   zona sin cobertura, la instalación hay que hacerla antes de salir a campo.
6. **Las versiones distribuidas caducan a los 150 días.** Firebase las retira
   automáticamente. Para un proyecto académico es irrelevante, pero significa
   que App Distribution **no sirve como archivo histórico**. Es otra razón por la
   que el APK firmado se adjunta además a la GitHub Release, que no caduca.

---

## 5. Evidencia para la sustentación

| Qué mostrar | Dónde |
|---|---|
| El pipeline de despliegue | `.github/workflows/deploy.yml` |
| Que la versión sale de la etiqueta | Ejecución del workflow, paso «Calcular la version» |
| Que el APK queda firmado | Paso «Verificar que el APK quedo firmado» |
| El histórico de versiones | Pestaña *Releases* del repositorio |
| Los testers y sus instalaciones | Consola de Firebase App Distribution |
| Que la app sigue sin red | Manifiesto fusionado del APK de release |
| Rollback | Descargar e instalar la Release anterior |

### Comprobación de que la versión se deriva de la etiqueta

Verificado localmente simulando lo que hace el pipeline:

```
./gradlew :app:assembleRelease -PversionName=0.2.0 -PversionCode=7
```

Resultado en `output-metadata.json`: `versionCode: 7`, `versionName: 0.2.0`.

---

## 6. Guion de la demostración en vivo

### Antes de entrar al aula

- [ ] *Firebase App Tester* instalado en el teléfono y sesión iniciada.
- [ ] Invitación de tester **ya aceptada** (no se acepta delante del público).
- [ ] CacaoScan **desinstalada** del teléfono, para poder instalarla en vivo.
- [ ] Capturas de pantalla de respaldo, por si no hay conexión en el aula.
- [ ] Al menos **dos versiones distribuidas**, para que el historial muestre un
      proceso y no un hecho aislado.

### Secuencia (4 minutos)

| # | Qué se muestra | Qué se dice |
|---|---|---|
| 1 | El comando de distribución en la terminal | «Desplegar no es pasar un archivo: es subir un artefacto identificado a un canal con control de acceso» |
| 2 | Consola de Firebase, pestaña Versiones | «Historial de versiones, notas y quién la recibió» |
| 3 | Correo o app *App Tester* en el teléfono | «El tester recibe aviso automático» |
| 4 | **Instalación en vivo y apertura de la app** | «Y aquí está la clave: el teléfono necesitó Internet una vez, para recibirla. La aplicación no lo necesita nunca» |
| 5 | **Activar modo avión y usar la app** | «Funciona igual. Deploy y operación sin conexión no se contradicen» |
| 6 | `.github/workflows/deploy.yml` | «Esto mismo, automatizado: una etiqueta de git dispara firma, publicación y distribución» |

El paso 5 es el que cierra el argumento. Si solo hay tiempo para uno, es ese.

### Plan B si no hay conexión

Mostrar las capturas y el archivo del workflow. La fase se sustenta igual: el
despliegue es código versionado, no una acción manual irrepetible.

---

## 7. Qué falta para que el despliegue funcione

Son pasos de configuración de cuentas, no de código:

1. Crear el repositorio en GitHub y subir el proyecto.
2. Generar el keystore de firma (`keytool`) y guardarlo fuera del repositorio.
3. Crear el proyecto en Firebase y registrar la aplicación para obtener el App ID.
4. Crear una cuenta de servicio con permiso de App Distribution.
5. Cargar cinco secretos en el repositorio:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
   `FIREBASE_SERVICE_ACCOUNT`, `FIREBASE_APP_ID`.
6. Crear el grupo de testers `testers` e invitar al equipo y al docente.
7. Publicar la primera etiqueta y ver el pipeline completo en verde.
