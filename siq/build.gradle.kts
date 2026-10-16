plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation(project(":siq:xml"))
    implementation(project(":core"))
    implementation(libs.kotlin.logging)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}