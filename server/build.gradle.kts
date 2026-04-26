plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation(project(":core"))
    implementation(libs.arrow.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
