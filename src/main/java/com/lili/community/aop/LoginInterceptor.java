package com.lili.community.aop;

import com.lili.community.annotation.LoginCheck;
import com.lili.community.model.entity.User;
import com.lili.community.service.UserService;
import org.aopalliance.intercept.Interceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @projectName: community
 * @package: com.lili.community.aop
 * @className: LoginInterceptor
 * @author: lili
 * @description: TODO
 * @date: 2024/2/17 20:25
 * @version: 1.0
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginCheck annotation = method.getAnnotation(LoginCheck.class);
            if (annotation != null && userService.getLoginUser(request) == null) {
                response.sendRedirect(request.getContextPath() + "/user/login");
                return false;
            }
        }
        return true;
    }


    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) throws Exception {
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null && modelAndView != null) {
            modelAndView.addObject("loginUser", loginUser);
        }
    }
}
