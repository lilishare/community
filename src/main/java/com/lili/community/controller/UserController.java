package com.lili.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.code.kaptcha.Producer;
import com.lili.community.annotation.AuthCheck;
import com.lili.community.annotation.LoginCheck;
import com.lili.community.common.BaseResponse;
import com.lili.community.common.DeleteRequest;
import com.lili.community.common.ErrorCode;
import com.lili.community.common.ResultUtils;
import com.lili.community.exception.ThrowUtils;
import com.lili.community.model.entity.User;
import com.lili.community.model.enums.ActiveEnum;
import com.lili.community.model.enums.FileUploadBizEnum;
import com.lili.community.model.vo.LoginUserVO;
import com.lili.community.model.vo.UserVO;
import com.lili.community.service.UserService;
import com.lili.community.constant.UserConstant;
import com.lili.community.exception.BusinessException;
import com.lili.community.model.dto.user.UserAddRequest;
import com.lili.community.model.dto.user.UserLoginRequest;
import com.lili.community.model.dto.user.UserQueryRequest;
import com.lili.community.model.dto.user.UserRegisterRequest;
import com.lili.community.model.dto.user.UserUpdateMyRequest;
import com.lili.community.model.dto.user.UserUpdateRequest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.jws.HandlerChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.lili.community.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.lili.community.constant.FileConstant.COS_HOST;

/**
 * 用户接口
 */
@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private Producer kaptchaProducer;
    @Value("${community.file.path}")
    private String baseFilePath;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public String userRegister(Model model, UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return null;
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String email = userRegisterRequest.getEmail();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, email)) {
            return null;
        }
        Map<String, Object> result = userService.userRegister(userAccount, userPassword, checkPassword, email, model);
        if (result.get("result") != null && "ok".equals(result.get("result"))) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/home");
            return "/site/operate-result";
        } else {
            model.addAttribute("error", result.get("result"));
            model.addAttribute("user", userRegisterRequest);
            return "/site/register";
        }
    }

    @GetMapping("/register")
    public String userRegisterGet() {
        return "/site/register";
    }


    @GetMapping("/activation/{userId}/{code}")
    public String userActivation(Model model, @PathVariable("userId") Long userId, @PathVariable("code") String code) {
        if (userId == null || userId < 0 || StringUtils.isBlank(code)) {
            return "/index";
        }
        int result = userService.activation(userId, code);
        if (result == ActiveEnum.ACTIVATION_CODE_ERROR.getValue()) {
            // 激活错误
            model.addAttribute("msg", "激活失败,请稍后重试!");
            model.addAttribute("target", "/home");
            return "/site/operate-result";
        } else if (result == ActiveEnum.REACTIVATION.getValue()) {
            model.addAttribute("msg", "该账号已激活!");
            model.addAttribute("target", "/home");
            return "/site/operate-result";
        } else if (result == ActiveEnum.ACTIVATION_CODE_ERROR.getValue()) {
            model.addAttribute("msg", "激活码错误!");
            model.addAttribute("target", "/home");
            return "/site/operate-result";
        } else {
            // 激活成功
            model.addAttribute("msg", "激活成功,即将登录!");
            model.addAttribute("target", "/user/login");
            return "/site/operate-result";
        }

    }


    /**
     * 获取用户登录页面
     *
     * @return
     */
    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public String userLogin(Model model, UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            model.addAttribute("paramMsg", "参数为空");

            return "/site/login";
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        if (StringUtils.isBlank(userAccount)) {
            model.addAttribute("accountMsg", "账号不能为空");
            model.addAttribute("user", userLoginRequest);
            return "/site/login";
        }
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isBlank(userPassword)) {
            model.addAttribute("passwordMsg", "密码不能为空");
            model.addAttribute("user", userLoginRequest);
            return "/site/login";
        }
        String code = userLoginRequest.getCode();
        String kaptcha = (String) request.getSession().getAttribute("kaptcha");
        if (StringUtils.isAnyBlank(kaptcha, code) || !code.equalsIgnoreCase(kaptcha)) {
            model.addAttribute("codeMsg", "验证码错误");
            model.addAttribute("user", userLoginRequest);
            return "/site/login";
        }
        Map<String, Object> result = userService.userLogin(userAccount, userPassword, request);
        if (result.containsKey("user")) {
            model.addAttribute("user", result.get("user"));
            return "redirect:/home";
        } else {
            model.addAttribute("accountMsg", result.get("accountMsg"));
            model.addAttribute("passwordMsg", result.get("passwordMsg"));
            model.addAttribute("user", userLoginRequest);
            return "/site/login";
        }

    }

    /**
     * 生成验证码
     *
     * @return
     */

    @GetMapping(path = "/kaptcha")
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        session.setAttribute("kaptcha", text);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            log.error("响应验证码失败:" + e.getMessage());
        }
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public String userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return "redirect:/user/login";
    }


    @LoginCheck
    @GetMapping("/setting")
    public String getSettingPage(HttpServletRequest request) {
        return "/site/setting";
    }

    /**
     * 修改用户头像
     *
     * @return
     */
    @LoginCheck
    @PostMapping("/upload")
    public String editUserAvatar(@RequestPart("file") MultipartFile multipartFile, HttpServletRequest request, Model model) {
        if (multipartFile == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }
        String result = FileUtils.validFile(multipartFile, FileUploadBizEnum.USER_AVATAR);
        if (result != null) {
            model.addAttribute("error", result);
            return "/site/setting";
        }
        User loginUser = userService.getLoginUser(request);
        // 生成随机文件名
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("/%s/%s", FileUploadBizEnum.USER_AVATAR.getValue(), filename);
        filepath = baseFilePath + filepath;
        // 确定文件存放的路径
        File dest = new File(filepath);
        try {
            // 存储文件
            multipartFile.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }
        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8101/community/user/header/xxx.png

        String userAvatar = COS_HOST + contextPath + "/user/header/" + filename;
        loginUser.setUserAvatar(userAvatar);
        userService.updateById(loginUser);
        return "redirect:/home";
    }

    @GetMapping("/header/{imgName}")
    public void getAvatar(@PathVariable String imgName, HttpServletRequest request, HttpServletResponse response) {
        // 读取后缀
        String suffix = imgName.substring(imgName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        String fileName = baseFilePath + "//" + FileUploadBizEnum.USER_AVATAR.getValue() + "//" + imgName;
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取头像失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @ResponseBody
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String defaultPassword = "12345678";
        String SALT = "";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @ResponseBody
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @ResponseBody
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @ResponseBody
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
