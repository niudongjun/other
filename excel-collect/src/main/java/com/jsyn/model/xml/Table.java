package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "Table")
@XmlAccessorType(XmlAccessType.FIELD)
public class Table {

    @XmlAttribute(name = "id")
    private Integer id;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "dir")
    private String dir;

    @XmlAttribute(name = "sheet")
    private Integer sheet;

    @XmlAttribute(name = "isLoad")
    private Boolean isLoad;

    @XmlElementWrapper(name = "Fields")
    @XmlElement(name = "Field")
    private List<Field> fields;
}
