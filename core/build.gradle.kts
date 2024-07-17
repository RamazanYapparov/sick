plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.github.nsk90:kstatemachine:0.30.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}