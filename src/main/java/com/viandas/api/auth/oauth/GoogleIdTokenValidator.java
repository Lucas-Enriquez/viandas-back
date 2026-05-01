package com.viandas.api.auth.oauth;

public interface GoogleIdTokenValidator {
	GoogleProfile validate(String idToken);
}
