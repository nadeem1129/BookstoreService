package com.bookstore.config;

import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import com.bookstore.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner seedDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder){
        return args -> {
            String username = "demo";
            if(userRepository.existsByUsername(username)){
                return;
            }
            User demo = User.builder().username(username).email("demo@bookstore.local").password(passwordEncoder.encode("password"))
                    .role(Role.USER).build();
            userRepository.save(demo);
            log.info("Seeded demo user: username='{}' password='password'", username);
        };
    }


}
