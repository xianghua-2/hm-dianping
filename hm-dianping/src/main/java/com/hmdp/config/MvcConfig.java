package com.hmdp.config;

import com.hmdp.properties.JwtProperties;
import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
//Configuration注解表明该类是一个配置类，它允许你通过Java代码来配置Spring应用程序，而不是使用XML文件。Spring容器在启动时会扫描并加载这些配置类。
public class MvcConfig implements WebMvcConfigurer { //WebMvcConfigurer接口允许自定义Spring MVC的配置。通过实现这个接口，可以添加拦截器、视图控制器、视图解析器等。

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtProperties jwtProperties;


    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                ).order(1);

        // order越小，优先级越高，所以是先会通过token刷新拦截器
        // token刷新的拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate,jwtProperties)).addPathPatterns("/**").order(0); //拦截所有请求
    }
}