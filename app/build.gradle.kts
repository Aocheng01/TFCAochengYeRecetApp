plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize") // Necesario para el plugin de Android Esta es la forma eficiente en Android de pasar objetos complejos entre componentes.
}

android {
    namespace = "com.example.recetapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.recetapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Lee las claves desde gradle.properties y las añade a BuildConfig
        buildConfigField("String", "EDAMAM_APP_ID", "\"${project.property("EDAMAM_APP_ID")}\"")
        buildConfigField("String", "EDAMAM_APP_KEY", "\"${project.property("EDAMAM_APP_KEY")}\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }



    buildFeatures {
        viewBinding = true // Esta línea activa View Binding
        buildConfig = true // Esta línea activa la generación de BuildConfig
    }

}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.runtime.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Dependencias para Retrofit y Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Para convertir JSON a objetos Kotlin

    // Añade Coil para cargar imágenes
    implementation("io.coil-kt:coil:2.6.0")


    // Importa Firebase Bill of Materials (BoM) - gestiona versiones
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    // Dependencia de Firebase Authentication (KTX para extensiones Kotlin)
    implementation("com.google.firebase:firebase-auth-ktx")

    // Dependencia para Google Sign-In (Necesaria para el login con Google)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

}