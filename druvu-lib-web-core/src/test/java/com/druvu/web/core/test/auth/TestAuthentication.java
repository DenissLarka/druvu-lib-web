package com.druvu.web.core.test.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BasicAuthentication;
import java.time.Duration;
import java.util.Set;
import org.testng.annotations.Test;

/** The sign-in configuration builder: inline users become Basic authentication with an in-memory permission store. */
public class TestAuthentication {

    @Test
    public void inlineUsersBecomeBasicAuthentication() {
        AuthConfig config = AuthConfig.builder()
                .basicAuth()
                .realm("Test Realm")
                .user("user", "pass", "generic:permission")
                .user("admin", "secret", "generic:permission", "admin:permission")
                .build();
        assertThat(config.people()).isInstanceOf(BasicAuthentication.class);
        BasicAuthentication basic = (BasicAuthentication) config.people();
        assertThat(basic.realm()).isEqualTo("Test Realm");
        assertThat(basic.users()).containsOnlyKeys("user", "admin");
        assertThat(basic.users().get("user").password()).isEqualTo("pass");
        assertThat(config.permissions().permissions("admin"))
                .containsExactlyInAnyOrder("generic:permission", "admin:permission");
    }

    @Test
    public void theDefaultsAreHalfAnHourAndAPlainCookieName() {
        AuthConfig config = AuthConfig.builder().user("user", "pass").build();
        assertThat(config.sessionTimeout()).isEqualTo(Duration.ofMinutes(30));
        assertThat(config.cookieName()).isEqualTo("session");
        assertThat(((BasicAuthentication) config.people()).realm()).isEqualTo(AuthConfig.DEFAULT_REALM);
    }

    @Test
    public void anUnknownSubjectHasNoPermissions() {
        AuthConfig config =
                AuthConfig.builder().user("user", "pass", "generic:permission").build();
        assertThat(config.permissions().permissions("unknown")).isEmpty();
    }

    @Test
    public void anApplicationsOwnStoreWins() {
        AuthConfig config = AuthConfig.builder()
                .user("user", "pass", "ignored:permission")
                .permissions(subject -> Set.of("from:store"))
                .sessionTimeout(Duration.ofHours(8))
                .cookieName("shop")
                .build();
        assertThat(config.permissions().permissions("user")).containsExactly("from:store");
        assertThat(config.sessionTimeout()).isEqualTo(Duration.ofHours(8));
        assertThat(config.cookieName()).isEqualTo("shop");
    }

    @Test
    public void nothingToSignInWithIsRefused() {
        assertThatThrownBy(() -> AuthConfig.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No way to sign in");
    }
}
