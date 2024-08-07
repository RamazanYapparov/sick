plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation(project(":siq:xml"))
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}