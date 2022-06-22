package com.jsyn.model.xml;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "CollectFileCfg")
@XmlAccessorType(XmlAccessType.FIELD)
public class CollectFileCfg {

    @XmlElement(name = "SubItem")
    @XmlElementWrapper(name="SubItems")
    private List<SubItem> subItems;
}
