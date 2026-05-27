plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
