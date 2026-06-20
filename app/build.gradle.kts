plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("androidx.room") version "2.7.0-alpha01" // Need room plugin for KMP
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")
                
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("androidx.datastore:datastore-preferences-core:1.1.1")
                
                // Room
                implementation("androidx.room:room-runtime:2.7.0-alpha01")
                implementation("androidx.sqlite:sqlite-bundled:2.5.0-alpha01")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.lifecycle:lifecycle-process:2.8.3")
                implementation("androidx.datastore:datastore-preferences:1.1.1")
                
                // LiteRT for Android
                implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")
            }
        }
    }
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.gymlogger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gymlogger"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        noCompress += "litertlm"
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspAndroid", "androidx.room:room-compiler:2.7.0-alpha01")
    add("kspIosX64", "androidx.room:room-compiler:2.7.0-alpha01")
    add("kspIosArm64", "androidx.room:room-compiler:2.7.0-alpha01")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.7.0-alpha01")
}

room {
    schemaDirectory("$projectDir/schemas")
}
