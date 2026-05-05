package com.viandas.api.auth.application;

import java.time.Clock;
import java.time.Instant;

import com.viandas.api.auth.dto.request.BootstrapCookRequest;
import com.viandas.api.auth.dto.request.GoogleLoginRequest;
import com.viandas.api.auth.dto.request.LoginRequest;
import com.viandas.api.auth.dto.request.RefreshTokenRequest;
import com.viandas.api.auth.dto.response.AuthResponse;
import com.viandas.api.auth.dto.response.AuthUserResponse;
import com.viandas.api.auth.oauth.GoogleIdTokenValidator;
import com.viandas.api.auth.oauth.GoogleProfile;
import com.viandas.api.auth.oauth.OAuthAccount;
import com.viandas.api.auth.oauth.OAuthAccountRepository;
import com.viandas.api.auth.oauth.OAuthProvider;
import com.viandas.api.auth.security.JwtService;
import com.viandas.api.auth.token.RefreshToken;
import com.viandas.api.auth.token.RefreshTokenRepository;
import com.viandas.api.shared.TokenHasher;
import com.viandas.api.shared.helpers.Texts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.shared.ApiException;
import com.viandas.api.user.domain.User;
import com.viandas.api.user.persistence.UserRepository;
import com.viandas.api.user.domain.UserRole;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenValidator googleIdTokenValidator;
    private final TokenHasher tokenHasher;
    private final Clock clock;
    private final String bootstrapKey;
    private final long refreshExpirationDays;

    public AuthService(
            UserRepository userRepository,
            OAuthAccountRepository oAuthAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenValidator googleIdTokenValidator,
            TokenHasher tokenHasher,
            Clock clock,
            @Value("${viandas.bootstrap.key}") String bootstrapKey,
            @Value("${viandas.refresh.expiration-days}") long refreshExpirationDays
    ) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenValidator = googleIdTokenValidator;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
        this.bootstrapKey = bootstrapKey;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Transactional
    public AuthResponse bootstrapCook(BootstrapCookRequest request) {
        if (!bootstrapKey.equals(request.bootstrapKey())) {
            throw ApiException.forbidden("Invalid bootstrap key");
        }
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email already exists");
        }

        User user = new User(Texts.trim(request.name()), email, passwordEncoder.encode(request.password()), UserRole.COOK);

        User cook = userRepository.save(user);

        return authResponse(cook);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            throw ApiException.forbidden("Customers must use Google Login");
        }
        return authResponse(user);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleProfile profile = googleIdTokenValidator.validate(request.idToken());
        OAuthAccount account = oAuthAccountRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, profile.subject())
                .orElse(null);
        User user;
        if (account != null) {
            user = account.getUser();
            user.setName(profile.name());
            user.setEmail(profile.email());
            user.setUpdatedAt(Instant.now());
            account.setProviderEmail(profile.email());
            account.setUpdatedAt(Instant.now());
        } else {
            user = userRepository.findByEmailIgnoreCase(profile.email()).orElse(null);
            if (user != null && user.getRole() != UserRole.CUSTOMER) {
                throw ApiException.conflict("Email is already used by another account type");
            }
            if (user == null) {
                user = userRepository.save(new User(profile.name(), profile.email(), null, UserRole.CUSTOMER));
            }
            oAuthAccountRepository.save(new OAuthAccount(user, OAuthProvider.GOOGLE, profile.subject(), profile.email()));
        }
        return authResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        String tokenHash = tokenHasher.hash(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        Instant now = Instant.now(clock);

        if (!storedToken.isActive(now)) {
            throw ApiException.unauthorized("Invalid refresh token");
        }

        User user = storedToken.getUser();
        if (!user.isEnabled()) {
            storedToken.setRevokedAt(now);
            throw ApiException.unauthorized("Invalid refresh token");
        }

        String newRefreshToken = tokenHasher.newToken();
        String newRefreshTokenHash = tokenHasher.hash(newRefreshToken);
        RefreshToken replacement = new RefreshToken(
                user,
                newRefreshTokenHash,
                now,
                now.plusSeconds(refreshExpirationDays * 24 * 60 * 60));

        storedToken.setRevokedAt(now);
        storedToken.setReplacedByTokenHash(newRefreshTokenHash);
        refreshTokenRepository.save(replacement);

        return authResponse(user, newRefreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = tokenHasher.hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshToken -> {
            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(Instant.now(clock));
            }
        });
    }

    public AuthResponse createSession(User user) {
        return authResponse(user);
    }

    private AuthResponse authResponse(User user) {
        String refreshToken = tokenHasher.newToken();
        String refreshTokenHash = tokenHasher.hash(refreshToken);
        Instant now = Instant.now(clock);
        refreshTokenRepository.save(new RefreshToken(
                user,
                refreshTokenHash,
                now,
                now.plusSeconds(refreshExpirationDays * 24 * 60 * 60)));

        return authResponse(user, refreshToken);
    }

    private AuthResponse authResponse(User user, String refreshToken) {
        String accessToken = jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
