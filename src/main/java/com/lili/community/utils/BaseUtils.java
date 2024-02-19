package com.lili.community.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

/**
 * @projectName: community
 * @package: com.lili.community.utils
 * @className: BaseUtils
 * @author: lili
 * @description: 基础工具类
 * @date: 2024/2/16 21:30
 * @version: 1.0
 */
public class BaseUtils {
    // 生成随机字符串
    public static String genUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    // 加密
    public static String MD5(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());

    }
}
