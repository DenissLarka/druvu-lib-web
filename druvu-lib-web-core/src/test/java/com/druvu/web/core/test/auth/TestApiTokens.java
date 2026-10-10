package com.druvu.web.core.test.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.web.api.auth.ApiTokens;
import org.testng.annotations.Test;

/** Token generation is random and URL-safe; the hash is SHA-256 hex and stable. */
public class TestApiTokens {

    @Test
    public void tokensAreFreshAndUrlSafe() {
        String one = ApiTokens.generate();
        String two = ApiTokens.generate();
        assertThat(one).isNotEqualTo(two).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    public void theHashIsSha256Hex() {
        assertThat(ApiTokens.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(ApiTokens.hash("abc")).isEqualTo(ApiTokens.hash("abc"));
    }
}
