package app.session

import com.sick.com.sick.siq.reader.SiqExtractor
import com.sick.com.sick.siq.reader.SiqReader
import com.sick.com.sick.siq.sanitizeForSimpleMode
import com.sick.model.Package
import java.nio.file.Path

data class LoadedPack(
    val pack: Package,
    val extractedBasePath: Path,
)

class PackLoader(
    private val reader: SiqReader = SiqReader(),
    private val tempDir: String = System.getProperty("java.io.tmpdir"),
) {
    fun load(path: Path): Result<LoadedPack> = runCatching {
        val extracted = SiqExtractor(
            source = path.toString(),
            destination = tempDir,
        ).extract()
        val pack = reader.read(extracted).sanitizeForSimpleMode()
        LoadedPack(pack = pack, extractedBasePath = extracted)
    }
}
