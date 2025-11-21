plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.devst.verservidores"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devst.verservidores"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Dependencias necesarias para la conexión de los servidores a través de okhttp3 y gson
    // Conexión HTTP y JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    // Glide
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    // ** FIREBASE: CORRECCIÓN E IMPLEMENTACIÓN **
    // 1. Usar la BOM (Lista de Materiales) para gestionar las versiones
    implementation(platform("com.google.firebase:firebase-bom:32.7.0")) // Usamos la versión más reciente (32.7.0 en lugar de 32.2.2)

    // 2. Dependencias de Firebase sin especificar versión (la toma del BOM)
    implementation("com.google.firebase:firebase-firestore")     // Para Usuarios
    implementation("com.google.firebase:firebase-database")      // Para Comentarios (Realtime DB)


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}