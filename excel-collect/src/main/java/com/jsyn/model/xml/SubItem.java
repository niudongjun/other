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
@XmlRootElement(name = "SubItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class SubItem {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "FileOption")
    @XmlElementWrapper(name = "FileOptions")
    private List<FileOption> fileOptions;
}
