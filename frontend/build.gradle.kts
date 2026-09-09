import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "frontend"
        browser {
            commonWebpackConfig {
                outputFileName = "frontend.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                    proxy = mutableListOf(
                        KotlinWebpackConfig.DevServer.Proxy(
                            context = mutableListOf("/api"),
                            target = "http://localhost:8080",
                        )
                    )
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("io.ktor:ktor-client-core:3.1.3")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.3.0-beta02")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.3.0-beta02")
                implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.3.0-beta02")
                implementation("io.ktor:ktor-client-js:3.1.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3.1")
            }
        }
    }
}
