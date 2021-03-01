package com.info7255.demo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.info7255.demo.model.ErrorResponse;
import com.info7255.demo.util.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;
    private final JwtUtil jwtUtil;

    public JwtFilter(ObjectMapper mapper, JwtUtil jwtUtil) {
        this.mapper = mapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = httpServletRequest.getHeader("Authorization");

        if(authorizationHeader == null){
            ErrorResponse errorResponse = new ErrorResponse("Token missing", HttpStatus.UNAUTHORIZED, new Date());

            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            mapper.writeValue(httpServletResponse.getWriter(), errorResponse);
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
            ErrorResponse errorResponse = new ErrorResponse("Invalid Token!", HttpStatus.UNAUTHORIZED, new Date());

            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            mapper.writeValue(httpServletResponse.getWriter(), errorResponse);
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
