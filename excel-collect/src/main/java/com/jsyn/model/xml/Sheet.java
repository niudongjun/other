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
@XmlRootElement(name = "Sheet")
@XmlAccessorType(XmlAccessType.FIELD)
public class Sheet {

    @XmlAttribute(name = "index")
    private Integer index;

    @XmlElementWrapper(name = "Grids")
    @XmlElement(name = "Grid")
    private List<Grid> grids;

    @XmlElementWrapper(name = "Groups")
    @XmlElement(name = "Group")
    private List<Group> groups;
}
