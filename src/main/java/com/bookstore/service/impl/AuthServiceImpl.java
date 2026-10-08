package com.bookstore.service.impl;


import com.bookstore.domain. Role;
import com.bookstore.domain.User;
import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth. LoginRequest;
import com.bookstore.dto.auth. RegisterRequest;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtTokenProvider;
import com.bookstore.security.UserPrincipal;
import com.bookstore.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework. transaction.annotation. Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }

            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email already registered: " + request.email());
            }

            User user = User.builder()
                    .username(request.username())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .role(Role.USER)
                    .build();
            User saved = userRepository.save(user);
            log.info("USER_REGISTERED userId={} username={}", saved.getId(), saved.getUsername());

            String token = tokenProvider.generateToken(UserPrincipal.from(saved));
            return AuthResponse.bearer(token, saved.getUsername(), saved.getRole().name());
        }


        @Override
        public AuthResponse login(LoginRequest request){
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = tokenProvider.generateToken(principal);
            log.info("USER_LOGIN userId={} username={}", principal.getId(), principal.getUsername());
            return AuthResponse.bearer(token, principal.getUsername(), principal.getRole());
        }
    }
