package com.viandas.api.auth.oauth;

import java.util.UUID;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
	Optional<OAuthAccount> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
