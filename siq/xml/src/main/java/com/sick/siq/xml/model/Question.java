package com.sick.siq.xml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class Question {
    @JacksonXmlProperty(localName = "price", isAttribute = true)
    private int price;
    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;
    private Info info;
    private Params params;
    private Right right;
    private Wrong wrong;
}
