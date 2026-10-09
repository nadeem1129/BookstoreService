package com.bookstore.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtTokenProvider;
import com.bookstore.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplLoggingTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(AuthServiceImpl.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final AuthServiceImpl authService = new AuthServiceImpl(
            userRepository,
            passwordEncoder,
            tokenProvider,
            authenticationManager);

    @BeforeEach
    void attachLogAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsSuccessfulRegistrationWithoutPasswordOrToken() {
        User user = User.builder()
                .id(20L)
                .username("reader")
                .email("reader@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("encoded-password");
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("sensitive-token");

        authService.register(RegisterRequest.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password("plaintext-password")
                .build());

        String message = findLog("USER_REGISTERED");
        assertTrue(message.contains("userId=20"));
        assertTrue(message.contains("username=reader"));
        assertFalse(message.contains("plaintext-password"));
        assertFalse(message.contains("encoded-password"));
        assertFalse(message.contains("sensitive-token"));
    }

    @Test
    void logsSuccessfulLoginWithoutPasswordOrToken() {
        UserPrincipal principal = new UserPrincipal(21L, "reader", "encoded-password", "USER");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(tokenProvider.generateToken(principal)).thenReturn("sensitive-token");

        authService.login(LoginRequest.builder()
                .username("reader")
                .password("plaintext-password")
                .build());

        String message = findLog("USER_LOGIN");
        assertTrue(message.contains("userId=21"));
        assertTrue(message.contains("username=reader"));
        assertFalse(message.contains("plaintext-password"));
        assertFalse(message.contains("encoded-password"));
        assertFalse(message.contains("sensitive-token"));
    }

    private String findLog(String eventName) {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains(eventName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing log event: " + eventName));
    }
}
