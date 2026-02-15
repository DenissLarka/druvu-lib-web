package com.druvu.web.core.auth;

import java.util.Set;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.util.security.Password;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.InMemoryUserStore;

/**
 * Bridges {@link AuthConfig} (API) to Jetty's {@link Authenticator.Configuration}.
 * <p>
 * Internally maps permissions to Jetty roles (1:1) so that Jetty's
 * role-based security model works transparently with the permissions-only API.
 *
 * @author Deniss Larka
 */
public class JettyAuthAdapter implements Authenticator.Configuration {

	private final AuthConfig authConfig;
	private final LoginService loginService;
	private final IdentityService identityService;

	public JettyAuthAdapter(AuthConfig authConfig) {
		this.authConfig = authConfig;
		this.loginService = createLoginService(authConfig);
		this.identityService = new AuthIdentityService();
	}

	public AuthConfig authConfig() {
		return authConfig;
	}

	@Override
	public String getAuthenticationType() {
		return authConfig.authType();
	}

	@Override
	public String getRealmName() {
		return authConfig.realmName();
	}

	@Override
	public String getParameter(String param) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getParameterNames() {
		return Set.of();
	}

	@Override
	public LoginService getLoginService() {
		return loginService;
	}

	@Override
	public IdentityService getIdentityService() {
		return identityService;
	}

	@Override
	public boolean isSessionRenewedOnAuthentication() {
		return authConfig.sessionRenewal();
	}

	@Override
	public int getSessionMaxInactiveIntervalOnAuthentication() {
		return authConfig.sessionTimeoutSeconds();
	}

	private static LoginService createLoginService(AuthConfig config) {
		if (config.userStore() instanceof InMemoryUserStore inMemory) {
			return createHashLoginService(inMemory);
		}
		throw new UnsupportedOperationException(
				"Custom UserStore implementations require a LoginService adapter. " +
				"Override JettyAuthAdapter to provide a custom LoginService.");
	}

	private static LoginService createHashLoginService(InMemoryUserStore store) {
		final HashLoginService hashLoginService = new HashLoginService();
		final UserStore userStore = new UserStore();
		for (var entry : store.users().entrySet()) {
			// Permissions are used as Jetty roles (1:1 mapping)
			String[] permissions = entry.getValue().permissions().toArray(new String[0]);
			userStore.addUser(entry.getKey(), new Password(entry.getValue().password()), permissions);
		}
		hashLoginService.setUserStore(userStore);
		return hashLoginService;
	}
}
