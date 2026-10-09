package com.bookstore.service.impl;

import com.bookstore.domain.Role;
import com.bookstore.domain.User;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtTokenProvider;
import com.bookstore.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder =
            mock(org.springframework.security.crypto.password.PasswordEncoder.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, tokenProvider, authenticationManager);
    }

    @Test
    void registersUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = RegisterRequest.builder()
                .username("reader")
                .email("reader@example.com")
                .password("plain-password")
                .build();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        var response = authService.register(request);

        assertEquals("Bearer", response.tokenType());
        assertEquals("reader", response.username());
        assertEquals("USER", response.role());
        assertEquals("jwt-token", response.token());
        verify(passwordEncoder).encode("plain-password");
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getRole() == Role.USER && "encoded-password".equals(user.getPassword())));
    }

    @Test
    void rejectsExistingUsernameBeforeEmailLookupOrSave() {
        RegisterRequest request = RegisterRequest.builder()
                .username("reader")
                .email("reader@example.com")
                .password("plain-password")
                .build();
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void rejectsExistingEmailWithoutSaving() {
        RegisterRequest request = RegisterRequest.builder()
                .username("reader")
                .email("reader@example.com")
                .password("plain-password")
                .build();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginReturnsBearerTokenForAuthenticatedPrincipal() {
        UserPrincipal principal = new UserPrincipal(43L, "reader", "encoded-password", "USER");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
        when(tokenProvider.generateToken(principal)).thenReturn("jwt-token");

        var response = authService.login(LoginRequest.builder()
                .username("reader")
                .password("plain-password")
                .build());

        assertEquals("Bearer", response.tokenType());
        assertEquals("reader", response.username());
        assertEquals("USER", response.role());
        assertEquals("jwt-token", response.token());
    }

    @Test
    void loginPropagatesInvalidCredentialsWithoutCreatingToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(
                LoginRequest.builder().username("reader").password("wrong").build()));

        verify(tokenProvider, never()).generateToken(any(UserPrincipal.class));
    }
}
