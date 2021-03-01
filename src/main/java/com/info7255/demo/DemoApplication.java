package com.info7255.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.info7255.demo.filter.JwtFilter;
import com.info7255.demo.util.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistrationBean(JwtUtil jwtUtil){
        FilterRegistrationBean<JwtFilter> registrationBean = new FilterRegistrationBean<>();

        ObjectMapper mapper = new ObjectMapper();
        registrationBean.setFilter(new JwtFilter(mapper, jwtUtil));
        registrationBean.setUrlPatterns(Arrays.asList(new String[]{"/plan","/plan/*"}));

        return registrationBean;
    }

}
