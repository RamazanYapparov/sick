package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;

import java.lang.reflect.Parameter;
import java.util.List;

@Data
public class Param {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private String type;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Item> item;
    @JacksonXmlProperty
    private NumberSet numberSet;
    @JacksonXmlText
    private String value;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Param> param;
}
