package com.druvu.web.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * API tokens the way they should be made and kept: 256 random bits shown to the user once, and only a hash stored.
 *
 * <pre>{@code
 * String token = ApiTokens.generate();          // shown once, never stored
 * repository.save(subject, ApiTokens.hash(token));
 * TokenStore store = presented -> repository.subjectOf(ApiTokens.hash(presented));
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public final class ApiTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private ApiTokens() {}

    /** A fresh token: 256 random bits, URL-safe, no padding. */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The SHA-256 of a token as lower-case hex, which is what belongs in a database. */
    public static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", impossible);
        }
    }
}
