# Ciclo DevOps aplicado a CacaoScan

Las ocho fases del ciclo DevOps, traducidas a lo que significan realmente en una
aplicacion movil **que funciona sin conexion y no tiene servidor**.

---

## 0. La traduccion es el punto, no el diagrama

El diagrama clasico de DevOps asume un sistema con servidores: se despliega a
produccion, se opera una infraestructura y se monitorea con telemetria remota.

**CacaoScan no tiene nada de eso, y es a proposito.** El IEEE830 excluye del
alcance el uso de servidores y bases de datos remotas, y RNF02/RNF06 exigen
operacion sin conexion y cero transmision de datos. La aplicacion ni siquiera
declara `android.permission.INTERNET`.

Por lo tanto, tres fases **no se saltan: se traducen**.

| Fase | Lectura ingenua | Lectura correcta aqui |
|---|---|---|
| Deploy | Subir a un servidor | Hacer llegar el APK a un telefono real |
| Operate | Administrar infraestructura | Gestionar versiones publicadas y reversiones |
| Monitor | Telemetria remota del usuario | Metricas del pipeline + diagnostico en el dispositivo |

> **Riesgo que hay que evitar expresamente:** "resolver" la fase de monitoreo
> anadiendo Crashlytics, Sentry o Firebase Analytics. Los tres exigen permiso de
> red y romperian RNF06, que es el argumento de privacidad mas fuerte del
> proyecto. Esta prohibicion queda escrita en la tarjeta correspondiente del
> tablero.

---

## 1. Plan

**Que significa:** convertir el IEEE830 en un backlog ejecutable y trazable.

**Herramientas:** Trello (tablero `CacaoScan — DevOps`), opcionalmente GitHub
Issues para enlazar tareas con commits.

**Trabajo concreto**
- Tablero con una lista por fase y etiquetas de estado.
- Cada tarjeta referencia el RF o RNF que justifica su existencia.
- Definition of Done acordado por el equipo.
- Convencion de ramas y de mensajes de commit.

**Evidencia:** el propio tablero, con trazabilidad visible hacia el IEEE830.

---

## 2. Code

**Que significa:** control de versiones, estandares de codigo y revision entre
pares.

**Herramientas:** Git + GitHub, detekt (analisis estatico de Kotlin), Android
Lint (ya integrado en el build).

**Trabajo concreto**
- Inicializar el repositorio y subirlo como privado.
- Verificar que `local.properties` queda fuera del control de versiones.
- Anadir detekt y corregir los hallazgos criticos.
- Ramas: `main` protegida, `develop`, `feature/*`.
- Plantilla de Pull Request y al menos un PR revisado por el otro integrante.

**Evidencia:** historial de commits, un PR con comentarios de revision, informe
de detekt.

**Punto de partida real:** el proyecto ya pasa `lintDebug` con 0 errores y 16
avisos, todos documentados en `03-decisiones-tecnicas.md`. Eso ya es analisis
estatico; detekt lo complementa con metricas de complejidad.

---

## 3. Build

**Que significa:** que cualquiera pueda compilar el proyecto de forma
reproducible, y que la maquina lo haga sola en cada cambio.

**Herramientas:** Gradle (ya configurado con version catalog) + GitHub Actions.

**Trabajo concreto**
- Workflow `build.yml` que corre en cada push y cada PR.
- JDK 17 Temurin y cache de Gradle en CI.
- `./gradlew assembleDebug`.
- Badge de estado en el README.
- APK de debug publicado como artefacto de la ejecucion.

**Evidencia:** historial de ejecuciones verdes en la pestana Actions.

---

## 4. Test

**Que significa:** que ninguna regresion llegue a `main` sin ser detectada.

**Herramientas:** JUnit (39 pruebas ya escritas), GitHub Actions,
`reactivecircus/android-emulator-runner` para las instrumentadas, Jacoco para
cobertura.

**Trabajo concreto**
- `testDebugUnitTest` en CI.
- `lintDebug` en CI, fallando ante errores.
- **Ejecutar `ScanDaoTest` en un emulador dentro del pipeline.**
- Jacoco publicando el porcentaje de cobertura.
- Branch protection: sin pruebas verdes no hay merge.

**Evidencia:** CI que bloquea merges rotos.

**Por que esta fase cierra un hueco real:** `09-pruebas.md` documenta que las
cinco pruebas instrumentadas de Room estan escritas pero **nunca se han
ejecutado**, por falta de dispositivo. El pipeline resuelve exactamente eso.

---

## 5. Release

**Que significa:** convertir un commit en un artefacto instalable, firmado,
versionado e identificable.

**Herramientas:** etiquetas de Git, GitHub Releases, keystore de firma, GitHub
Secrets.

**Trabajo concreto**
- Generar el keystore y guardar sus credenciales como secretos del repositorio,
  nunca en el codigo.
- `signingConfigs` de release en `app/build.gradle.kts`.
- Derivar `versionName` y `versionCode` desde la etiqueta de Git, para que la
  version del APK y la del repositorio no puedan divergir.
- Workflow `release.yml` disparado por etiquetas `v*`.
- `CHANGELOG.md`.

**Evidencia:** una GitHub Release con el APK firmado adjunto automaticamente.

---

## 6. Deploy

**Que significa aqui:** no hay servidor. Desplegar es **poner el APK en manos de
alguien que lo instale**.

**Herramienta:** Firebase App Distribution.

> **Detalle critico:** Firebase App Distribution **no requiere anadir el SDK de
> Firebase a la aplicacion**. El APK se sube desde el workflow con la CLI y el
> tester lo instala. El SDK solo haria falta para avisos de actualizacion dentro
> de la app, y *ese* si exigiria permiso de red. Por eso se usa unicamente como
> canal de distribucion externo: la aplicacion sigue sin acceso a internet y
> RNF06 queda intacto.

**Trabajo concreto**
- Proyecto de Firebase creado solo para distribucion.
- Grupo de testers: equipo y docente.
- Subida automatica del APK firmado desde el workflow de release.
- Verificacion de instalacion en un telefono de gama media real.

**Alternativa sin Firebase:** adjuntar el APK a la GitHub Release y compartir el
enlace. Menos comodo, cero configuracion.

---

## 7. Operate

**Que significa aqui:** no hay infraestructura que operar. Operar es **gestionar
el ciclo de vida de las versiones publicadas**.

**Herramientas:** GitHub Releases como historico, un runbook escrito.

**Trabajo concreto**
- Runbook: como se instala, como se revierte, a quien se contacta.
- Politica de versionado semantico.
- **Ejecutar un rollback real** a la version anterior y documentarlo.
- Procedimiento ante un reporte de fallo, teniendo en cuenta que no hay
  telemetria: el reporte llega por voz o por escrito, no por un panel.

**Evidencia:** el runbook y un rollback efectivamente probado.

---

## 8. Monitor

**Que significa aqui:** se monitorean el pipeline y el dispositivo, **no al
usuario**.

### 8.1 Monitoreo del pipeline
- Duracion de los builds.
- Porcentaje de ejecuciones con pruebas en verde.
- **Tendencia del tamano del APK frente al limite de 100 MB de RNF08.** Hoy son
  17,1 MB sin modelo; cuando entre el `.tflite` esa cifra sube y conviene verla
  evolucionar.
- Numero de avisos de lint y de detekt a lo largo del tiempo.

### 8.2 Monitoreo en el dispositivo
La aplicacion **ya mide y guarda el tiempo de inferencia de cada clasificacion**
en el historial. Esa es la evidencia de RNF01 (menos de 5 segundos). Falta una
pantalla de diagnostico que calcule la mediana y el percentil 95 a partir de los
registros locales.

### 8.3 Lo que NO se hace, y por que
Sin `android.permission.INTERNET` no es posible usar Crashlytics, Sentry ni
analitica remota. **No es una limitacion tecnica: es la consecuencia directa de
una decision de requisitos.** Si algun dia el proyecto se publicara en Google
Play, los *Android vitals* de Play Console reportan fallos sin que la aplicacion
necesite permiso de red, porque quien reporta es el sistema operativo y no el
codigo de la app. Esa es la unica via de monitoreo remoto compatible con RNF06.

---

## Orden de ejecucion recomendado

| Bloque | Fases | Razon |
|---|---|---|
| A | Plan + Code | Sin repositorio no existe ninguna de las demas fases |
| B | Build + Test | Donde esta el grueso del valor demostrable |
| C | Release + Deploy | Depende de que B funcione |
| D | Operate + Monitor | Sobre todo documentacion y una pantalla nueva |

Si el tiempo se agota, un pipeline de Build y Test impecable demuestra mas que
una cadena completa a medio terminar.
