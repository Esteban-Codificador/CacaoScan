import java.util.Properties

plugins {
    // AGP 9 aplica el soporte de Kotlin por si mismo: aplicar ademas
    // 'org.jetbrains.kotlin.android' provoca un error de configuracion.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * FASE RELEASE / DEPLOY - credenciales de firma.
 *
 * Nunca se escriben en el codigo fuente. Se leen de dos origenes posibles:
 *
 *  1. keystore.properties en la raiz, para firmar desde el equipo local.
 *     Ese archivo esta en .gitignore y NO se versiona.
 *  2. Variables de entorno, para el pipeline. GitHub Actions las inyecta
 *     desde los Secrets del repositorio.
 *
 * Si no hay ninguno de los dos, la build de release sigue funcionando y genera
 * un APK sin firmar. Asi cualquiera puede compilar el proyecto sin tener las
 * claves del equipo.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(propertyKey: String, environmentKey: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: System.getenv(environmentKey)

val signingStoreFile: String? = signingValue("storeFile", "KEYSTORE_FILE")
val signingStorePassword: String? = signingValue("storePassword", "KEYSTORE_PASSWORD")
val signingKeyAlias: String? = signingValue("keyAlias", "KEY_ALIAS")
val signingKeyPassword: String? = signingValue("keyPassword", "KEY_PASSWORD")

val canSignRelease: Boolean = listOf(
    signingStoreFile,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() } && rootProject.file(signingStoreFile!!).exists()

android {
    namespace = "com.cacaoscan"
    // Lo exigen las versiones actuales de AndroidX y Compose. Compilar contra una
    // API nueva no cambia el comportamiento en tiempo de ejecucion: eso lo decide
    // targetSdk, y la compatibilidad de instalacion la decide minSdk.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cacaoscan"
        // RNF07: Android 8.0 (API 26) o superior.
        // API 26 tambien habilita java.time de forma nativa, sin desugaring.
        minSdk = 26
        targetSdk = 36
        // FASE RELEASE: el pipeline inyecta la version derivada de la etiqueta de
        // git, de modo que la version del APK y la del repositorio no puedan
        // divergir. En compilaciones locales se usan los valores por defecto.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // RNF08: reduccion de tamano del APK.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Solo se firma si hay credenciales disponibles; si no, el APK sale
            // sin firmar y la compilacion no falla.
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Room: exporta el esquema para poder versionar migraciones y revisarlas en code review.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    // Persistencia local (RF06, RNF05).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Inyeccion de dependencias: permite sustituir el clasificador real por uno
    // de prueba sin tocar la UI ni los ViewModels.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Captura de imagen (RF01).
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Orientacion EXIF: sin esto, las fotos llegan rotadas al modelo.
    implementation(libs.androidx.exifinterface)

    // Inferencia en el dispositivo (RF03, RF05).
    //
    // Se usa TensorFlow Lite y no su sucesor LiteRT porque los artefactos
    // publicados de LiteRT (litert y litert-api) declaran el mismo espacio de
    // nombres, y AGP 9 rechaza esa duplicidad al fusionar el manifiesto. La API
    // de programacion es la misma (org.tensorflow.lite.Interpreter), de modo que
    // migrar a LiteRT cuando se corrija consistira en cambiar esta linea.
    implementation(libs.tensorflow.lite)

    // Carga de miniaturas del historial sin agotar memoria en gama media.
    implementation(libs.coil.compose)

    // Autenticacion local con huella o credencial del dispositivo (FASE 10).
    implementation(libs.androidx.biometric)
    // BiometricPrompt exige FragmentActivity; se declara explicitamente porque
    // MainActivity depende de ella de forma directa.
    implementation(libs.androidx.fragment.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core.ktx)
    // Implementacion JVM de org.json: en Android esta en el sistema, pero en las
    // pruebas unitarias el stub del SDK no funciona. Permite probar el modulo de
    // autorizacion sin emulador.
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
}
