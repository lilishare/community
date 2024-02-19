package com.lili.community.utils;

import cn.hutool.core.io.FileUtil;
import com.lili.community.common.ErrorCode;
import com.lili.community.exception.BusinessException;
import com.lili.community.model.enums.FileUploadBizEnum;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * @projectName: community
 * @package: com.lili.community.utils
 * @className: FileUtils
 * @author: lili
 * @description: TODO
 * @date: 2024/2/17 23:27
 * @version: 1.0
 */
public class FileUtils {
    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    public static String validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                return "文件大小不能超过 1M";
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                return "文件类型错误";
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
        return null;
    }
}