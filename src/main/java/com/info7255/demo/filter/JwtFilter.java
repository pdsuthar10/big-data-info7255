package com.info7255.demo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.info7255.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private ObjectMapper mapper;
    private final JwtUtil jwtUtil;


    public JwtFilter(ObjectMapper mapper, JwtUtil jwtUtil) {
        this.mapper = mapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = httpServletRequest.getHeader("Authorization");
        Map<String, Object> errorDetails = new HashMap<>();

        if(authorizationHeader == null){
            errorDetails.put("message", "Token missing!");
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            mapper.writeValue(httpServletResponse.getWriter(), errorDetails);
            return;
        }
        String token = authorizationHeader.substring(7);
        boolean isValid;
        try {
            isValid = jwtUtil.validateToken(token);
        } catch (Exception e) {
            System.out.println(e);
            isValid = false;
        }

        if ( !isValid ) {
            errorDetails.put("message", "Invalid Token!");
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            mapper.writeValue(httpServletResponse.getWriter(), errorDetails);
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
