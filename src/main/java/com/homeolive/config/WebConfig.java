package com.homeolive.config;

import com.homeolive.acceso.AccesoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccesoInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/logout", "/error",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico");
    }
}
