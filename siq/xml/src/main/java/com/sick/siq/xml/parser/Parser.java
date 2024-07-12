package com.sick.siq.xml.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sick.siq.xml.model.Package;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Path;

public class Parser {

    @SneakyThrows
    public Package parse(Path packageFile) {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new ParameterNamesModule());
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File file = packageFile.resolve("content.xml").toFile();
        return xmlMapper.readValue(file, Package.class);
    }
}
