package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
public class Wrong {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "answer")
    private List<String> answer;
}