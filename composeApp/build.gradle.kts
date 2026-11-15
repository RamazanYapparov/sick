import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.net.URI
import java.security.MessageDigest

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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
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
            implementation(project(":siq:xml"))
            implementation(project(":server"))
            implementation(libs.kotlin.logging)
            implementation(libs.logback.classic)
            implementation(libs.zxing.core)
            implementation(libs.vlcj)
            implementation(libs.jackson.dataformat.xml)
            implementation(libs.jackson.module.parameter.names)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Files copied into <app>/resources: legal notices for every platform, libVLC for Windows.
val appResources = layout.buildDirectory.dir("app-resources")

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs(
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sick"
            packageVersion = "1.0.0"
            // jdeps (suggestRuntimeModules) finds all but java.naming, which logback's config loading needs.
            modules("java.instrument", "java.management", "java.naming", "java.sql", "jdk.unsupported")
            // libVLC for Windows lands in <app>/resources/vlc; main() loads it from there.
            appResourcesRootDir.set(appResources)
        }
    }
}

// libVLC, pinned and checksummed, so a packaged build never depends on an installed VLC.
val vlcVersion = "3.0.24"
val vlcSha256 = "fcf30850371ad10c9373cc4f0f4501e7dee49e3e9ae9f20c72fb2661a1ca6323"
val vlcArchive = layout.buildDirectory.file("vlc/vlc-$vlcVersion-win64.zip")

fun File.sha256(): String = inputStream().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(1 shl 16)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

val downloadVlc by tasks.registering {
    description = "Downloads the pinned libVLC build for Windows."
    inputs.property("sha256", vlcSha256)
    outputs.file(vlcArchive)
    doLast {
        val archive = vlcArchive.get().asFile
        // A build script change reruns this task; an archive that already checks out is kept.
        if (archive.isFile && archive.sha256() == vlcSha256) return@doLast
        archive.parentFile.mkdirs()
        val url = URI("https://download.videolan.org/pub/videolan/vlc/$vlcVersion/win64/vlc-$vlcVersion-win64.zip").toURL()
        // The mirror sometimes ends the stream early without an error; the checksum catches it.
        val attempts = 3
        for (attempt in 1..attempts) {
            url.openStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
            val actual = archive.sha256()
            if (actual == vlcSha256) return@doLast
            logger.warn("VLC download $attempt/$attempts: checksum $actual, expected $vlcSha256")
        }
        archive.delete()
        throw GradleException("Could not download $url with SHA-256 $vlcSha256")
    }
}

// libvlc, libvlccore, VLC's licence and the plugins, minus those the app never uses:
// GUI, stream and access output, discovery, visualization, DVD/VCD access and the encoders.
val prepareVlc by tasks.registering(Sync::class) {
    description = "Unpacks libVLC into the app resources for Windows."
    dependsOn(downloadVlc)
    from(zipTree(vlcArchive)) {
        include(
            "vlc-$vlcVersion/libvlc.dll",
            "vlc-$vlcVersion/libvlccore.dll",
            "vlc-$vlcVersion/COPYING.txt",
            "vlc-$vlcVersion/plugins/**",
        )
        exclude(
            "vlc-$vlcVersion/plugins/gui/**",
            "vlc-$vlcVersion/plugins/access_output/**",
            "vlc-$vlcVersion/plugins/stream_out/**",
            "vlc-$vlcVersion/plugins/services_discovery/**",
            "vlc-$vlcVersion/plugins/visualization/**",
            "vlc-$vlcVersion/plugins/access/libdvdnav_plugin.dll",
            "vlc-$vlcVersion/plugins/access/libdvdread_plugin.dll",
            "vlc-$vlcVersion/plugins/access/libvcd_plugin.dll",
            "vlc-$vlcVersion/plugins/codec/libx264_plugin.dll",
            "vlc-$vlcVersion/plugins/codec/libx26410b_plugin.dll",
            "vlc-$vlcVersion/plugins/codec/libx265_plugin.dll",
            "vlc-$vlcVersion/plugins/codec/libtwolame_plugin.dll",
        )
        eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
        includeEmptyDirs = false
    }
    into(appResources.map { it.dir("windows/vlc") })
}

// The project's licence and the third-party notices go with every packaged build.
val prepareLegalNotices by tasks.registering(Sync::class) {
    description = "Copies LICENSE and THIRD-PARTY-NOTICES.md into the app resources."
    from(rootProject.layout.projectDirectory) { include("LICENSE", "THIRD-PARTY-NOTICES.md") }
    into(appResources.map { it.dir("common") })
}

// The Compose plugin registers its tasks after this script runs.
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(prepareVlc, prepareLegalNotices) }

// One sick.exe that carries the whole app image: see packaging/windows/PortableLauncher.cs.
val portableDir = layout.buildDirectory.dir("compose/binaries/main/portable")
val portableWork = layout.buildDirectory.dir("portable")

val zipPortablePayload by tasks.registering(Zip::class) {
    description = "Zips the app image that the portable exe carries."
    val createDistributable = tasks.named<AbstractJPackageTask>("createDistributable")
    dependsOn(createDistributable)
    from(createDistributable.flatMap { task -> task.destinationDir.map { it.dir(task.packageName.get()) } })
    destinationDirectory.set(portableWork)
    archiveFileName.set("payload.zip")
}

val packagePortableExe by tasks.registering(Exec::class) {
    group = "compose desktop"
    description = "Builds a single self-contained sick.exe for Windows."
    onlyIf("Windows only") { System.getProperty("os.name").startsWith("Windows") }
    val launcherSource = layout.projectDirectory.file("packaging/windows/PortableLauncher.cs")
    val payload = zipPortablePayload.flatMap { it.archiveFile }
    val payloadInfo = portableWork.map { it.file("payload.txt") }
    val exe = portableDir.map { it.file("sick.exe") }
    val launcherName = tasks.named<AbstractJPackageTask>("createDistributable").flatMap { it.packageName }.map { "$it.exe" }
    inputs.file(launcherSource)
    inputs.file(payload)
    inputs.property("launcher", launcherName)
    outputs.file(exe)
    executable = "${System.getenv("WINDIR")}\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe"
    doFirst {
        payloadInfo.get().asFile.writeText("${payload.get().asFile.sha256()}\n${launcherName.get()}\n")
        exe.get().asFile.parentFile.mkdirs()
        args(
            "/nologo", "/optimize+", "/target:winexe", "/platform:x64",
            "/out:${exe.get().asFile}",
            "/reference:System.IO.Compression.dll",
            "/reference:System.IO.Compression.FileSystem.dll",
            "/reference:System.Windows.Forms.dll",
            "/resource:${payload.get().asFile},payload.zip",
            "/resource:${payloadInfo.get().asFile},payload.txt",
            launcherSource.asFile.path,
        )
    }
}
