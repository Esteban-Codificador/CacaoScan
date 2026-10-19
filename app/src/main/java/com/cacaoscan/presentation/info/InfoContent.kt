package com.cacaoscan.presentation.info

/**
 * Contenido informativo de la aplicacion (RF07, RF08).
 *
 * Esta escrito en el codigo y no se descarga de ningun servicio: debe estar
 * disponible sin conexion (RNF02).
 *
 * LIMITE DE ALCANCE: este contenido describe la enfermedad y explica el uso de
 * la herramienta. NO incluye recomendaciones de manejo agronomico, tratamientos
 * ni dosis, porque el IEEE830 las excluye explicitamente del alcance
 * ("Won't have"). Cualquier decision de manejo del cultivo corresponde al
 * productor y a su asistencia tecnica.
 */
data class InfoSection(
    val title: String,
    val paragraphs: List<String>,
)

object InfoContent {

    val aboutMonilia: List<InfoSection> = listOf(
        InfoSection(
            title = "Que es la monilia",
            paragraphs = listOf(
                "La monilia es una enfermedad causada por el hongo Moniliophthora roreri, " +
                    "que ataca directamente la vaina del cacao. Es una de las causas mas " +
                    "importantes de perdida de cosecha en las zonas productoras del pais.",
                "Segun la informacion recogida en el documento de requisitos de este " +
                    "proyecto (Fedecacao, 2023), las perdidas pueden ir de un 30% a mas de " +
                    "un 80% de la produccion cuando la enfermedad no se detecta y se " +
                    "controla a tiempo.",
            ),
        ),
        InfoSection(
            title = "Por que importa detectarla temprano",
            paragraphs = listOf(
                "El hongo avanza dentro de la vaina antes de que los sintomas sean " +
                    "evidentes por fuera. Cuando el dano ya se ve con claridad, la " +
                    "enfermedad suele haberse extendido a otras vainas del lote.",
                "Reconocer la monilia en sus etapas iniciales exige experiencia tecnica " +
                    "que no siempre esta disponible en zonas rurales. Esa es la necesidad " +
                    "que CacaoScan busca apoyar.",
            ),
        ),
        InfoSection(
            title = "Senales que suelen asociarse a la monilia",
            paragraphs = listOf(
                "Deformaciones o abultamientos en la superficie de la vaina.",
                "Manchas de color pardo o chocolate que crecen y se unen entre si.",
                "Una capa blanca o cremosa de aspecto polvoriento sobre la mancha, en " +
                    "etapas mas avanzadas.",
                "Vainas que se sienten mas pesadas de lo normal o suenan macizas al " +
                    "golpearlas suavemente.",
                "Estas senales son orientativas. La aplicacion analiza la fotografia " +
                    "completa de la vaina y no senala en que parte esta la lesion.",
            ),
        ),
        InfoSection(
            title = "Que NO hace esta aplicacion",
            paragraphs = listOf(
                "No diagnostica otras enfermedades del cacao distintas de la monilia.",
                "No recomienda tratamientos, productos ni dosis.",
                "No indica el lugar exacto de la lesion sobre la vaina.",
                "No sustituye la revision directa del cultivo ni el criterio de un tecnico.",
            ),
        ),
    )

    val howToUse: List<InfoSection> = listOf(
        InfoSection(
            title = "Como revisar una vaina",
            paragraphs = listOf(
                "1. En la pantalla de inicio, toque \"Escanear vaina\".",
                "2. Apunte la camara a una sola vaina y tome la foto, o elija una " +
                    "fotografia que ya tenga en la galeria.",
                "3. Revise que la vaina se vea completa y enfocada, y toque \"Analizar\".",
                "4. Lea el resultado y, si quiere conservarlo, toque " +
                    "\"Guardar en el historial\".",
            ),
        ),
        InfoSection(
            title = "Como tomar una buena fotografia",
            paragraphs = listOf(
                "Fotografie una sola vaina por vez.",
                "Busque luz natural y evite sombras muy marcadas o contraluz.",
                "Acerquese lo suficiente para que la vaina ocupe casi toda la imagen.",
                "Mantenga el telefono firme hasta que la imagen se vea nitida.",
                "Evite fotografiar con el suelo o el follaje ocupando la mayor parte " +
                    "de la escena.",
            ),
        ),
        InfoSection(
            title = "Como leer el nivel de confianza",
            paragraphs = listOf(
                "El porcentaje indica que tan seguro esta el modelo de la categoria que " +
                    "eligio. No es la probabilidad de que el diagnostico sea correcto.",
                "Una confianza alta no garantiza acierto, y una confianza baja no " +
                    "significa que la vaina este sana. Ante cualquier duda, revise la " +
                    "vaina directamente.",
            ),
        ),
        InfoSection(
            title = "Sobre sus datos",
            paragraphs = listOf(
                "El analisis se hace dentro del telefono. La aplicacion no envia sus " +
                    "fotografias ni su historial a ningun servidor.",
                "De hecho, CacaoScan no pide permiso de acceso a internet, por lo que el " +
                    "sistema operativo le impide conectarse a la red.",
                "Las fotografias y el historial se guardan solo en este dispositivo y se " +
                    "borran si desinstala la aplicacion.",
            ),
        ),
        InfoSection(
            title = "Funciona sin senal",
            paragraphs = listOf(
                "Puede tomar fotos, analizar vainas y consultar el historial sin " +
                    "conexion a internet y sin datos moviles.",
            ),
        ),
    )
}
