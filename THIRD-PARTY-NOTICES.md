# Third-party notices

sick is licensed under the GNU General Public License, version 3 (see `LICENSE`). The Windows build (`sick.exe`, made by `:composeApp:packagePortableExe`) also carries the components below. Their licences and source code are listed here. In a packaged build, this file, `LICENSE` and VLC's own licence (`vlc/COPYING.txt`) are in the app's `resources` folder.

## VLC (libVLC 3.0.24)

- Files: `resources/vlc/libvlc.dll`, `libvlccore.dll` and `plugins/`, taken unchanged from VideoLAN's `vlc-3.0.24-win64.zip`. Plugins the app never uses (GUI, stream output, access output, service discovery, visualization, DVD/VCD access and the x264, x265 and TwoLAME encoders) are left out.
- Licence: libVLC and libvlccore are under the GNU LGPL version 2.1 or later. VLC as a whole, and some of its plugins (such as faad and postproc), are under the GNU GPL version 2 or later. See `resources/vlc/COPYING.txt`.
- Source code: <https://download.videolan.org/pub/videolan/vlc/3.0.24/vlc-3.0.24.tar.xz>. Source code of the libraries VLC is built with (contribs): <https://download.videolan.org/pub/videolan/contrib/>.
- Project: <https://www.videolan.org/>

## Java libraries

| Component | Licence | Source |
|---|---|---|
| vlcj 4.12.1, vlcj-natives 4.12.0 | GPL v3 or later | <https://github.com/caprica/vlcj> |
| JNA 5.13.0 / 5.16.0 | Apache 2.0 or LGPL 2.1 (used under LGPL 2.1) | <https://github.com/java-native-access/jna> |
| Logback 1.5.6 | EPL 1.0 or LGPL 2.1 (used under LGPL 2.1) | <https://github.com/qos-ch/logback> |
| Compose Multiplatform 1.6.11, AndroidX (annotation, collection, lifecycle) | Apache 2.0 | <https://github.com/JetBrains/compose-multiplatform> |
| Skiko 0.8.4 | Apache 2.0 | <https://github.com/JetBrains/skiko> |
| Kotlin standard library, kotlin-reflect, kotlinx.coroutines, atomicfu, JetBrains annotations | Apache 2.0 | <https://github.com/JetBrains/kotlin>, <https://github.com/Kotlin/kotlinx.coroutines> |
| Ktor 2.3.12 | Apache 2.0 | <https://github.com/ktorio/ktor> |
| Arrow 1.2.4 | Apache 2.0 | <https://github.com/arrow-kt/arrow> |
| Jackson 2.17.2 (core, databind, annotations, dataformat-xml, module-kotlin, module-parameter-names) | Apache 2.0 | <https://github.com/FasterXML/jackson> |
| Woodstox 6.7.0 | Apache 2.0 | <https://github.com/FasterXML/woodstox> |
| Stax2 API 4.2.2 | BSD 2-Clause | <https://github.com/FasterXML/stax2-api> |
| ZXing core 3.5.3 | Apache 2.0 | <https://github.com/zxing/zxing> |
| kotlin-logging 5.1.4 | Apache 2.0 | <https://github.com/oshai/kotlin-logging> |
| SLF4J API 2.0.13 | MIT | <https://github.com/qos-ch/slf4j> |
| Typesafe Config 1.4.3 | Apache 2.0 | <https://github.com/lightbend/config> |
| Jansi 2.4.1 | Apache 2.0 | <https://github.com/fusesource/jansi> |
| Animal Sniffer annotations 1.23 | MIT | <https://github.com/mojohaus/animal-sniffer> |
| ICU data (`icudtl.dat`, shipped with Skiko) | Unicode License v3 | <https://github.com/unicode-org/icu> |

The libraries' own licence and notice files are kept inside their jars (`META-INF/`).

## Java runtime

The bundled runtime (`runtime/`) is OpenJDK 21 (BellSoft Liberica), made smaller with `jlink`. Licence: GNU GPL version 2 with the Classpath Exception. Its legal notices are in `runtime/legal/`. Source code: <https://github.com/openjdk/jdk21u>.
