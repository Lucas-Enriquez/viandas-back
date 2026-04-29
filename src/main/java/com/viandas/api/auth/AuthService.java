package com.viandas.api.auth;

import java.time.Instant;

import com.viandas.api.auth.dto.BootstrapCookRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viandas.api.company.CompanyMembershipRepository;
import com.viandas.api.company.CompanyRepository;
import com.viandas.api.shared.ApiException;
import com.viandas.api.user.User;
import com.viandas.api.user.UserRepository;
import com.viandas.api.user.UserRole;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenValidator googleIdTokenValidator;
    private final String bootstrapKey;

    public AuthService(
            UserRepository userRepository,
            OAuthAccountRepository oAuthAccountRepository,
            CompanyMembershipRepository companyMembershipRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleIdTokenValidator googleIdTokenValidator,
            @Value("${viandas.bootstrap.key}") String bootstrapKey
    ) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdTokenValidator = googleIdTokenValidator;
        this.bootstrapKey = bootstrapKey;
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

        User cook = userRepository.save(new User(request.name()
                .trim(), email, passwordEncoder.encode(request.password()), UserRole.COOK));
        return authResponse(cook);
    }

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

    private Long resolveCompanyClaim(User user, Long requestedCompanyId) {
        if (user.getRole() == UserRole.COOK) {
            if (requestedCompanyId != null && companyRepository.findByIdAndCookId(requestedCompanyId, user.getId())
                    .isEmpty()) {
                throw ApiException.forbidden("Company does not belong to cook");
            }
            return requestedCompanyId;
        }
        if (user.getRole() == UserRole.EMPLOYEE) {
            if (requestedCompanyId != null) {
                if (!companyMembershipRepository.existsByCompanyIdAndUserId(requestedCompanyId, user.getId())) {
                    throw ApiException.forbidden("User does not belong to company");
                }
                return requestedCompanyId;
            }
            return companyMembershipRepository.findFirstByUserIdOrderById(user.getId())
                    .map(membership -> membership.getCompany().getId())
                    .orElse(null);
        }
        return null;
    }

    private AuthResponse authResponse(User user) {
        String token = jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public record LoginRequest(
            @NotBlank(message = "El mail es obligatorio")
            @Email(message = "El mail debe ser válido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {
    }

    public record GoogleLoginRequest(String idToken) {
    }

    public record AuthUserResponse(
            Long id,
            String name,
            String email,
            UserRole role
    ) {
    }

    public record AuthResponse(
            String accessToken,
            AuthUserResponse user
    ) {
    }
}
