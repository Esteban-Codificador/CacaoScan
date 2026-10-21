package com.cacaoscan.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.EmptyState

/** Tarjeta con titulo para agrupar una seccion de una pantalla de administracion. */
@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    contenido: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            contenido()
        }
    }
}

/** Fila "etiqueta ........ valor". */
@Composable
fun FilaDato(etiqueta: String, valor: String, monoespaciado: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monoespaciado) FontFamily.Monospace else null,
        )
    }
}

/** Etiqueta de color corta: VIVO, APTO, RECHAZADA... */
@Composable
fun Insignia(texto: String, fondo: Color, contenido: Color) {
    Surface(color = fondo, contentColor = contenido, shape = RoundedCornerShape(50)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun InsigniaPositiva(texto: String) = Insignia(
    texto = texto,
    fondo = MaterialTheme.colorScheme.primaryContainer,
    contenido = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
fun InsigniaNegativa(texto: String) = Insignia(
    texto = texto,
    fondo = MaterialTheme.colorScheme.errorContainer,
    contenido = MaterialTheme.colorScheme.onErrorContainer,
)

/** Lista de mensajes del validador, uno por linea con vineta. */
@Composable
fun ListaMensajes(mensajes: List<String>, color: Color = MaterialTheme.colorScheme.error) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        mensajes.forEach { mensaje ->
            Text(text = "• $mensaje", style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

/** Lo que se muestra en lugar de la pantalla cuando la sesion no autoriza. */
@Composable
fun AccesoDenegado(motivo: String, onIniciarSesion: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        EmptyState(message = motivo)
        PrimaryAction(text = "Iniciar sesion", icon = Icons.Default.Lock, onClick = onIniciarSesion)
    }
}

fun porcentaje(valor: Double): String = "%.1f %%".format(valor * 100)
