package com.auth_service.auth_service.Security;


import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                email = jwtUtil.getEmailFromToken(token);
            } catch (Exception e) {
                logger.error("Cannot get email from token: " + e.getMessage());
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                User user = userService.findById(userId).orElse(null);

                if (user != null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}