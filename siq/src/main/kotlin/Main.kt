package com.sick

import com.sick.com.sick.siq.reader.SiqExtractor
import com.sick.com.sick.siq.reader.SiqReader
// import com.sick.com.sick.siq.reader.process

fun main() {
    val path = """C:\Users\ramazan\Downloads\Solyanka_iz_soli_s_privkusom_randoma_2.siq"""
//    process("/Users/ryapparov/src/etc/sick/pack/oldone/Ночные посиделки_ Кинопак №1.siq")
    val tmpPath = SiqExtractor(path, "tempdirpath").extract()
    val result = SiqReader().read(tmpPath)
    println(result)
}