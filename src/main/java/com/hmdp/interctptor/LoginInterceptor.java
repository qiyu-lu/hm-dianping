package com.hmdp.interctptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从 Session 获取用户
        UserDTO user = (UserDTO) request.getSession().getAttribute("user");
        if (user == null) {
            response.setStatus(401);
            return false;
        }

        // 保存到 ThreadLocal，后续可以获取
        UserHolder.saveUser(user);
        // 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal
        UserHolder.removeUser();
    }
}
