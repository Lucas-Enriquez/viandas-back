package com.viandas.api.auth.oauth;

public record GoogleProfile(String subject, String email, String name) {
}
