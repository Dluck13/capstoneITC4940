package com.itsupportoperationsdashboard.capstoneProject.config;

import com.itsupportoperationsdashboard.capstoneProject.auth.AuthDtos.ErrorResponse;
import com.itsupportoperationsdashboard.capstoneProject.security.DatabaseUserDetailsService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    AuthenticationManager authenticationManager(DatabaseUserDetailsService users, BCryptPasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                            CsrfTokenRepository csrfTokens, ObjectMapper mapper) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
            .securityContext(context -> context.securityContextRepository(contexts))
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("END_USER", "TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/tickets", "/api/tickets/{id}")
                    .hasAnyRole("END_USER", "TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tickets")
                    .hasAnyRole("END_USER", "TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/assign", "/api/tickets/{id}/status")
                    .hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/tickets/{id}/comments")
                    .hasAnyRole("END_USER", "TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/comments")
                    .hasAnyRole("END_USER", "TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/resolve")
                    .hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/{id}/close").hasRole("ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    mapper.writeValue(response.getWriter(), new ErrorResponse(401, "Authentication required"));
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    mapper.writeValue(response.getWriter(), new ErrorResponse(403, "Access denied"));
                }))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5173"));
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
