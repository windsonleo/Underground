package com.underground.shared;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/styles.css", "/account.css", "/app.js", "/account.js").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/accounts/me", "/api/profiles/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/profiles/*").authenticated()
                .anyRequest().denyAll())
            .formLogin(login -> login.loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .successHandler((request, response, auth) -> response.setStatus(204))
                .failureHandler((request, response, exception) -> response.setStatus(401)).permitAll())
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> response.setStatus(401))
                .accessDeniedHandler((request, response, ex) -> response.setStatus(403)))
            .requestCache(cache -> cache.disable())
            .build();
    }
}
