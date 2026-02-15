package com.druvu.web.core.test.auth;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.InMemoryUserStore;
import com.druvu.web.api.auth.UserStore;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests for authentication and authorization
 *
 * @author : Deniss Larka
 */
public class TestAuthentication {

	@Test
	public void testAuthConfigBuilder() {
		AuthConfig config = AuthConfig.builder()
				.basicAuth()
				.realm("Test Realm")
				.sessionTimeout(600)
				.user("user", "pass", "generic:permission")
				.user("admin", "secret", "generic:permission", "admin:permission")
				.build();

		assertEquals(config.authType(), "BASIC");
		assertEquals(config.realmName(), "Test Realm");
		assertTrue(config.sessionRenewal());
		assertEquals(config.sessionTimeoutSeconds(), 600);
		assertNotNull(config.userStore());
	}

	@Test
	public void testAuthConfigDefaults() {
		AuthConfig config = AuthConfig.builder()
				.user("user", "pass", "generic:permission")
				.build();

		assertEquals(config.authType(), "BASIC");
		assertEquals(config.realmName(), "Application Access");
		assertTrue(config.sessionRenewal());
		assertEquals(config.sessionTimeoutSeconds(), 1800);
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void testAuthConfigBuilderRequiresUsers() {
		AuthConfig.builder().build();
	}

	@Test
	public void testInMemoryUserStorePermissions() {
		AuthConfig config = AuthConfig.builder()
				.user("user", "pass", "generic:permission")
				.user("admin", "secret", "generic:permission", "admin:permission")
				.build();

		UserStore store = config.userStore();

		Set<String> userPerms = store.permissions("user");
		assertNotNull(userPerms);
		assertEquals(userPerms.size(), 1);
		assertTrue(userPerms.contains("generic:permission"));

		Set<String> adminPerms = store.permissions("admin");
		assertNotNull(adminPerms);
		assertEquals(adminPerms.size(), 2);
		assertTrue(adminPerms.contains("generic:permission"));
		assertTrue(adminPerms.contains("admin:permission"));
	}

	@Test
	public void testInMemoryUserStoreUnknownUser() {
		AuthConfig config = AuthConfig.builder()
				.user("user", "pass", "generic:permission")
				.build();

		Set<String> permissions = config.userStore().permissions("unknown");
		assertNotNull(permissions);
		assertTrue(permissions.isEmpty());
	}

	@Test
	public void testCustomUserStore() {
		UserStore customStore = principalName -> switch (principalName) {
			case "alice" -> Set.of("read:data", "write:data");
			default -> Set.of();
		};

		AuthConfig config = AuthConfig.builder()
				.userStore(customStore)
				.build();

		assertEquals(config.userStore().permissions("alice"), Set.of("read:data", "write:data"));
		assertTrue(config.userStore().permissions("unknown").isEmpty());
	}

	@Test
	public void testInMemoryUserStoreCredentials() {
		AuthConfig config = AuthConfig.builder()
				.user("user", "pass", "generic:permission")
				.build();

		assertTrue(config.userStore() instanceof InMemoryUserStore);
		InMemoryUserStore inMemory = (InMemoryUserStore) config.userStore();
		assertEquals(inMemory.users().size(), 1);
		assertEquals(inMemory.users().get("user").password(), "pass");
	}

	@Test
	public void testCompletePermissionFlow() {
		AuthConfig config = AuthConfig.builder()
				.user("user", "pass", "generic:permission")
				.user("admin", "secret", "generic:permission", "admin:permission")
				.build();

		// User has required permission
		Set<String> userPerms = config.userStore().permissions("user");
		Set<String> requiredPerms = Set.of("generic:permission");
		assertTrue(userPerms.containsAll(requiredPerms));

		// Admin has both permissions
		Set<String> adminPerms = config.userStore().permissions("admin");
		assertTrue(adminPerms.containsAll(Set.of("generic:permission", "admin:permission")));

		// User does NOT have admin permission
		assertFalse(userPerms.containsAll(Set.of("admin:permission")));
	}
}
