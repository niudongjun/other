package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "Grid")
@XmlAccessorType(XmlAccessType.FIELD)
public class Grid {

    @XmlAttribute(name = "row")
    private Integer row;

    @XmlAttribute(name = "column")
    private Integer column;

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "format")
    private String format;
}
