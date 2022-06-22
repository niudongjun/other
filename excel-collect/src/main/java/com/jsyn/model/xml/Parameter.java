package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * description:
 *
 * @author : niudongjun
 * @date : 2021/12/10 11:10
 */

@Data
@XmlRootElement(name = "Parameter")
@XmlAccessorType(XmlAccessType.FIELD)
public class Parameter {

    @XmlAttribute(name = "key")
    private String key;

    @XmlAttribute(name = "value")
    private String value;

    @XmlAttribute(name = "type")
    private String type;
}
