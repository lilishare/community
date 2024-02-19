package com.lili.community.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

/**
 * 用户角色枚举
 */
public enum ActiveEnum {

    ACTIVATION_CODE_ERROR("激活码错误", 0),
    REACTIVATION("重复激活", 1),
    FAILED("激活失败", 2),
    SUCCEED("成功激活", 3);

    private final String text;

    private final int value;

    ActiveEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static ActiveEnum getEnumByValue(int value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ActiveEnum anEnum : ActiveEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
