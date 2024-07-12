package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
public class Authors {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "author")
    private List<String> author;
}
