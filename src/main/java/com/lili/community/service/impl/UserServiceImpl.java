package com.lili.community.service.impl;

import static com.lili.community.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lili.community.common.ErrorCode;
import com.lili.community.constant.UserConstant;
import com.lili.community.manager.MailClient;
import com.lili.community.mapper.UserMapper;
import com.lili.community.model.dto.user.UserQueryRequest;
import com.lili.community.model.entity.User;
import com.lili.community.model.enums.ActiveEnum;
import com.lili.community.model.enums.UserRoleEnum;
import com.lili.community.model.vo.LoginUserVO;
import com.lili.community.model.vo.UserVO;
import com.lili.community.service.UserService;
import com.lili.community.utils.BaseUtils;
import com.lili.community.utils.SqlUtils;
import com.lili.community.constant.CommonConstant;
import com.lili.community.exception.BusinessException;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private MailClient mailClient;
    @Resource
    private TemplateEngine templateEngine;
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    @Override
    public Map<String, Object> userRegister(String userAccount, String userPassword, String checkPassword, String email, Model model) {
        Map<String, Object> resultMap = new HashMap<>();
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            resultMap.put("result", "参数为空");
            return resultMap;
        }
        if (userAccount.length() < 4) {
            resultMap.put("result", "用户账号过短");
            return resultMap;
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            resultMap.put("result", "用户密码过短");
            return resultMap;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            resultMap.put("result", "两次输入的密码不一致");
            return resultMap;
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                resultMap.put("result", "账号重复");
                return resultMap;
            }
            // 2. 加密
//            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setSalt(BaseUtils.genUUID().substring(0, 5));
            user.setUserPassword(BaseUtils.MD5(userPassword + user.getSalt()));
            user.setActivationCode(BaseUtils.genUUID());
            user.setEmail(email);
            user.setUserAvatar(String.format("https://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                resultMap.put("result", "注册失败，数据库错误");
                return resultMap;
            }
            // 激活邮件
            Context context = new Context();
            context.setVariable("email", user.getEmail());
            // http://localhost:8101/api/user/activation/101/code
            String url = domain + contextPath + "/user/activation/" + user.getId() + "/" + user.getActivationCode();
            context.setVariable("url", url);
            String content = templateEngine.process("/mail/activation", context);
            mailClient.sendMail(user.getEmail(), "激活账号", content);

            resultMap.put("result", "ok");
            return resultMap;
        }
    }

    @Override
    public int activation(Long userId, String activationCode) {
        if (StringUtils.isBlank(activationCode) || userId == null || userId < 0) {
            return ActiveEnum.FAILED.getValue();
        }
        User user = this.getById(userId);

        String userRole = user.getUserRole();
        if (!userRole.equals(UserConstant.UNACTIVATED_ROLE)) {
            return ActiveEnum.REACTIVATION.getValue();
        }
        String code = user.getActivationCode();
        if (code.equals(activationCode)) {
            user.setUserRole(UserConstant.DEFAULT_ROLE);
            boolean b = this.updateById(user);
            if (!b) {
                return ActiveEnum.ACTIVATION_CODE_ERROR.getValue();
            }
            return ActiveEnum.SUCCEED.getValue();
        }
        return ActiveEnum.ACTIVATION_CODE_ERROR.getValue();

    }

    @Override
    public Map<String, Object> userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            result.put("accountMsg", "账号不能为空");
            return result;
        }
        if (userAccount.length() < 4) {
            result.put("accountMsg", "账号过短");
            return result;
        }
        if (userPassword.length() < 8) {
            result.put("passwordMsg", "密码应为8位以上");
            return result;
        }
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        String salt = user.getSalt();
        String encryptPassword = DigestUtils.md5DigestAsHex((userPassword + salt).getBytes());
        queryWrapper.eq("userPassword", encryptPassword);
        User coorrectUser = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (coorrectUser == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            result.put("accountMsg", "用户不存在或密码错误");
            return result;
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, coorrectUser);
        result.put("user", coorrectUser);
        return result;
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            return null;
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            return false;
//            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String email = userQueryRequest.getEmail();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(email != null, "email", email);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
