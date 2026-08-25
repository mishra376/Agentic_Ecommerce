package com.ecom.Backend.security;

import com.ecom.Backend.repository.MerchantRepo;
import com.ecom.Backend.repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final MerchantRepo merchantRepo;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String email;
        final String role;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            email = jwtService.extractEmail(jwt);
            role = jwtService.extractRole(jwt);
        } catch (Exception e) {
            // Token is invalid or expired
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails = null;

            if ("ROLE_USER".equals(role)) {
                var userOpt = userRepo.findByEmail(email);
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    userDetails = new CustomUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(), "ROLE_USER");
                }
            } else if ("ROLE_MERCHANT".equals(role)) {
                var merchant = merchantRepo.getMerchantByEmail(email);
                if (merchant != null) {
                    userDetails = new CustomUserDetails(merchant.getId(), merchant.getEmail(), merchant.getPasswordHash(), "ROLE_MERCHANT");
                }
            }

            if (userDetails != null && jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
