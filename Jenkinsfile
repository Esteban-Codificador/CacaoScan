// ===========================================================================
// CacaoScan - Pipeline de Jenkins
//
// ETAPAS DEVOPS: BUILD y TEST
//
//   Build   Compila el APK de Android con Gradle y verifica que el codigo de
//           los dos microservicios compila. El APK queda archivado en Jenkins
//           y se puede descargar desde la pagina de cada ejecucion.
//   Test    Ejecuta las pruebas JUnit de la aplicacion y las de pytest de los
//           microservicios. Jenkins publica ambos reportes y dibuja la
//           tendencia de pruebas entre ejecuciones.
//
// Jenkins corre en este mismo equipo (http://localhost:8090) y construye la
// copia de trabajo directamente, sin necesidad de que el codigo este subido.
// Cuando lo este, el job puede pasar a "Pipeline script from SCM" apuntando al
// repositorio de GitHub sin cambiar nada de este archivo salvo las rutas.
// ===========================================================================

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    environment {
        PROYECTO        = 'C:/Users/Esteban Avila/Documents/AndroidStudioProjects/CacaoScan'
        MICROSERVICIOS  = 'C:/Users/Esteban Avila/Documents/CacaoScan-Microservicios'
        JAVA_HOME       = 'C:\\Program Files\\Android\\Android Studio\\jbr'
        // En este equipo la ruta temporal por omision es la forma corta 8.3
        // (C:\Users\ESTEBA~1\...), con la que la JVM no logra abrir su conexion
        // de bucle local. Se fuerza una ruta temporal sin espacios.
        TMP             = 'C:\\gtmp'
        TEMP            = 'C:\\gtmp'
        GRADLE_OPTS     = '-Djava.io.tmpdir=C:\\gtmp'
    }

    // En Jenkins para Windows el interprete de comandos NO busca ejecutables en
    // la carpeta actual, por seguridad: por eso se escribe .\gradlew.bat y no
    // gradlew.bat a secas.
    stages {

        stage('Preparar') {
            steps {
                bat '"%JAVA_HOME%\\bin\\java.exe" -version'
                bat 'python --version'
            }
        }

        stage('Build · APK Android') {
            steps {
                dir(env.PROYECTO) {
                    bat '.\\gradlew.bat :app:assembleDebug --console=plain'
                    archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', fingerprint: true
                }
            }
        }

        stage('Build · Microservicios') {
            steps {
                dir(env.MICROSERVICIOS) {
                    // Sin Docker: el "build" de un servicio Python es comprobar que
                    // todo el codigo compila a bytecode y que las dependencias estan.
                    bat 'python -m compileall -q comun servicio_modelos servicio_calidad'
                    bat 'python -c "import fastapi, uvicorn, pydantic; print(\'Dependencias OK\')"'
                }
            }
        }

        stage('Test · JUnit (aplicacion)') {
            steps {
                dir(env.PROYECTO) {
                    bat '.\\gradlew.bat :app:testDebugUnitTest --console=plain'
                }
            }
            post {
                always {
                    dir(env.PROYECTO) {
                        junit testResults: 'app/build/test-results/testDebugUnitTest/*.xml',
                              allowEmptyResults: false
                    }
                }
            }
        }

        stage('Test · pytest (microservicios)') {
            steps {
                dir(env.MICROSERVICIOS) {
                    // pytest escribe el reporte en formato JUnit XML, el mismo que
                    // entiende Jenkins: las dos baterias se ven en un solo reporte.
                    bat 'python -m pytest tests -q --junitxml=reportes/pytest-junit.xml'
                }
            }
            post {
                always {
                    dir(env.MICROSERVICIOS) {
                        junit testResults: 'reportes/pytest-junit.xml', allowEmptyResults: false
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'BUILD y TEST correctos. El APK esta en "Artefactos" de esta ejecucion.'
        }
        failure {
            echo 'El pipeline fallo. Revise la etapa en rojo y su registro.'
        }
    }
}
