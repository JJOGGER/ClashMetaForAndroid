import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))

    implementation(libs.kotlin.coroutine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    implementation(libs.quickie.bundled)
    implementation(libs.androidx.activity.ktx)

    implementation("com.tencent:mmkv:2.2.4")
    // Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.json)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Image loading
    implementation(libs.glide.core)
    kapt(libs.glide.compiler)

    // Material Design 3
    implementation(libs.material3)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation("org.greenrobot:eventbus:3.3.1")
    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // UI
    implementation(libs.lottie)
    implementation(libs.androidx.swiperefreshlayout)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.material) // Material 2 for PullRefresh API
    implementation(libs.coil.compose)
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")
}

android {
    buildFeatures {
        compose = true
    }
    namespace = "com.xboard"
}

tasks.getByName("clean", type = Delete::class) {
    delete(file("release"))
}

val geoFilesDownloadDir = "src/main/assets"

tasks.register("downloadGeoFiles") {
    val geoFilesUrls = mapOf(
        "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb" to "geoip.metadb",
        "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat" to "geosite.dat",
        // "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/country.mmdb" to "country.mmdb",
        "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb" to "ASN.mmdb",
    )

    // 只有在本地文件不存在时才下载，避免每次构建都重复下载
    doLast {
        geoFilesUrls.forEach { (downloadUrl, outputFileName) ->
            val outputPath = file("$geoFilesDownloadDir/$outputFileName")
            if (outputPath.exists()) {
                println("$outputFileName already exists, skip downloading")
            } else {
                val url = URL(downloadUrl)
                outputPath.parentFile.mkdirs()
                url.openStream().use { input ->
                    Files.copy(input, outputPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    println("$outputFileName downloaded to $outputPath")
                }
            }
        }
    }
}

afterEvaluate {
    // 默认不再强制在 assemble 任务前下载 GeoFiles，避免每次编译都触发网络下载
    // 如需在构建前自动下载，可使用:
    //   ./gradlew -PwithGeoFiles assembleDebug
    if (project.hasProperty("withGeoFiles")) {
        val downloadGeoFilesTask = tasks.named("downloadGeoFiles")
        tasks.matching { it.name.startsWith("assemble") }.configureEach {
            dependsOn(downloadGeoFilesTask)
        }
    }
}