import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.play.publisher)
}

val mapsApiKey: String = (findProperty("MAPS_API_KEY") as String?) ?: run {
    val localProps = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(localProps::load)
    }
    localProps.getProperty("MAPS_API_KEY", "")
}

val versionProps = Properties()
val versionPropsFile = rootProject.file("gradle/version.properties")
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use(versionProps::load)
}

fun envVersion(name: String, fallback: Int, upperBound: Int = 9_999): Int {
    val value = System.getenv(name)?.toIntOrNull() ?: fallback
    return value.coerceAtLeast(0).coerceAtMost(upperBound)
}

val versionMajor = versionProps.getProperty("major", "1").toInt()
val versionMinor = envVersion("CI_VERSION_MINOR", versionProps.getProperty("minor", "0").toInt(), 9_999)
val versionPatch = envVersion("CI_VERSION_PATCH", versionProps.getProperty("patch", "0").toInt(), 9_999)

val computedVersionCode = versionMajor * 1_000_000 + versionMinor * 10_000 + versionPatch
val computedVersionName = listOf(versionMajor, versionMinor, versionPatch).joinToString(separator = ".")

val keystoreFile = rootProject.file("gradle/keystore.jks")
val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
val keyPassword = System.getenv("ANDROID_KEY_ALIAS_PASSWORD")
val isReleaseKeystoreConfigured = keystoreFile.exists() && !keystorePassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()

android {
    namespace = "com.mapshoppinglist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mapshoppinglist"
        minSdk = 29
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        resValue("string", "google_maps_key", mapsApiKey)
    }

    signingConfigs {
        if (isReleaseKeystoreConfigured) {
            create("release") {
                val resolvedStorePassword = keystorePassword!!
                val resolvedKeyAlias = keyAlias!!
                val resolvedKeyPassword = keyPassword!!
                storeFile = keystoreFile
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        managedDevices {
            localDevices {
                create("pixel2api31") {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 2"
                    // Use only API levels 27 and higher.
                    apiLevel = 31
                    // To include Google services, use "google".
                    systemImageSource = "google-atd"
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
    arg("room.skipVerification", "true")
}

aboutLibraries {
    android.registerAndroidTasks = true
}

val playCredentialsPath = System.getenv("PLAY_SERVICE_ACCOUNT_JSON_PATH")?.let(::file)
val defaultPlayCredentialsFile = rootProject.file("gradle/play-service-account.json")
val playCredentialsFile = when {
    playCredentialsPath != null && playCredentialsPath.exists() -> playCredentialsPath
    defaultPlayCredentialsFile.exists() -> defaultPlayCredentialsFile
    else -> null
}

play {
    enabled.set(playCredentialsFile != null)
    defaultToAppBundles.set(true)
    track.set("internal")
    releaseStatus.set(ReleaseStatus.DRAFT)
    playCredentialsFile?.let { credentials ->
        serviceAccountCredentials.set(credentials)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.places)
    implementation(libs.maps.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.sqlite.jdbc)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
