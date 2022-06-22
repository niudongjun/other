package com.jsyn.util;

import org.apache.commons.collections4.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * description: UUID工具
 *
 * @author : niudongjun
 * @date : 2021/12/10 9:07
 */
public class UUIDUtil {
    private UUIDUtil() { }

    /**
     * 根据字符串生成带 - 的UUID字符串
     *
     * @param sources 字符串
     * @return UUID字符串
     */
    public static String buildOriginUUID(String ... sources) {
        if (CollectionUtils.sizeIsEmpty(sources)) {
            return null;
        }
        return UUID.nameUUIDFromBytes(Arrays.toString(sources).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 随机生成带 - 的UUID字符串
     *
     * @return UUID字符串
     */
    public static String buildOriginUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 根据字符串生成不带 - 的UUID字符串
     *
     * @param sources 字符串
     * @return UUID字符串
     */
    public static String buildSimpleUUID(String ... sources) {
        String originUUID = buildOriginUUID(sources);
        return originUUID == null ? null : originUUID.replaceAll("-", "");
    }

    /**
     * 随机生成不带 - 的UUID字符串
     *
     * @return UUID字符串
     */
    public static String buildSimpleUUID() {
        return buildOriginUUID().replaceAll("-", "");
    }
}
