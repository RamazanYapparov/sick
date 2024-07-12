package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@JacksonXmlRootElement
@Data
public class Package {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private String version;
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true)
    private String date;
    @JacksonXmlProperty(isAttribute = true)
    private String publisher;
    @JacksonXmlProperty(isAttribute = true)
    private String difficulty;
    @JacksonXmlProperty private Tags tags;
    @JacksonXmlProperty private Info info;
    @JacksonXmlProperty private Rounds rounds;
}
