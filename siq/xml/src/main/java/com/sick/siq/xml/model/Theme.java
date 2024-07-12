package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class Theme {
    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;
    private Questions questions;
}
