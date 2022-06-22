package com.jsyn.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FieldTransMethod {

    public static String getTime() {
        return LocalDateTime.now().toString();
    }

    public static String getGroup(String[] strs) {
        return strs.length + "";
    }

    /**
     * 匹配文件方法模板：MatchFileMethod
     *
     * @param data 每行数据<行标，每列数据列表>
     * @return 是否是符合条件的文件
     */
    public static boolean matchFileMethodTemplate(Map<Integer, List<String>> data) {
        return true;
    }

    /**
     * 参数校验样例 ValidFileMethods
     *
     * @param validMap 每行的数据 <行标，每列的数据>
     * @return 是否通过
     */
    public static boolean vaildMethodTemplate(Map<Integer, List<String>> validMap) {
        return true;
    }

    /**
     * 方法模板：PreHandleFileMethod/HandleFileMethod/AfterHandleFileMethod/PreLoadTableMethod
     *
     * @param data 每行数据<行标，每列数据列表>
     * @return 每行数据<行标，每列数据列表>
     */
    public static Map<Integer, List<String>> fileMethodTemplate(Map<Integer, List<String>> data) {
        return null;
    }

    /**
     * 方法模板：LoadTableMethod/AfterLoadTableMethod
     *
     * @param data 每行数据<行标，每列数据列表>
     * @return 是否执行成功
     */
    public static boolean loadMethodTemplate(Map<Integer, List<String>> data) {
        return true;
    }

 }
