package com.jsyn.util;

import com.jsyn.model.entity.XlsSheetValue;
import com.jsyn.model.xml.CollectFileCfg;
import com.jsyn.model.xml.Field;
import com.jsyn.model.xml.FileOption;
import com.jsyn.model.xml.Grid;
import com.jsyn.model.xml.Group;
import com.jsyn.model.xml.Sheet;
import com.jsyn.model.xml.SubItem;
import com.jsyn.model.xml.Table;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import lombok.Cleanup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * description: 文件采集 抽取工具类
 *
 * @author : niudongjun
 * @date : 2021/12/3
 */
public class CollectFileUtil {
    private static final Logger logger = LoggerFactory.getLogger(CollectFileUtil.class);

    private CollectFileUtil() {

    }

    private static final String COLLECT_FILE_CFG_XML_PATH = "classpath:collect/CollectFileCfg.xml";

    private static final String COLLECT_FILE_CFG_XSD_PATH = "collect/CollectFileCfgXsd.xml";

    private static final String COLLECT_FILE_CFG_XSD_TMP_PATH = "./CollectFileCfgXsd.xml";

    private static final CollectFileCfg COLLECTFILECFG = initCollectFileCfg();

    private static CollectFileCfg initCollectFileCfg() {
        try {
            ClassPathResource classPathResource = new ClassPathResource(COLLECT_FILE_CFG_XSD_PATH);
            @Cleanup InputStream xsdFileInput = classPathResource.getInputStream();
            File xsdFile = new File(COLLECT_FILE_CFG_XSD_TMP_PATH);
            FileUtils.copyInputStreamToFile(xsdFileInput, xsdFile);
            File xmlFile = ResourceUtils.getFile(COLLECT_FILE_CFG_XML_PATH);
            JAXBContext context = JAXBContext.newInstance(CollectFileCfg.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsdFile);
            unmarshaller.setSchema(schema);
            return (CollectFileCfg) unmarshaller.unmarshal(xmlFile);
        } catch (IOException e) {
            logger.error("Get file failed. {}", e.getMessage());
            throw new RuntimeException("Get file failed.", e);
        } catch (SAXException saxException) {
            logger.error("Init xml schema failed. {}", saxException.getMessage());
            throw new RuntimeException("Init xml schema failed.", saxException);
        } catch (JAXBException jaxbException) {
            logger.error("Parse xml error. {}", jaxbException.getMessage());
            throw new RuntimeException("Parse xml error.", jaxbException);
        }
    }

    /**
     * 获取xml配置
     *
     * @return xml配置实体
     */
    public static CollectFileCfg getCollectFileCfg() {
        return COLLECTFILECFG;
    }

    /**
     * 获取一个子项目的配置
     *
     * @param itemName 子项目名
     * @return 子 项目实体
     */
    public static SubItem getSubItem(String itemName) {
        List<SubItem> subItems = COLLECTFILECFG.getSubItems();
        if (CollectionUtils.isEmpty(subItems)) {
            return null;
        }
        List<SubItem> items = subItems.stream().filter(val -> val.getName().equals(itemName)).collect(Collectors.toList());
        return items.get(0);
    }

    private static boolean validXlsFile(FileOption fileOption, Integer sheetIndex, List<XlsSheetValue> sheetsValues) {
        if  (fileOption == null) {
            return false;
        }
        List<com.jsyn.model.xml.Method> validFileMethods = fileOption.getValidFileMethods();
        if (CollectionUtils.isEmpty(validFileMethods)) {
            return true;
        }
        if (CollectionUtils.isEmpty(sheetsValues)) {
            return false;
        }
        return validMap(validFileMethods, sheetIndex, parseXlsListToMap(sheetsValues));
    }

    private static boolean validMap(List<com.jsyn.model.xml.Method> validFileMethods, Integer sheetIndex, Map<Integer, List<String>> validMap) {
        for (com.jsyn.model.xml.Method method : validFileMethods) {
            if (isNotRightMethod(method, sheetIndex)) {
                continue;
            }
            String clazz = method.getClazz();
            String name = method.getName();
            Object value = getValueFromMethod(clazz, name, true, validMap);
            if (!(value != null && (boolean) value)) {
                return false;
            }
        }
        return true;
    }

    private static Map<Integer, List<String>> parseXlsListToMap(List<XlsSheetValue> sheetsValues) {
        // 获取所有行，行级校验
        Map<Integer, List<XlsSheetValue>> collect = sheetsValues.stream().collect(Collectors.groupingBy(XlsSheetValue::getRowIndex));
        Map<Integer, List<String>> validMap = new HashMap<>(collect.size());
        for (Map.Entry<Integer, List<XlsSheetValue>> entry : collect.entrySet()) {
            validMap.put(entry.getKey(), entry.getValue().stream().map(XlsSheetValue::getValue).collect(Collectors.toList()));
        }
        return validMap;
    }

    private static boolean isNotRightMethod(com.jsyn.model.xml.Method method, Integer index) {
        return !isRightMethod(method, index);
    }

    private static boolean isRightMethod (com.jsyn.model.xml.Method method, Integer index) {
        String ref = method.getRef();
        if (StringUtils.isBlank(ref)
                || Arrays.stream(ref.split(",")).map(Integer::parseInt).noneMatch(val -> val.equals(index))) {
            return false;
        }
        String clazz = method.getClazz();
        String name = method.getName();
        return !StringUtils.isBlank(clazz) && !StringUtils.isBlank(name);
    }

    private static List<XlsSheetValue> preHandleFile(com.jsyn.model.xml.Method method, Integer sheetIndex, List<XlsSheetValue> sheetsValues) {
        if (CollectionUtils.isEmpty(sheetsValues)) {
            return Collections.emptyList();
        }
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, parseXlsListToMap(sheetsValues));
        if (value != null) {
            return parseRowStringMapToBeanList((Map<Integer, List<String>>) value, sheetIndex);
        }
        return null;
    }

    private static List<XlsSheetValue> parseRowStringMapToBeanList(Map<Integer, List<String>> mapLines, Integer sheetIndex) {
        List<XlsSheetValue> result = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : mapLines.entrySet()) {
            List<String> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                XlsSheetValue sheetValue = new XlsSheetValue();
                sheetValue.setSheetIndex(sheetIndex);
                sheetValue.setRowIndex(entry.getKey());
                sheetValue.setColumnIndex(i);
                sheetValue.setValue(list.get(i));
                result.add(sheetValue);
            }
        }
        return result;
    }

    private static Map<Integer, List<XlsSheetValue>> parseRowBeanListToRowBeanMap(List<XlsSheetValue> list) {
        return list.stream().collect(Collectors.groupingBy(XlsSheetValue::getRowIndex));
    }

    private static Map<Integer, List<XlsSheetValue>> handleFile(com.jsyn.model.xml.Method method, Integer sheetIndex, List<XlsSheetValue> sheetsValues) {
        if (CollectionUtils.isEmpty(sheetsValues)) {
            return new HashMap<>(0);
        }
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, parseXlsListToMap(sheetsValues));
        if (value != null) {
            return parseRowBeanListToRowBeanMap(parseRowStringMapToBeanList((Map<Integer, List<String>>) value, sheetIndex));
        }
        return null;
    }

    private static Map<Integer, List<String>> parseRowBeanMapToRowStringMap(Map<Integer, List<XlsSheetValue>> data) {
        Map<Integer, List<String>> result =  new HashMap<>(data.size());
        for (Map.Entry<Integer, List<XlsSheetValue>> entry : data.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().map(XlsSheetValue::getValue).collect(Collectors.toList()));
        }
        return result;
    }

    private static Map<Integer, List<XlsSheetValue>> afterHandleFile(com.jsyn.model.xml.Method method, Integer sheetIndex, Map<Integer, List<XlsSheetValue>> data) {
        if (MapUtils.isEmpty(data)) {
            return new HashMap<>(0);
        }
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, parseRowBeanMapToRowStringMap(data));
        if (value != null) {
            return parseRowBeanListToRowBeanMap(parseRowStringMapToBeanList((Map<Integer, List<String>>) value, sheetIndex));
        }
        return null;
    }

    private static Map<Integer, List<String>> preLoadTable(com.jsyn.model.xml.Method method, Map<Integer, List<XlsSheetValue>> data) {
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, parseRowBeanMapToRowStringMap(data));
        if (value != null) {
            return (Map<Integer, List<String>>) value;
        }
        return null;
    }

    private static boolean loadTable(com.jsyn.model.xml.Method method, Map<Integer, List<String>> data) {
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, data);
        if (value != null) {
            return (boolean) value;
        }
        return false;
    }

    private static com.jsyn.model.xml.Method getMethodByIndex(Integer index, List<com.jsyn.model.xml.Method> methods) {
        if (CollectionUtils.isEmpty(methods)) {
            return null;
        }
        List<com.jsyn.model.xml.Method> collect = methods.stream().filter(method ->
                Arrays.stream(method.getRef().split(",")).anyMatch(val -> StringUtils.equals(val, String.valueOf(index))))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(collect)) {
            return null;
        }
        return collect.get(0);
    }

    private static boolean matchFile(com.jsyn.model.xml.Method method, List<XlsSheetValue> sheetsValues) {
        String clazz = method.getClazz();
        String name = method.getName();
        Object value = getValueFromMethod(clazz, name, true, parseXlsListToMap(sheetsValues));
        if (value != null) {
            return (boolean) value;
        }
        return false;
    }

    private static Map<String, Map<Integer, List<String>>> extractLoadFile(FileOption fileOption, Workbook sheets, boolean nameMatched) {
        List<Sheet> optionSheets = fileOption.getSheets();
        if (CollectionUtils.isEmpty(optionSheets)) {
            return null;
        }
        for (Sheet sheet : optionSheets) {
            Integer sheetIndex = sheet.getIndex();
            List<XlsSheetValue> curSheetValues = getSheetsValue(fileOption, sheetIndex, sheets);
            List<com.jsyn.model.xml.Method> matchFileMethod = fileOption.getMatchFileMethod();
            com.jsyn.model.xml.Method matchMethodByIndex;
            boolean matchFile = false;
            if (CollectionUtils.isNotEmpty(matchFileMethod) && (matchMethodByIndex = getMethodByIndex(sheetIndex, matchFileMethod)) != null) {
                matchFile = matchFile(matchMethodByIndex, curSheetValues);
            }
            if (!(nameMatched || matchFile)) {
                continue;
            }
            if (!validXlsFile(fileOption, sheetIndex, curSheetValues)) {
                continue;
            }
            Map<Integer, List<XlsSheetValue>> data;
            List<com.jsyn.model.xml.Method> preHandleFileMethod = fileOption.getPreHandleFileMethod();
            com.jsyn.model.xml.Method methodByIndex;
            if (CollectionUtils.isNotEmpty(preHandleFileMethod) && (methodByIndex = getMethodByIndex(sheetIndex, preHandleFileMethod)) != null) {
                curSheetValues = preHandleFile(methodByIndex, sheetIndex, curSheetValues);
            }
            List<com.jsyn.model.xml.Method> handleFileMethod = fileOption.getHandleFileMethod();
            com.jsyn.model.xml.Method handMethodByIndex;
            if (CollectionUtils.isNotEmpty(handleFileMethod) && (handMethodByIndex = getMethodByIndex(sheetIndex, handleFileMethod)) != null) {
                data = handleFile(handMethodByIndex, sheetIndex, curSheetValues);
            } else {
                if (fileOption.getGroup()) {
                    data = buildGroup(fileOption, sheet.getIndex(), curSheetValues);
                } else {
                    data = buildGrids(fileOption, sheet.getIndex(), curSheetValues);
                }
            }
            List<com.jsyn.model.xml.Method> afterHandleFileMethod = fileOption.getAfterHandleFileMethod();
            com.jsyn.model.xml.Method afterHandleMethodByIndex;
            if (CollectionUtils.isEmpty(afterHandleFileMethod) && (afterHandleMethodByIndex = getMethodByIndex(sheetIndex, afterHandleFileMethod)) != null) {
                data = afterHandleFile(afterHandleMethodByIndex, sheetIndex, data);
            }
            return buildTables(fileOption, sheet.getIndex(), data);
        }
        return null;
    }

    /**
     * 执行采集任务
     *
     * @param itemName 子项目名
     * @param fileName 文件名
     * @param inputStream 文件流
     * @return Map<String, Map < Integer, List < String>>> 表名，行标，一行数据的每一列
     */
    public static Map<String, Map<Integer, List<String>>> exec(String itemName, String fileName, InputStream inputStream) throws IOException {
        Map<String, Map<Integer, List<String>>> result = new HashMap<>();
        if (!checkItemCfg(itemName) || inputStream == null) {
            return result;
        }
        List<FileOption> fileOptions = Objects.requireNonNull(getSubItem(itemName)).getFileOptions();
        for (FileOption fileOption : fileOptions) {
            String fileOptionName = fileOption.getName();
            String fileSuffix = fileOption.getSuffix();
            if (StringUtils.isBlank(fileName)) {
                continue;
            }
            try (Workbook sheets = ".xls".equalsIgnoreCase(fileSuffix)
                    ? new HSSFWorkbook(inputStream)
                    : (".xlsx".equalsIgnoreCase(fileSuffix)
                    ? new SXSSFWorkbook(new XSSFWorkbook(inputStream)) : null)) {
                if (sheets == null) {
                    throw new RuntimeException("Not support collect file type.");
                }
                // 实际抽取
                Map<String, Map<Integer, List<String>>> loadFile = extractLoadFile(fileOption, sheets, buildFileNamePattern(fileOptionName, fileSuffix).matcher(fileName).matches());
                if (loadFile != null) {
                    result.putAll(loadFile);
                }
            }
        }
        return result;
    }

    /**
     * 执行采集任务
     *
     * @param itemName 子项目名
     * @param fileName 文件名
     * @param file 文件
     * @return Map<String, Map < Integer, List < String>>> 表名，行标，一行数据的每一列
     */
    public static Map<String, Map<Integer, List<String>>> exec(String itemName, String fileName, MultipartFile file) throws IOException {
        Map<String, Map<Integer, List<String>>> result = new HashMap<>();
        if (file == null || file.isEmpty()) {
            return result;
        }
        List<FileOption> fileOptions = Objects.requireNonNull(getSubItem(itemName)).getFileOptions();
        for (FileOption fileOption : fileOptions) {
            String fileOptionName = fileOption.getName();
            String fileSuffix = fileOption.getSuffix();
            if (StringUtils.isBlank(fileName)) {
                continue;
            }
            try (InputStream inputStream = file.getInputStream();
                 Workbook sheets = ".xls".equalsIgnoreCase(fileSuffix)
                    ? new HSSFWorkbook(inputStream)
                    : (".xlsx".equalsIgnoreCase(fileSuffix)
                    ? new SXSSFWorkbook(new XSSFWorkbook(inputStream)) : null)) {
                if (sheets == null) {
                    throw new RuntimeException("Not support collect file type.");
                }
                // 实际抽取
                Map<String, Map<Integer, List<String>>> loadFile = extractLoadFile(fileOption, sheets, buildFileNamePattern(fileOptionName, fileSuffix).matcher(fileName).matches());
                if (loadFile != null) {
                    result.putAll(loadFile);
                }
            }
        }
        return result;
    }

    /**
     * 执行猜采集任务
     *
     * @param itemName 子项目名
     * @param files http文件流
     * @return Map<String, Map < Integer, List < String>>> 表名，行标，一行数据的每一列
     */
    public static Map<String, Map<Integer, List<String>>> exec(String itemName, MultipartFile[] files) {
        Map<String, Map<Integer, List<String>>> result = new HashMap<>();
        if (files == null || files.length == 0) {
            return result;
        }
        try {
            for (MultipartFile file : files) {
                result.putAll(exec(itemName, file.getOriginalFilename(), file));
            }
        } catch (IOException e) {
            logger.error("Exec collect file to db failed.", e);
            throw new RuntimeException("Exec collect file to db failed.", e);
        }
        return result;
    }

    private static boolean checkItemCfg(String itemName) {
        SubItem subItem = getSubItem(itemName);
        if (subItem == null) {
            logger.warn("No such item task config.");
            return false;
        }
        List<FileOption> fileOptions = subItem.getFileOptions();
        return !CollectionUtils.isEmpty(fileOptions);
    }

    private static Pattern buildFileNamePattern(String fileOptionName, String fileSuffix) {
        String fileNamePre;
        Pattern pattern;
        if (StringUtils.isNotBlank(fileOptionName)) {
            if (fileOptionName.contains(".")) {
                int indexOf = fileOptionName.indexOf(".");
                fileNamePre = fileOptionName.substring(0, indexOf);
            } else {
                fileNamePre = fileOptionName;
            }
            pattern = Pattern.compile(fileNamePre + fileSuffix);
        } else {
            pattern = Pattern.compile("\\*" + fileSuffix);
        }
        return pattern;
    }

    /**
     * 执行猜采集任务
     *
     * @param itemName 子项目名
     * @return Map<String, Map < Integer, List < String>>> 表名，行标，一行数据的每一列
     */
    public static Map<String, Map<Integer, List<String>>> exec(String itemName) {
        Map<String, Map<Integer, List<String>>> result = new HashMap<>();
        if (!checkItemCfg(itemName)) {
            return result;
        }
        List<FileOption> fileOptions = Objects.requireNonNull(getSubItem(itemName)).getFileOptions();
        try {
            for (FileOption fileOption : fileOptions) {
                Path path;
                if ("local".equals(fileOption.getProtocol())) {
                    if (StringUtils.isBlank(fileOption.getDir())) {
                        continue;
                    }
                    path = Paths.get(fileOption.getDir());
                } else if ("sftp".equals(fileOption.getProtocol())) {
                    if (StringUtils.isBlank(fileOption.getToDir())) {
                        continue;
                    }
                    path = Paths.get(fileOption.getToDir());
                } else {
                    throw new RuntimeException("Not support collect file type.");
                }
                String fileOptionName = fileOption.getName();
                String fileSuffix = fileOption.getSuffix();
                FindFileVisitor filterFilesVisitor = new FindFileVisitor(fileOptionName, fileSuffix);
                Files.walkFileTree(path, filterFilesVisitor);
                List<Path> pathList = filterFilesVisitor.getFilenameList();
                for (Path filePath : pathList) {
                    try (Workbook sheets = ".xls".equalsIgnoreCase(fileSuffix)
                            ? new HSSFWorkbook(new FileInputStream(filePath.toFile()))
                            : (".xlsx".equalsIgnoreCase(fileSuffix)
                            ? new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(filePath.toFile()))) : null)) {
                        if (sheets == null) {
                            throw new RuntimeException("Not support collect file type.");
                        }
                        // 实际抽取
                        Map<String, Map<Integer, List<String>>> loadFile = extractLoadFile(fileOption, sheets, true);
                        if (loadFile != null) {
                            result.putAll(loadFile);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Exec collect file to db failed.", e);
            throw new RuntimeException("Exec collect file to db failed.", e);
        }
        return result;
    }

    /**
     * 根据列查所有的行
     *
     * @param sheetsValues 原数据列表
     * @param columnValues 列列表
     * @return 所有行
     */
    private static Map<Integer, List<XlsSheetValue>> getRowsByColumns(List<XlsSheetValue> sheetsValues, List<XlsSheetValue> columnValues) {
        return sheetsValues.stream().filter(sheetsValue -> columnValues.stream()
                .anyMatch(xlsSheetValue -> sheetsValue.getRowIndex().equals(xlsSheetValue.getRowIndex())))
                .collect(Collectors.groupingBy(XlsSheetValue::getRowIndex));
    }

    /**
     * 根据行查所有列
     *
     * @param sheetsValues 原数据列表
     * @param rowValues 行列表
     * @return 所有列
     */
    private static Map<Integer, List<XlsSheetValue>> getColumnsByRows(List<XlsSheetValue> sheetsValues, List<XlsSheetValue> rowValues) {
        return sheetsValues.stream().filter(sheetsValue -> rowValues.stream()
                .anyMatch(xlsSheetValue -> sheetsValue.getColumnIndex().equals(xlsSheetValue.getColumnIndex())))
                .collect(Collectors.groupingBy(XlsSheetValue::getColumnIndex));

    }

    /**
     * 根据列标获取该列
     *
     * @param sheetsValues 原数据
     * @param columnIndex 列标
     * @return 返回列数据数组
     */
    private static List<XlsSheetValue> findColumnsByIndex(List<XlsSheetValue> sheetsValues, Integer columnIndex) {
        return sheetsValues.stream().filter(sheetsValue ->
                sheetsValue.getColumnIndex().equals(columnIndex)).collect(Collectors.toList());
    }

    /**
     * 根据行标获取行
     *
     * @param sheetsValues 原数据
     * @param rowIndex 行标
     * @return 返回行数据列表
     */
    private static List<XlsSheetValue> findRowsByIndex(List<XlsSheetValue> sheetsValues, Integer rowIndex) {
        return sheetsValues.stream().filter(sheetsValue ->
                sheetsValue.getRowIndex().equals(rowIndex)).collect(Collectors.toList());
    }

    /**
     * 根据列值分组
     *
     * @param sheetsValues 原数据
     * @param columnIndex 列标
     * @return 分组后的这一列
     */
    private static Map<String, List<XlsSheetValue>> groupByColumn(List<XlsSheetValue> sheetsValues, Integer columnIndex) {
        return findColumnsByIndex(sheetsValues, columnIndex).stream()
                .collect(Collectors.groupingBy(XlsSheetValue::getValue, Collectors.toList()));
    }

    /**
     * 根据行值分组
     *
     * @param sheetsValues 原数据
     * @param rowIndex 行标
     * @return 分组后的这一行
     */
    private static Map<String, List<XlsSheetValue>> groupByRow(List<XlsSheetValue> sheetsValues, Integer rowIndex) {
        return findRowsByIndex(sheetsValues, rowIndex).stream()
                .collect(Collectors.groupingBy(XlsSheetValue::getValue, Collectors.toList()));
    }


    /**
     * 构建grid
     *
     * @param fileOption 文件操作配置
     * @param sheetsValues 一个sheet的值列表
     * @return 数据表
     */
    private static Map<Integer, List<XlsSheetValue>> buildGrids(FileOption fileOption, Integer sheetIndex, List<XlsSheetValue> sheetsValues) {
        Map<Integer, List<XlsSheetValue>> result = new HashMap<>();
        if (StringUtils.isBlank(fileOption.getMode())) {
            return result;
        }
        List<Sheet> sheetList = fileOption.getSheets().stream().filter(sheet -> sheet.getIndex().equals(sheetIndex)).limit(1).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sheetList) || CollectionUtils.isEmpty(sheetList.get(0).getGrids())) {
            return result;
        }
        Sheet sheet = sheetList.get(0);
        List<XlsSheetValue> xlsSheetValues = new ArrayList<>();
        for (Grid grid : sheet.getGrids()) {
            String format = grid.getFormat();
            List<XlsSheetValue> values;
            if (StringUtils.equals("row", fileOption.getMode())) {
                values = findColumnsByIndex(sheetsValues, grid.getColumn());
            } else if (StringUtils.equals("column", fileOption.getMode())) {
                values = findRowsByIndex(sheetsValues, grid.getRow());
            } else {
                continue;
            }
            if ("date".equalsIgnoreCase(grid.getType()) && StringUtils.isNotBlank(format)) {
                values.forEach(value -> value.setValue(LocalDate.parse(value.getValue(), DateTimeFormatter.ofPattern(format)).toString()));
            }
            xlsSheetValues.addAll(values);
        }
        if (StringUtils.equals("row", fileOption.getMode())) {
            return xlsSheetValues.stream().collect(Collectors.groupingBy(XlsSheetValue::getRowIndex, Collectors.toList()));
        } else {
            return xlsSheetValues.stream().collect(Collectors.groupingBy(XlsSheetValue::getColumnIndex, Collectors.toList()));
        }
    }

    /**
     * 数据分组
     *
     * @param fileOption 文件操作参数
     * @param sheetIndex sheet标识
     * @param sheetsValues 一个sheet的值列表
     * @return 结果
     */
    private static Map<Integer, List<XlsSheetValue>> buildGroup(FileOption fileOption, Integer sheetIndex, List<XlsSheetValue> sheetsValues) {
        Map<Integer, List<XlsSheetValue>> result = new IdentityHashMap<>();
        if (StringUtils.isBlank(fileOption.getMode())) {
            return result;
        }
        List<Sheet> sheetList = fileOption.getSheets().stream().filter(sheet -> sheet.getIndex().equals(sheetIndex)).limit(1).collect(Collectors.toList());
        if (!fileOption.getGroup() || CollectionUtils.isEmpty(sheetList)
                || CollectionUtils.isEmpty(sheetList.get(0).getGroups())) {
            return result;
        }
        List<Group> groups = sheetList.get(0).getGroups();
        Map<Group, Map<String, List<XlsSheetValue>>> groupMapTemp = new LinkedHashMap<>();
        for (Group group : groups) {
            Integer by = group.getBy();
            if (by == null) {
                continue;
            }
            Map<String, List<XlsSheetValue>> groupMap;
            if (StringUtils.equals("row", fileOption.getMode())) {
                groupMap = groupByColumn(sheetsValues, by);
            } else if (StringUtils.equals("column", fileOption.getMode())) {
                groupMap = groupByRow(sheetsValues, by);
            } else {
                continue;
            }
            for (Map.Entry<String, List<XlsSheetValue>> entry : groupMap.entrySet()) {
                List<XlsSheetValue> values = entry.getValue();
                Map<Integer, List<XlsSheetValue>> gridValues;
                if (StringUtils.equals("row", fileOption.getMode())) {
                    gridValues = getRowsByColumns(sheetsValues, values);
                } else {
                    gridValues = getColumnsByRows(sheetsValues, values);
                }
                List<XlsSheetValue> valuesTemp = new ArrayList<>();
                for (Map.Entry<Integer, List<XlsSheetValue>> gridEntry : gridValues.entrySet()) {
                    valuesTemp.addAll(gridEntry.getValue());
                }
                entry.setValue(valuesTemp);
            }
            groupMapTemp.put(group, groupMap);
        }
        return mergeSortMapByKey(groupMapTemp, StringUtils.equals("row", fileOption.getMode()));
    }

    private static Map<Integer, List<XlsSheetValue>> mergeSortMapByKey(Map<Group, Map<String, List<XlsSheetValue>>> groupMap, boolean isRow) {
        Map<Integer, List<XlsSheetValue>> result = new HashMap<>();
        int num = 0;
        for (Map.Entry<Group, Map<String, List<XlsSheetValue>>> entry : groupMap.entrySet()) {
            Map<String, Map<Integer, XlsSheetValue>> mergeSecondMap = new HashMap<>();
            for (Map.Entry<String, List<XlsSheetValue>> valueEntry : entry.getValue().entrySet()) {
                Map<Integer, XlsSheetValue> mergeFirstMap = new HashMap<>();
                Map<Integer, List<XlsSheetValue>> collect;
                if (isRow) {
                    collect = valueEntry.getValue().stream().collect(Collectors.groupingBy(XlsSheetValue::getColumnIndex));
                } else {
                    collect = valueEntry.getValue().stream().collect(Collectors.groupingBy(XlsSheetValue::getRowIndex));
                }
                collect.forEach((key, val) -> {
                    Optional<XlsSheetValue> reduce = val.stream().reduce((xlsSheetValue, xlsSheetValue2) -> {
                        xlsSheetValue.setValue(xlsSheetValue.getValue() + "," + xlsSheetValue2.getValue());
                        return xlsSheetValue;
                    });
                    reduce.ifPresent(xlsSheetValue -> mergeFirstMap.put(key, xlsSheetValue));
                });
                mergeSecondMap.put(valueEntry.getKey(), mergeFirstMap);
            }
            Group key = entry.getKey();
            Integer sort = key.getSort();
            Integer collector = key.getCollect();
            String operator = key.getOperator();
            if (collector != null) {
                if (StringUtils.equals("list", operator)) {
                    if (sort != null) {
                        for (Map.Entry<String, Map<Integer, XlsSheetValue>> secondEntry : mergeSecondMap.entrySet()) {
                            XlsSheetValue sortSheet = secondEntry.getValue().get(sort);
                            XlsSheetValue collectSheet = secondEntry.getValue().get(collector);
                            String[] split1 = sortSheet.getValue().split(",");
                            String[] split2 = collectSheet.getValue().split(",");
                            Map<String, String> treeMap = new TreeMap<>(String::compareTo);
                            for (int i = 0; i < split1.length; i++) {
                                treeMap.put(split1[i], split2[i]);
                            }
                            StringJoiner sortJoiner = new StringJoiner(",");
                            List<String> collectList = new ArrayList<>();
                            treeMap.forEach((sortK, vsl) -> {
                                sortJoiner.add(sortK);
                                collectList.add(vsl);
                            });
                            secondEntry.getValue().get(sort).setValue(sortJoiner.toString());
                            secondEntry.getValue().get(collector).setValue(collectList.toString());
                        }
                    } else {
                        for (Map.Entry<String, Map<Integer, XlsSheetValue>> secondEntry : mergeSecondMap.entrySet()) {
                            XlsSheetValue collectSheet = secondEntry.getValue().get(collector);
                            String[] split2 = collectSheet.getValue().split(",");
                            List<String> collectList = new ArrayList<>(Arrays.asList(split2));
                            secondEntry.getValue().get(collector).setValue(collectList.toString());
                        }
                    }
                } else if (StringUtils.equals(operator, "+") || StringUtils.equals(operator, "*")) {
                    for (Map.Entry<String, Map<Integer, XlsSheetValue>> secondEntry : mergeSecondMap.entrySet()) {
                        XlsSheetValue xlsSheetValue = secondEntry.getValue().get(collector);
                        String[] valuesStr = xlsSheetValue.getValue().split(",");
                        Optional<String> reduce = Arrays.stream(valuesStr).reduce((s, s2) -> reduce(s, s2, operator));
                        reduce.ifPresent(xlsSheetValue::setValue);
                        secondEntry.getValue().replace(collector, xlsSheetValue);
                    }
                } else if (key.getMethod() != null && StringUtils.isNotBlank(key.getMethod().getClazz())
                        && StringUtils.isNotBlank(key.getMethod().getName())) {
                    for (Map.Entry<String, Map<Integer, XlsSheetValue>> secondEntry : mergeSecondMap.entrySet()) {
                        XlsSheetValue xlsSheetValue = secondEntry.getValue().get(collector);
                        String[] valuesStr = xlsSheetValue.getValue().split(",");
                        xlsSheetValue.setValue(String.valueOf(getValueFromMethod(key.getMethod().getClazz(), key.getMethod().getName(), true, (Object) valuesStr)));
                        secondEntry.getValue().replace(collector, xlsSheetValue);
                    }
                } else {
                    continue;
                }
                for (Map.Entry<String, Map<Integer, XlsSheetValue>> secondEntry : mergeSecondMap.entrySet()) {
                    List<XlsSheetValue> resultList = new ArrayList<>();
                    Map<Integer, XlsSheetValue> valueMap = secondEntry.getValue();
                    valueMap.forEach((secKey, val) -> {
                        if (!collector.equals(secKey)) {
                            String[] strings = val.getValue().split(",");
                            val.setValue(strings[0]);
                        }
                        resultList.add(val);
                    });
                    result.put(num++, resultList);
                }
            }
        }
        return result;
    }

    private static String reduce(String num1, String num2, String operator) {
        if (isNumeric(num1) && isNumeric(num2)) {
            BigDecimal bigDecimal1 = new BigDecimal(num1);
            BigDecimal bigDecimal2 = new BigDecimal(num2);
            if (StringUtils.equals("+", operator)) {
                return bigDecimal1.add(bigDecimal2).toString();
            } else if (StringUtils.equals("*", operator)) {
                return bigDecimal1.multiply(bigDecimal2).toString();
            } else {
                throw new IllegalArgumentException("Don't support this operator of calculation.");
            }
        }
        return null;
    }

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?[0-9]+(.[0-9]+)?");

    private static boolean isNumeric(String str) {
        return NUMERIC_PATTERN.matcher(str).matches();
    }

    private static Map<String, Map<Integer, List<String>>> buildTables(FileOption fileOption, Integer sheetIndex, Map<Integer, List<XlsSheetValue>> data) {
        Map<String, Map<Integer, List<String>>> result = new HashMap<>();
        List<Table> tables = fileOption.getTables();
        if (MapUtils.isEmpty(data) || CollectionUtils.isEmpty(tables) || sheetIndex == null) {
            return result;
        }
        for (Table table : tables) {
            if (!sheetIndex.equals(table.getSheet())) {
                continue;
            }
            String tableName = table.getName();
            List<Field> fields = table.getFields();
            if (CollectionUtils.isEmpty(fields)) {
                continue;
            }
            Map<Integer, List<String>> map;
            List<com.jsyn.model.xml.Method> preLoadTableMethod = fileOption.getPreLoadTableMethod();
            com.jsyn.model.xml.Method method;
            if (CollectionUtils.isNotEmpty(preLoadTableMethod) && (method = getMethodByIndex(table.getId(), preLoadTableMethod)) != null) {
                map = preLoadTable(method, data);
            } else {
                map = new HashMap<>();
                boolean headerNum = true;
                for (Map.Entry<Integer, List<XlsSheetValue>> entry : data.entrySet()) {
                    List<String> fieldVals = new ArrayList<>(fields.size());
                    if (fileOption.getHeader() && headerNum) {
                        headerNum = false;
                        continue;
                    }
                    for (Field field : fields) {
                        if (field.getMethod() != null && StringUtils.isNotBlank(field.getMethod().getClazz())
                                && StringUtils.isNotBlank(field.getMethod().getName())) {
                            String transMethodClass = field.getMethod().getClazz();
                            String methodName = field.getMethod().getName();
                            String args = field.getMethod().getArgs();
                            String[] argsArray = null;
                            if (args != null) {
                                String[] split = args.split(",");
                                int len = split.length;
                                argsArray = new String[len];
                                for (int i = 0; i < len; i++) {
                                    if (StringUtils.isNumeric(split[i])) {
                                        int anInt = Integer.parseInt(split[i]);
                                        argsArray[i] = entry.getValue().get(anInt).getValue();
                                    }
                                }
                            }
                            fieldVals.add(String.valueOf(getValueFromMethod(transMethodClass, methodName, false, argsArray)));
                            continue;
                        }
                        if (StringUtils.equals(field.getName(), "id") && StringUtils.equals("UUID", field.getType())) {
                            fieldVals.add(UUIDUtil.buildSimpleUUID());
                            continue;
                        }
                        Integer collect = field.getCollect();
                        if (collect == null) {
                            continue;
                        }
                        fieldVals.add(entry.getValue().get(collect).getValue());
                    }
                    map.put(entry.getKey(), fieldVals);
                }
            }
            if (table.getIsLoad() && MapUtils.isNotEmpty(map)) {
                List<com.jsyn.model.xml.Method> loadTableMethod = fileOption.getLoadTableMethod();
                com.jsyn.model.xml.Method loadMethod;
                if (CollectionUtils.isNotEmpty(loadTableMethod) && (loadMethod = getMethodByIndex(table.getId(), loadTableMethod)) != null) {
                    if (!loadTable(loadMethod, map)) {
                        logger.error("Load date to table {} failed.", tableName);
                    }
                } else {
                    String dir = table.getDir();
                    writeDataToCSV(map, dir);
                    loadDataToDB(tableName, dir);
                }
                List<com.jsyn.model.xml.Method> afterLoadTableMethod = fileOption.getAfterLoadTableMethod();
                com.jsyn.model.xml.Method afterLoadMethod;
                if (CollectionUtils.isNotEmpty(afterLoadTableMethod) && (afterLoadMethod = getMethodByIndex(table.getId(), afterLoadTableMethod)) != null) {
                    if (!loadTable(afterLoadMethod, map)) {
                        logger.error("Exec method after load date to table {} failed.", tableName);
                    }
                }
            }
            result.put(tableName, map);
        }
        return result;
    }

    private static final Map<String, Method> METHOD_CACHE_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Object> INSTANCE_CACHE_MAP = new ConcurrentHashMap<>();

    private static Object getValueFromMethod(String transMethodClass, String methodName, boolean isOneArg, Object... args) {
        try {
            Object newInstance = INSTANCE_CACHE_MAP.get(transMethodClass);
            Method method = METHOD_CACHE_MAP.get(transMethodClass + "." + methodName);
            int parameterCount;
            if (newInstance == null || method == null) {
                Class<?> newClass = Class.forName(transMethodClass);
                newInstance = newClass.newInstance();
                INSTANCE_CACHE_MAP.put(transMethodClass, newInstance);
                Method[] methods = newClass.getMethods();
                for (Method methodTmp : methods) {
                    if (StringUtils.equals(methodName, methodTmp.getName())) {
                        METHOD_CACHE_MAP.put(transMethodClass + "." + methodName, methodTmp);
                        method = methodTmp;
                        break;
                    }
                }
            }
            if (method == null) {
                return null;
            }
            if (args == null) {
                return String.valueOf(method.invoke(newInstance));
            } else {
                parameterCount = method.getParameterCount();
                int len = args.length;
                Object[] objs = new Object[parameterCount];
                if (isOneArg) {
                    return method.invoke(newInstance, args);
                } else {
                    System.arraycopy(args, 0, objs, 0, len);
                    return method.invoke(newInstance, objs);
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Not found class Exception.", e);
            throw new RuntimeException("Not found class Exception.", e);
        } catch (IllegalAccessException e) {
            logger.error("Error access exception.", e);
            throw new RuntimeException("Error access exception.", e);
        } catch (InstantiationException e) {
            logger.error("Create class {} instance failed.", transMethodClass, e);
            throw new RuntimeException("Create class " + transMethodClass + " instance failed.", e);
        } catch (InvocationTargetException e) {
            logger.error("Invoke method {} Exception.", transMethodClass + "." + methodName, e);
            throw new RuntimeException("Invoke method " + transMethodClass + "." + methodName + " Exception.", e);
        }
    }

    private static void writeDataToCSV(Map<Integer, List<String>> map, String dir) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir)))) {
            for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
                StringJoiner joiner = new StringJoiner("|");
                entry.getValue().forEach(joiner::add);
                writer.write(joiner.toString());
                writer.newLine();
            }
        } catch (FileNotFoundException e) {
            logger.error("Not found file exception.");
            throw new RuntimeException("Not found file exception.", e);

        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);

        }
    }

    private static void loadDataToDB(String tableName, String fileDir) {
        String replace = fileDir.replace("\\", "\\\\");
        String sql = "LOAD DATA LOCAL INFILE '" + replace + "' REPLACE INTO TABLE " + tableName + " FIELDS TERMINATED BY '|'";
//        JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);
//        jdbcTemplate.execute(sql);
    }

    private static List<XlsSheetValue> getSheetsValue(FileOption fileOption, Integer sheetIndex, Workbook sheets) {
        List<XlsSheetValue> sheetValues = new ArrayList<>();
        if (CollectionUtils.isEmpty(fileOption.getSheets())) {
            return sheetValues;
        }
        org.apache.poi.ss.usermodel.Sheet sheetAt = sheets.getSheetAt(sheetIndex);
        int lastRowNum = sheetAt.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++) {
            int j = 0;
            Iterator<Cell> cellIterator = sheetAt.getRow(i).cellIterator();
            while (cellIterator.hasNext()) {
                XlsSheetValue value = new XlsSheetValue();
                value.setSheetIndex(sheetIndex);
                value.setRowIndex(i);
                value.setColumnIndex(j);
                value.setValue(cellIterator.next().getStringCellValue());
                sheetValues.add(value);
                j++;
            }
        }
        return sheetValues;
    }

    public static void main(String[] args) throws IOException {
//        exec("item1", false);

    }

    private static class FindFileVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> filenameList = new LinkedList<>();
        private Pattern pattern;

        public FindFileVisitor(String fileName, String fileSuffix) {
            if (StringUtils.isNotBlank(fileSuffix)) {
                this.pattern = buildFileNamePattern(fileName, fileSuffix);
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path fileName = file.getFileName();
            if (pattern != null) {
                //正则表达式+文件后缀名匹配整个文件名
                //例如pattern \\d{13}  后缀.wav组成新的表达式  "\\d{13}\\.wav"
                if (!pattern.matcher(fileName.toString()).matches()) {
                    return FileVisitResult.CONTINUE;
                }
            }
            filenameList.add(file.normalize());
            return FileVisitResult.CONTINUE;
        }

        public List<Path> getFilenameList() {
            return filenameList;
        }
    }
}
