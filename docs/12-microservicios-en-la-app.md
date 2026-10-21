# 12 · Microservicios dentro de la aplicación

## Qué cambió

Hasta esta fase CacaoScan no declaraba ningún permiso de red y los dos
microservicios del proyecto se operaban solo desde el equipo. A petición
expresa, los microservicios deben **verse y usarse dentro de la aplicación**.
Se añade un **módulo de Administración** que los consume por la red local.

Es una **ampliación declarada de alcance**, no una deriva: se registra aquí, en
el manifiesto y en `docs/06-seguridad-y-autorizacion.md`.

## Qué NO cambió

El flujo del productor —RF01 a RF06: cámara, clasificación, resultado e
historial— **no hace ninguna petición de red**. La inferencia sigue siendo local
con el `.tflite` empaquetado, y ninguna fotografía ni resultado sale del teléfono.

| Requisito | Antes | Ahora |
|---|---|---|
| RNF02 · funciona sin conexión | Garantizado por el sistema (sin permiso) | Garantizado por la arquitectura: el escaneo no depende de la red |
| RNF05 · cero transmisión de datos del productor | Ídem | Se mantiene: el único código de red vive en `data/remote` y solo lo usa `presentation/admin` |

Lo que se pierde es honesto decirlo: la garantía ya no la impone el sistema
operativo por ausencia del permiso, sino el diseño del código.

## Arquitectura

```
presentation/admin/          4 pantallas + ViewModels
   │   exigen scope servicios:leer (y servicios:escribir para escribir)
   ▼
domain/repository/MicroserviciosRepository     ← única puerta a la red
   ▼
data/repository/MicroserviciosRepositoryImpl   ← traduce cada fallo a un motivo
   ▼
data/remote/
   TransporteHttp / ClienteHttp     HttpURLConnection, sin dependencias nuevas
   LectorJson                       JSON ↔ dominio con org.json (del sistema)
   ConfiguracionServicios           dirección del equipo (10.0.2.2 por omisión)
   MonitorConectividad              distingue "sin red" de "servicio caído"
```

**Cero dependencias nuevas.** Se usa `HttpURLConnection` y `org.json`, ambos
incluidos en Android. Retrofit u OkHttp añadirían peso al APK (RNF08) sin aportar
nada para un puñado de peticiones JSON a dos servicios conocidos.

**Las reglas de negocio no se duplican.** La app no valida versión semántica,
rangos, hash, 100 MB ni el umbral del 85 %. Lo hace el *Script del Validador* de
cada microservicio y la app muestra sus motivos. Dos copias de las mismas
reglas terminarían discrepando.

## Las pantallas

| Pantalla | Microservicio | Rutas | Qué se demuestra |
|---|---|---|---|
| Administración | ambos | `/salud`, `/version` | Dirección configurable y estado VIVO/CAÍDO con versión |
| Registro de Modelos | 1 · puerto 8001 | `GET /modelos`, `GET /resumen`, `POST /modelos` | Tres ejemplos: apto, bajo el 85 % (se registra como NO APTO) e inválido (el validador lo rechaza con cada motivo) |
| Control de Calidad | 2 · puerto 8002 | `POST /validaciones`, `GET /validaciones[/rechazadas]`, `GET /resumen` | Validar una **foto real del teléfono** (solo viajan metadatos) y el caso de etiqueta ambigua de la fase CRISP-DM |
| Monitoreo | ambos | `/metricas` cada 3 s, `GET /modelos/{version}` | Panel en vivo; "Generar tráfico" provoca un 404 y se ve subir la tasa de error |

Las cifras de los ejemplos del Registro son **de demostración** y así se anotan
en las notas del modelo: no son resultados de ningún entrenamiento.

## Autorización

Las pantallas de administración son el primer uso real del módulo de
autorización (FASE 10). El login emite un token con los scopes
`servicios:leer` y `servicios:escribir`; cada pantalla comprueba el primero y
cada acción que escribe comprueba el segundo. Si el token caducó (15 min), la
pantalla lo dice y ofrece volver al login vaciando la pila de navegación.

## Red y tráfico sin cifrar

Los servicios corren sobre HTTP en la red local. `res/xml/network_security_config.xml`
permite tráfico sin cifrar de forma general, porque la dirección del equipo la
escribe quien administra y la política de red de Android no admite rangos.

**Coste asumido:** si alguna parte de la app hiciera HTTP a un servidor público,
el sistema ya no lo bloquearía. Hoy no existe tal petición.

**Trabajo futuro:** servir los microservicios tras TLS y restringir el dominio.

## Cómo probarlo

1. En el equipo: `python operar.py` (muestra la IP para un teléfono real).
2. En la app: **Inicio → Administración y microservicios**.
3. Conexión: `10.0.2.2` en el emulador, o la IP mostrada en un teléfono.

## Pruebas

| Archivo | Qué cubre |
|---|---|
| `LectorJsonTest` | Contrato con los servicios usando **respuestas reales capturadas** con curl, incluidas las tres formas de error de FastAPI |
| `MicroserviciosRepositoryImplTest` | Sin red, servicio apagado, tiempo agotado, 422, 404, respuesta ilegible, puerto correcto por servicio |
| `ConfiguracionServiciosTest` | Validación y limpieza de la dirección (acepta una URL pegada del navegador) |
| `ModelosViewModelTest` | Versión siguiente en orden semántico; coma decimal |
