package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "Method")
@XmlAccessorType(XmlAccessType.FIELD)
public class Method {

    @XmlAttribute(name = "class")
    private String clazz;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "args")
    private String args;

    @XmlAttribute(name = "ref")
    private String ref;
}
