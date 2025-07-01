package com.thinkerscave.common.usrm.security;

import com.thinkerscave.common.usrm.filter.JwtAuthFilter;
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
	
    /**
     * Configures the UserDetailsService bean used for fetching user details
     * from the database. This service is used by Spring Security for authentication.
     * 
     * @return a UserDetailsService implementation to load user-specific data
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // Use UserUserInfoDetailsService to load user details from the database
        return new UserUserInfoDetailsService();
    }

    /**
     * Configures HTTP security for the application.
     * 
     * @param http the HttpSecurity object used to configure security settings
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs while configuring HTTP security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/users/**",
                                "/login",
                                "/api/schema/**",
                                "/api/organizations/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/home/**").authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }



    /**
     * Configures the PasswordEncoder bean used to encode passwords.
     * BCryptPasswordEncoder is a strong password encoder.
     * 
     * @return the configured PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // BCrypt is a strong hashing function for passwords
    }

    /**
     * Exposes the AuthenticationManager as a Spring bean. This allows the
     * AuthenticationManager to be injected and used manually in other parts
     * of the application, such as controllers or service classes.
     * 
     * @param authenticationConfiguration the AuthenticationConfiguration provided by Spring Security
     * @return the configured AuthenticationManager
     * @throws Exception if an error occurs while obtaining the AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures an AuthenticationProvider bean for authentication.
     * DaoAuthenticationProvider is used to authenticate users with a database.
     * 
     * @return the configured AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());  // Set the UserDetailsService for loading user data
        authenticationProvider.setPasswordEncoder(passwordEncoder());  // Set the PasswordEncoder for encoding passwords
        return authenticationProvider;
    }
}
