package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
public class Info {
    @JacksonXmlProperty
    private Authors authors;
    @JacksonXmlProperty
    private String comments;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty
    private List<String> sources;
}
