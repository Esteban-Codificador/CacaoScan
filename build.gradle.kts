// Configuración raíz. Los plugins se declaran aquí sin aplicarse y se activan
// en el módulo :app, según la convención actual de Gradle con version catalogs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
