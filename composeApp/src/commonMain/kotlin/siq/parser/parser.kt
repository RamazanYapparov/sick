package siq.parser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.File



fun main() {
    val file = File("/Users/ryapparov/src/etc/sick/pack/content.xml")
    val xmlMapper = XmlMapper()
    val value = xmlMapper.readValue(file, String::class.java)
    println(value)
}