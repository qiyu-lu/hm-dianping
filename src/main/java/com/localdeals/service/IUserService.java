package com.localdeals.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.localdeals.dto.LoginFormDTO;
import com.localdeals.dto.Result;
import com.localdeals.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    public Result sendCode(String phone, HttpSession session);
    public Result login(LoginFormDTO loginForm, HttpSession session);
    public Result me();

    Result logout(String token);

    Result sign();

    Result signCount();
}
