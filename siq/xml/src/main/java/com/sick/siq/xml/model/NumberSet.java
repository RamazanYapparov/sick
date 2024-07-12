package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class NumberSet {
    @JacksonXmlProperty(localName = "minimum", isAttribute = true)
    private int minimum;
    @JacksonXmlProperty(localName = "maximum", isAttribute = true)
    private int maximum;
    @JacksonXmlProperty(localName = "step", isAttribute = true)
    private int step;

    // Getters and setters
}
