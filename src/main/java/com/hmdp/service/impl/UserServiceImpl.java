package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import javax.xml.ws.Action;

import java.time.LocalDateTime;

import static com.hmdp.utils.RegexUtils.isPhoneInvalid;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private HttpSession session;

    @Override
    public Result sendCode(String phone, HttpSession session){
        // 1 号码校验
        if(isPhoneInvalid(phone)){
            return Result.fail("号码不合法");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        //存入session
        session.setAttribute("code", code);
        //打印或者发送短信
        log.debug("验证码是：" + code);
        return Result.ok();
    }
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session){
        // 号码校验
        String phone = loginForm.getPhone();
        if(isPhoneInvalid(phone)){
            return Result.fail("号码不合法");
        }
        // 取出验证码 用户输入的验证吗
        String rawCode = loginForm.getCode();
        //和之前发送的验证码进行比较
        if(rawCode ==null || !rawCode.equals(session.getAttribute("code"))){
            return Result.fail("验证码不正确");
        }
        //根据号码查询用户，如果存在返回用户，不存在新建用户
        User user = lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
        if(user == null){
            user = generateUserWithphone(phone);
            save(user);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);//复制不隐私的信息，不过我认为这里应该使用vo
        session.setAttribute("user", userDTO);
        return Result.ok(userDTO);
    }

    private User generateUserWithphone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    @Override
    public Result me(){
        return Result.ok(UserHolder.getUser());
    }
}
