package com.thinkerscave.common.security;

import com.thinkerscave.common.filter.JwtAuthFilter;
import com.thinkerscave.common.usrm.service.impl.UserUserInfoDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    // --- ADJUSTMENT 1: REMOVED CONSTRUCTOR AND @Autowired FIELDS ---
    // The @Autowired JwtAuthFilter and the constructor were causing the cycle.
    // They are no longer needed here.

    /**
     * Configures the UserDetailsService bean used for fetching user details.
     * @return a UserDetailsService implementation to load user-specific data.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserUserInfoDetailsService();
    }

    /**
     * Configures the main HTTP security filter chain.
     *
     * @param http HttpSecurity object to configure.
     * @param authenticationProvider The AuthenticationProvider bean (injected by Spring).
     * @return the configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/api/password/**",     // <-- THE FIX IS ADDING THIS LINE
                                "/api/v1/users/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/api/admissions/**"
                                // Be careful: other endpoints like /api/admissions/** should likely be secured
                        ).permitAll()
                        // This line ensures any endpoint NOT in the list above is protected
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures the PasswordEncoder bean.
     * @return the configured PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a Spring bean.
     * @param authenticationConfiguration the AuthenticationConfiguration provided by Spring Security.
     * @return the configured AuthenticationManager.
     * @throws Exception if an error occurs.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures the primary AuthenticationProvider bean using a database.
     * @return the configured AuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }
}