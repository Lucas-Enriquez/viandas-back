package com.viandas.api.auth;

public interface GoogleIdTokenValidator {
	GoogleProfile validate(String idToken);
}
