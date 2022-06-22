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
@XmlRootElement(name = "FileOption")
@XmlAccessorType(XmlAccessType.FIELD)
public class FileOption {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "suffix")
    private String suffix;

    @XmlAttribute(name = "dir")
    private String dir;

    @XmlAttribute(name = "toDir")
    private String toDir;

    @XmlAttribute(name = "rename")
    private String rename;

    @XmlAttribute(name = "protocol")
    private String protocol;

    @XmlAttribute(name = "mode")
    private String mode;

    @XmlAttribute(name = "group")
    private Boolean group;

    @XmlAttribute(name = "header")
    private Boolean header;

    @XmlElementWrapper(name = "Sheets")
    @XmlElement(name = "Sheet")
    private List<Sheet> sheets;

    @XmlElementWrapper(name = "Tables")
    @XmlElement(name = "Table")
    private List<Table> tables;

    @XmlElementWrapper(name = "Parameters")
    @XmlElement(name = "Parameter")
    private List<Parameter> parameters;

    @XmlElementWrapper(name = "ValidFileMethods")
    @XmlElement(name = "Method")
    private List<Method> validFileMethods;

    @XmlElementWrapper(name = "MatchFileMethod")
    @XmlElement(name = "Method")
    private List<Method> matchFileMethod;

    @XmlElementWrapper(name = "PreHandleFileMethod")
    @XmlElement(name = "Method")
    private List<Method> preHandleFileMethod;

    @XmlElementWrapper(name = "HandleFileMethod")
    @XmlElement(name = "Method")
    private List<Method> handleFileMethod;

    @XmlElementWrapper(name = "AfterHandleFileMethod")
    @XmlElement(name = "Method")
    private List<Method> afterHandleFileMethod;

    @XmlElementWrapper(name = "PreLoadTableMethod")
    @XmlElement(name = "Method")
    private List<Method> preLoadTableMethod;

    @XmlElementWrapper(name = "LoadTableMethod")
    @XmlElement(name = "Method")
    private List<Method> loadTableMethod;

    @XmlElementWrapper(name = "AfterLoadTableMethod")
    @XmlElement(name = "Method")
    private List<Method> afterLoadTableMethod;
}
