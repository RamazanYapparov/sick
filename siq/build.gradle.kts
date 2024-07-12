plugins {
    kotlin("jvm")
}

group = "com.sick"

dependencies {
    implementation(project(":siq:xml"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}