# Arquitectura de CacaoScan

## 1. Por qué MVVM

MVVM es el patrón elegido. No por costumbre: por cuatro razones concretas y defendibles.

**1. Es el patrón que Android recomienda oficialmente.** La *Guide to App Architecture* de Android
describe exactamente esta separación: capa de interfaz (Compose + ViewModel), capa de dominio
(casos de uso) y capa de datos (repositorios). Defender MVVM en Android es defender el estándar de
la plataforma, no una preferencia personal.

**2. El problema real de CacaoScan es la supervivencia del estado.** Una clasificación tarda
segundos. Si el productor gira el teléfono mientras se analiza la vaina, Android destruye y recrea
la Activity. Con MVVM el `ScanViewModel` sobrevive a esa recreación y la clasificación en curso no
se pierde ni se reinicia. Con MVC o MVP habría que resolver eso a mano, guardando y restaurando
estado manualmente.

**3. Aísla la inteligencia artificial de la interfaz.** Ningún `@Composable` sabe que existe
TensorFlow. La pantalla observa un `StateFlow<ScanUiState>` y reacciona. Esa frontera es la que
permite sustituir el clasificador de demostración por el modelo real sin tocar una sola pantalla.

**4. Hace el código comprobable.** Los ViewModels y el repositorio se prueban en la JVM, sin
emulador. De las 36 pruebas unitarias del proyecto, ninguna necesita un dispositivo.

### Por qué no Clean Architecture completa

Se consideró y se descartó. Una Clean Architecture estricta exigiría modelos e interfaces
duplicados en cada frontera, con *mappers* entre todos ellos. Para una aplicación de seis pantallas
y una entidad de dominio, ese coste no compra nada: añade archivos y ceremonia sin reducir el
acoplamiento real. Lo que sí se conserva de esa escuela es lo que aporta valor aquí:

- una capa de dominio con los modelos y las **interfaces** de repositorio;
- casos de uso donde encapsulan una regla real (no como envoltorios vacíos);
- dependencia siempre hacia dentro: `presentation → domain ← data`.

## 2. Capas

```
┌──────────────────────────────────────────────────────────────┐
│ presentation/                                                │
│   Composables (Compose + Material 3)                         │
│   ViewModels  (StateFlow<UiState>)                           │
└───────────────┬──────────────────────────────────────────────┘
                │ solo eventos y estado
┌───────────────▼──────────────────────────────────────────────┐
│ domain/                                                      │
│   model/       PodCondition, Confidence, ClassificationOutput │
│   repository/  interfaces (ScanRepository)                   │
│   usecase/     ClassifyPodImageUseCase, SaveScanUseCase, ...  │
└───────┬──────────────────────────────────┬───────────────────┘
        │                                  │
┌───────▼──────────────┐        ┌──────────▼───────────────────┐
│ data/                │        │ ml/                          │
│   local/db  (Room)   │        │   PodClassifier (interfaz)   │
│   local/file         │        │   preprocessing/             │
│   repository/        │        │   interpretation/            │
│   mapper/            │        │   mock/  ·  tflite/          │
└──────────────────────┘        └──────────────────────────────┘

security/   módulo independiente: Keystore, bloqueo local, autorización JWT
navigation/ grafo de navegación
di/         módulos de Hilt
```

Regla que se cumple sin excepción en el proyecto:

- Ningún `@Composable` accede a Room.
- Ningún `@Composable` conoce TensorFlow.
- Ningún ViewModel construye SQL ni abre archivos.
- La lógica de inferencia no importa nada de `androidx.compose`.

## 3. Flujo de datos (unidireccional)

```
  Usuario toca "Analizar"
        │
        ▼
  ScanScreen  ──── evento ────►  ScanViewModel
                                      │
                                      ▼
                            ClassifyPodImageUseCase
                                      │  (Dispatchers.Default)
                                      ▼
                                PodClassifier
                                 ├── MockPodClassifier   (sin modelo)
                                 └── TfLitePodClassifier (con modelo)
                                      │
                                      ▼
                            ClassificationOutput
                                      │
  ScanScreen ◄──── StateFlow<ScanUiState> ◄──┘
```

El estado fluye en una sola dirección: la pantalla emite eventos hacia arriba y recibe estado hacia
abajo. Nunca modifica el estado directamente.

## 4. Repository Pattern

`ScanRepository` es una **interfaz** en `domain/`, implementada por `ScanRepositoryImpl` en `data/`.
Coordina dos fuentes que deben mantenerse coherentes:

| Fuente | Contenido |
|---|---|
| Room (`scans`) | fecha, categoría, confianza, tiempo de inferencia, origen, versión del modelo, nombre del archivo de imagen |
| Almacenamiento interno (`filesDir/scan_images`) | la fotografía reescalada |

Reglas de coherencia implementadas y probadas:

- Si la imagen no se puede guardar, **la revisión se guarda igualmente sin imagen**. Perder la foto
  es molesto; perder el resultado lo es más.
- Si la fila no se puede insertar, **se borra la imagen** para no dejar archivos huérfanos.
- Al borrar una revisión, **se borra también su imagen**.

## 5. Estados de interfaz

`UiState<T>` obliga a tratar cuatro casos: `Loading`, `Success`, `Empty`, `Error`. `Empty` existe
separado de `Error` a propósito: un historial vacío no es un fallo, y presentarlo como tal
confundiría al productor. Ninguna pantalla del proyecto queda sin manejo de estados.

## 6. Inyección de dependencias

Hilt, con módulos en `di/`. La razón técnica concreta: el mismo `PodClassifier` se inyecta en los
ViewModels y debe poder sustituirse por un doble en las pruebas sin tocar las pantallas. Hacerlo a
mano con `ViewModelProvider.Factory` en seis pantallas generaría bastante código repetido.

Los `CoroutineDispatcher` también se inyectan (`@IoDispatcher`, `@DefaultDispatcher`) en lugar de
referenciar `Dispatchers.IO` directamente: es lo que permite que las pruebas sean deterministas.
