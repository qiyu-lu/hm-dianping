package com.hmdp.config;

import com.hmdp.interctptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",//不需要登录就可以访问 放行
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/voucher/**"
                );
    }
}
