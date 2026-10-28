package com.inchef.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.inchef.exception.BadRequestException;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleAuthService(@Value("${inchef.google.client-id:}") String clientId) {
        this.clientId = (clientId != null) ? clientId.trim() : "";
        if (!this.clientId.isEmpty()) {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(this.clientId))
                    .build();
        } else {
            this.verifier = null;
        }
    }

    /**
     * Constructor for testing with mock or custom GoogleIdTokenVerifier.
     */
    public GoogleAuthService(String clientId, GoogleIdTokenVerifier verifier) {
        this.clientId = (clientId != null) ? clientId.trim() : "";
        this.verifier = verifier;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean isConfigured() {
        return this.clientId != null && !this.clientId.isEmpty();
    }

    /**
     * Validates the Google ID token.
     * Verifies signature, issuer, audience against configured Client ID, expiration, and email verified status.
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        if (!isConfigured() || this.verifier == null) {
            throw new IllegalStateException("Google Client ID is not configured. Please set GOOGLE_CLIENT_ID environment variable or inchef.google.client-id property.");
        }

        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new BadRequestException("Google ID token is required");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString.trim());
            if (idToken == null) {
                log.warn("Google ID token verification failed (returned null - invalid signature, audience, or expired)");
                throw new BadCredentialsException("Invalid, expired, or unauthorized Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify audience against configured Client ID
            boolean audienceMatches = (payload.getAudienceAsList() != null && payload.getAudienceAsList().contains(this.clientId))
                    || this.clientId.equals(payload.getAudience());
            if (!audienceMatches) {
                log.warn("Google token audience mismatch: expected {}, got {}", this.clientId, payload.getAudience());
                throw new BadCredentialsException("Google ID token audience mismatch");
            }

            // Verify email verified status
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Google email is unverified for subject: {}", payload.getSubject());
                throw new BadCredentialsException("Google email address is not verified");
            }

            return payload;
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            log.warn("Error occurred while verifying Google ID token: {}", e.getMessage());
            throw new BadCredentialsException("Failed to verify Google ID token: Invalid or malformed token");
        }
    }
}
