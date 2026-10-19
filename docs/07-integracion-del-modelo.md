# Integración del modelo entrenado

Este documento explica cómo pasar del clasificador de demostración al modelo real.

---

## 1. Estado actual: la aplicación NO tiene un modelo entrenado

Debe quedar explícito, porque afecta a cómo se lee cualquier resultado que muestre la aplicación hoy:

- El modelo de clasificación **todavía no ha sido entrenado ni evaluado**.
- Mientras no exista el archivo `.tflite`, la aplicación arranca en **modo demostración**.
- `MockPodClassifier` **no analiza la imagen**. Calcula una huella numérica a partir de unos pocos
  píxeles y deriva de ella una categoría. El resultado es **determinista** (la misma foto da siempre
  lo mismo, lo que hace las pruebas reproducibles) pero **arbitrario** respecto al estado real de la
  vaina.
- Todo resultado simulado se marca con `ClassificationOrigin.SIMULATION`, lo que obliga a la interfaz
  a mostrar un aviso no descartable y deja el registro del historial marcado como simulado de forma
  permanente.

**No existe ninguna medición de exactitud.** El IEEE830 fija en RNF04 una exactitud objetivo del
≥85 % sobre el conjunto de prueba; eso es un **criterio de evaluación del modelo**, y no podrá
afirmarse hasta que exista un conjunto de prueba real y una evaluación documentada.

---

## 2. Cómo se elige el clasificador

Todo ocurre en un único punto, `ml/ClassifierProvider.kt`:

```
¿Existe assets/cacao_pod_classifier.tflite?
        │
        ├── No  ──►  MockPodClassifier      (modo demostración, aviso en pantalla)
        │
        └── Sí  ──►  TfLitePodClassifier    (modelo real)
                        │
                        └── ¿falla al cargar?  ──►  MockPodClassifier
```

Si el archivo existe pero está corrupto o tiene un formato no soportado, se vuelve al modo
demostración en lugar de dejar la aplicación inutilizable. El usuario verá con claridad que los
resultados son simulados.

---

## 3. Pasos para integrar el modelo real

### Paso 1 — Colocar el archivo

Copiar el modelo entrenado en:

```
app/src/main/assets/cacao_pod_classifier.tflite
```

Si se prefiere otro nombre, cambiarlo en `ModelSpec.DEFAULT_ASSET_FILE_NAME`.

### Paso 2 — Confirmar los parámetros en `ModelSpec.kt`

**Los valores actuales son una propuesta razonable, no un hecho.** Deben confirmarse contra el
modelo entrenado antes de dar por buena ninguna clasificación:

| Parámetro | Valor propuesto | Qué hay que confirmar |
|---|---|---|
| `inputWidth` / `inputHeight` | 224 × 224 | Resolución de entrada usada al entrenar |
| `normalization` | `ZERO_TO_ONE` | ¿`pixel/255`, `(pixel/127.5)-1`, o entero sin escalar? |
| `outputFormat` | `SOFTMAX_TWO_CLASS` | ¿Dos salidas softmax o una salida sigmoide? |
| `labelOrder` | `[HEALTHY, MONILIA_AFFECTED]` | **Orden exacto de las clases en la capa de salida** |
| `modelVersion` | `"sin-modelo"` | Identificador del modelo, para trazabilidad |

> **El error más peligroso es `labelOrder`.** Si el orden está invertido, la aplicación no falla ni
> avisa: simplemente dice «sana» cuando debería decir «monilia». Confírmelo antes que ningún otro
> parámetro.

### Paso 3 — Obtener esos valores del archivo

Con Python, sobre el `.tflite` ya exportado:

```python
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="cacao_pod_classifier.tflite")
interpreter.allocate_tensors()

print("ENTRADA :", interpreter.get_input_details())
print("SALIDA  :", interpreter.get_output_details())
```

- `shape` de la entrada da `inputWidth` e `inputHeight` (formato `[1, alto, ancho, 3]`).
- `dtype` `float32` → `ZERO_TO_ONE` o `MINUS_ONE_TO_ONE` según el preprocesamiento del
  entrenamiento; `uint8` → `UINT8_RAW`.
- `shape` de la salida `[1, 2]` → `SOFTMAX_TWO_CLASS`; `[1, 1]` → `SIGMOID_SINGLE`.
- El orden de las etiquetas es el que se usó al construir el generador de datos durante el
  entrenamiento (con `image_dataset_from_directory`, es el orden **alfabético** de las carpetas).

### Paso 4 — Verificar

1. Instalar y comprobar que **desaparece el aviso de modo demostración**.
2. Comprobar en la pantalla de resultado que el «Tiempo de análisis» está **por debajo de 5000 ms**
   en un dispositivo de gama media (RNF01).
3. Fotografiar vainas de estado conocido y comprobar que las categorías no salen invertidas.

---

## 4. El runtime de inferencia

Se usa `org.tensorflow:tensorflow-lite:2.17.0`.

**Por qué no LiteRT**, que es su sucesor oficial: los artefactos publicados de LiteRT
(`com.google.ai.edge.litert:litert` y `litert-api`) declaran **el mismo espacio de nombres**, y
AGP 9 rechaza esa duplicidad al fusionar el manifiesto. Se verificó con las versiones 2.1.6 y 2.2.0
y el fallo es idéntico:

```
Namespace 'com.google.ai.edge.litert' is used in multiple modules and/or libraries
```

La API de programación es la misma (`org.tensorflow.lite.Interpreter`), de modo que migrar a LiteRT
cuando se corrija ese empaquetado consistirá en cambiar una línea de `app/build.gradle.kts`.

### Detalles de la implementación

- **Carga por *memory mapping*:** el archivo se mapea desde el APK y el sistema pagina solo lo
  necesario, en lugar de leer el modelo entero a memoria (importante en gama media, RNF07).
- **Un solo intérprete, reutilizado:** crearlo en cada clasificación consumiría buena parte del
  presupuesto de 5 s de RNF01. Como no es seguro para uso concurrente, las llamadas se serializan
  con un `Mutex`.
- **Tipo de salida consultado al modelo:** si la capa de salida es `UINT8`, se descuantiza con la
  escala y el punto cero que declara el propio archivo, en lugar de asumir un formato.

---

## 5. Lo que NO debe hacerse dentro de Android Studio

El entrenamiento del modelo es un proceso **independiente** (notebook, Python, TensorFlow/Keras).
La aplicación solo consume el resultado. No se debe:

- entrenar ni reentrenar dentro de la app;
- descargar el modelo desde internet (rompería RNF02 y exigiría el permiso de red);
- ajustar umbrales «a ojo» para que los resultados parezcan mejores.

---

## 6. Evaluación del modelo (fuera de la app)

RNF04 exige evaluar con exactitud, precisión, sensibilidad y matriz de confusión. Eso se hace sobre
el conjunto de prueba, en el entorno de entrenamiento, y se documenta aparte. La aplicación no puede
—ni debe— afirmar nada sobre su propia exactitud.

Lo que la aplicación **sí** aporta como evidencia es el **tiempo real de inferencia**, que mide y
guarda en cada revisión del historial. Esa es la evidencia de RNF01.
