package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "Group")
@XmlAccessorType(XmlAccessType.FIELD)
public class Group {

    @XmlAttribute(name = "by")
    private Integer by;

    @XmlAttribute(name = "sort")
    private Integer sort;

    @XmlAttribute(name = "collect")
    private Integer collect;

    @XmlAttribute(name = "operator")
    private String operator;

    @XmlElement(name = "Method")
    private Method method;
}
