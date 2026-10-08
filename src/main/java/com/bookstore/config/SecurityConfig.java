package com.bookstore.config;

import com.bookstore.security.JwtAuthenticationFilter;
import com.bookstore.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final String allowedorigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          @Value("${app.cors.allowed-origins}") String allowedorigins) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.allowedorigins = allowedorigins;
    }

        @Bean
        public SecurityFilterChain filterChain (HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/ ** ").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/books/ ** ").permitAll()
                            .requestMatchers("/h2-console/ ** ").permitAll()
                            .anyRequest().authenticated())
// Allow H2 console frames during local development.
                    .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                    .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                            UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }

            @Bean
            public CorsConfigurationSource corsConfigurationSource () {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(allowedorigins.split(",")));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/ ** ", config);
                return source;
            }
            @Bean
            public PasswordEncoder passwordEncoder () {
                return new BCryptPasswordEncoder();
            }

            @Bean
            public AuthenticationManager authenticationManager (AuthenticationConfiguration configuration) throws Exception
            {
                return configuration.getAuthenticationManager();
            }
    }
