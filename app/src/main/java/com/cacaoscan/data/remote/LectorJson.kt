package com.cacaoscan.data.remote

import com.cacaoscan.domain.model.admin.ImagenCandidata
import com.cacaoscan.domain.model.admin.MetricasServicio
import com.cacaoscan.domain.model.admin.ModeloRegistrado
import com.cacaoscan.domain.model.admin.NuevoModelo
import com.cacaoscan.domain.model.admin.ResumenCalidad
import com.cacaoscan.domain.model.admin.ResumenRegistro
import com.cacaoscan.domain.model.admin.ValidacionImagen
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Traduccion entre el JSON de los microservicios y el modelo de dominio.
 *
 * Son funciones puras sobre cadenas: no hacen red ni dependen de Android, de
 * modo que se prueban con el JSON real que devuelven los servicios. Los nombres
 * de los campos son exactamente los de servicio.py en cada microservicio; si un
 * servicio los cambia, estas pruebas son las que lo detectan.
 *
 * Cualquier campo ausente o con tipo inesperado lanza [JSONException], que el
 * repositorio traduce a "respuesta ilegible" en lugar de mostrar datos a medias.
 */
object LectorJson {

    // ------------------------------------------------------------ monitoreo

    fun version(json: String): String = JSONObject(json).getString("version")

    fun saludViva(json: String): Boolean = JSONObject(json).optString("estado") == "vivo"

    fun metricas(json: String): MetricasServicio {
        val o = JSONObject(json)
        val latencia = o.getJSONObject("latencia_ms")
        return MetricasServicio(
            servicio = o.getString("servicio"),
            version = o.getString("version"),
            tiempoActivoSegundos = o.getDouble("tiempo_activo_segundos"),
            peticionesTotales = o.getInt("peticiones_totales"),
            erroresTotales = o.getInt("errores_totales"),
            tasaErrorPct = o.getDouble("tasa_error_pct"),
            latenciaMedianaMs = latencia.getDouble("mediana"),
            latenciaP95Ms = latencia.getDouble("p95"),
            latenciaMaximaMs = latencia.getDouble("maxima"),
            peticionesPorRuta = mapaEnteros(o.optJSONObject("peticiones_por_ruta")),
            peticionesPorEstado = mapaEnteros(o.optJSONObject("peticiones_por_estado")),
        )
    }

    // ---------------------------------------------------- registro de modelos

    fun modelo(json: String): ModeloRegistrado = modelo(JSONObject(json))

    fun modelos(json: String): List<ModeloRegistrado> {
        val arreglo = JSONArray(json)
        return List(arreglo.length()) { i -> modelo(arreglo.getJSONObject(i)) }
    }

    fun resumenRegistro(json: String): ResumenRegistro {
        val o = JSONObject(json)
        return ResumenRegistro(
            totalRegistrados = o.getInt("total_registrados"),
            aptosParaPublicar = o.getInt("aptos_para_publicar"),
            noAptos = o.getInt("no_aptos"),
            mejorExactitud = decimalONulo(o, "mejor_exactitud"),
            versionVigente = textoONulo(o, "version_vigente"),
        )
    }

    fun nuevoModelo(modelo: NuevoModelo): String = JSONObject()
        .put("version", modelo.version)
        .put("exactitud", modelo.exactitud)
        .put("sensibilidad", modelo.sensibilidad)
        .put("hash_sha256", modelo.hashSha256)
        .put("tamano_mb", modelo.tamanoMb)
        .put("arquitectura", modelo.arquitectura)
        .put("notas", modelo.notas)
        .toString()

    private fun modelo(o: JSONObject) = ModeloRegistrado(
        version = o.getString("version"),
        exactitud = o.getDouble("exactitud"),
        sensibilidad = o.getDouble("sensibilidad"),
        hashSha256 = o.getString("hash_sha256"),
        tamanoMb = o.getDouble("tamano_mb"),
        arquitectura = o.optString("arquitectura", "no indicada"),
        notas = o.optString("notas", ""),
        aptoParaPublicar = o.getBoolean("apto_para_publicar"),
        motivo = o.optString("motivo", ""),
        registradoEn = o.getString("registrado_en"),
    )

    // ------------------------------------------------------ control de calidad

    fun validacion(json: String): ValidacionImagen = validacion(JSONObject(json))

    fun validaciones(json: String): List<ValidacionImagen> {
        val arreglo = JSONArray(json)
        return List(arreglo.length()) { i -> validacion(arreglo.getJSONObject(i)) }
    }

    fun resumenCalidad(json: String): ResumenCalidad {
        val o = JSONObject(json)
        return ResumenCalidad(
            totalValidadas = o.getInt("total_validadas"),
            aceptadas = o.getInt("aceptadas"),
            rechazadas = o.getInt("rechazadas"),
            tasaAceptacionPct = o.getDouble("tasa_aceptacion_pct"),
            conAdvertencias = o.getInt("con_advertencias"),
            motivosDeRechazo = mapaEnteros(o.optJSONObject("motivos_de_rechazo")),
            aceptadasPorClase = mapaEnteros(o.optJSONObject("aceptadas_por_clase")),
        )
    }

    fun imagenCandidata(imagen: ImagenCandidata): String = JSONObject()
        .put("archivo", imagen.archivo)
        .put("ancho", imagen.ancho)
        .put("alto", imagen.alto)
        .put("peso_kb", imagen.pesoKb)
        .put("clases", JSONArray(imagen.clases))
        .toString()

    private fun validacion(o: JSONObject) = ValidacionImagen(
        archivo = o.getString("archivo"),
        ancho = o.optInt("ancho"),
        alto = o.optInt("alto"),
        pesoKb = o.optDouble("peso_kb", 0.0),
        clases = textos(o.optJSONArray("clases")),
        claseBinaria = textoONulo(o, "clase_binaria"),
        aceptada = o.getBoolean("aceptada"),
        errores = textos(o.optJSONArray("errores")),
        advertencias = textos(o.optJSONArray("advertencias")),
        validadaEn = o.getString("validada_en"),
    )

    // ----------------------------------------------------------------- errores

    /**
     * Extrae mensajes legibles del cuerpo de un error de FastAPI.
     *
     * FastAPI usa tres formas para "detail", y las tres aparecen aqui:
     *  - Rechazo de negocio del Registro:  {"detail": {"mensaje": ..., "errores": [...]}}
     *  - Error de esquema de Pydantic:     {"detail": [{"loc": [...], "msg": ...}, ...]}
     *  - Error simple (404):               {"detail": "texto"}
     */
    fun mensajesDeError(json: String): List<String> = try {
        when (val detalle = JSONObject(json).opt("detail")) {
            is JSONObject -> {
                val errores = textos(detalle.optJSONArray("errores"))
                errores.ifEmpty { listOfNotNull(textoONulo(detalle, "mensaje")) }
            }

            is JSONArray -> List(detalle.length()) { i ->
                val e = detalle.getJSONObject(i)
                // El primer elemento de "loc" es "body"; el ultimo es el nombre del campo.
                val loc = e.optJSONArray("loc")
                val campo = if (loc != null && loc.length() > 0) loc.get(loc.length() - 1).toString() else null
                if (campo != null) "$campo: ${e.optString("msg")}" else e.optString("msg")
            }

            is String -> listOf(detalle)
            else -> emptyList()
        }
    } catch (_: JSONException) {
        emptyList()
    }

    // ---------------------------------------------------------------- utilidades

    private fun textoONulo(o: JSONObject, clave: String): String? =
        if (!o.has(clave) || o.isNull(clave)) null else o.getString(clave)

    private fun decimalONulo(o: JSONObject, clave: String): Double? =
        if (!o.has(clave) || o.isNull(clave)) null else o.getDouble(clave)

    private fun textos(arreglo: JSONArray?): List<String> =
        if (arreglo == null) emptyList() else List(arreglo.length()) { i -> arreglo.getString(i) }

    private fun mapaEnteros(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        val mapa = LinkedHashMap<String, Int>()
        o.keys().forEach { clave -> mapa[clave] = o.getInt(clave) }
        return mapa
    }
}
