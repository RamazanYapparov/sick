package app.session

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.sick.siq.xml.model.Package as XmlPackage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

data class ScannedPackInfo(
    val path: Path,
    val packName: String,
    val author: String,
    val difficulty: String?,
    val rounds: List<ScannedRoundInfo>,
)

data class ScannedRoundInfo(
    val name: String,
    val type: String,
    val themes: List<ScannedThemeInfo>,
)

data class ScannedThemeInfo(
    val name: String,
    val questions: List<ScannedQuestionInfo>,
)

data class ScannedQuestionInfo(
    val price: Int,
)

object PackScanner {
    private val xmlMapper: XmlMapper = XmlMapper().apply {
        registerModule(ParameterNamesModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun scan(directory: Path = Paths.get(System.getProperty("user.home"), "Downloads")): List<ScannedPackInfo> {
        val dir = directory.toFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir
            .listFiles { file -> file.extension.equals("siq", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file -> scanFile(file.toPath()) }
            ?: emptyList()
    }

    private fun scanFile(path: Path): ScannedPackInfo? {
        return try {
            ZipFile(path.toFile()).use { zipFile ->
                val entry = zipFile.getEntry("content.xml")
                    ?: return@use null

                val xmlPackage = zipFile.getInputStream(entry).use { inputStream ->
                    xmlMapper.readValue(inputStream, XmlPackage::class.java)
                }

                val rounds = xmlPackage.rounds?.round?.mapNotNull { round ->
                    val themes = round.themes?.theme?.mapNotNull { theme ->
                        val questions = theme.questions?.question?.map { q ->
                            ScannedQuestionInfo(price = q.price)
                        } ?: emptyList()
                        ScannedThemeInfo(
                            name = theme.name,
                            questions = questions,
                        )
                    } ?: emptyList()
                    ScannedRoundInfo(
                        name = round.name,
                        type = round.type ?: "simple",
                        themes = themes,
                    )
                } ?: emptyList()

                ScannedPackInfo(
                    path = path,
                    packName = xmlPackage.name ?: path.fileName.toString(),
                    author = xmlPackage.info?.authors?.author?.firstOrNull() ?: "",
                    difficulty = xmlPackage.difficulty,
                    rounds = rounds,
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
