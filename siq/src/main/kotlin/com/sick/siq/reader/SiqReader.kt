package com.sick.com.sick.siq.reader

import com.sick.com.sick.siq.mapper.toDomain
import com.sick.siq.xml.parser.Parser
import java.nio.file.Path

class SiqReader(private val parser: Parser = Parser()) {

    fun read(source: Path) = parser.parse(source).toDomain()
}