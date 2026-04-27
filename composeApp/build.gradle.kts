import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        desktopTest.dependencies {
            implementation(kotlin("test"))
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.arrow.core)
            implementation(project(":core"))
            implementation(project(":siq"))
            implementation(project(":server"))

            val javafxVersion = "21.0.5"
            listOf("win", "linux", "mac").forEach { platform ->
                listOf("base", "graphics", "swing", "media").forEach { module ->
                    implementation("org.openjfx:javafx-$module:$javafxVersion:$platform")
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs(
            "--add-opens", "javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sick"
            packageVersion = "1.0.0"
            modules("javafx.base", "javafx.graphics", "javafx.swing", "javafx.media")
        }
    }
}
