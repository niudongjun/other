package com.jsyn.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "XlsSheetValue实体", description = "Excel文件数据格点数据收集实体类")
public class XlsSheetValue implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "sheet指标")
    private Integer sheetIndex;

    @ApiModelProperty(value = "行标")
    private Integer rowIndex;

    @ApiModelProperty(value = "列标")
    private Integer columnIndex;

    @ApiModelProperty(value = "数据")
    private String value;
}
