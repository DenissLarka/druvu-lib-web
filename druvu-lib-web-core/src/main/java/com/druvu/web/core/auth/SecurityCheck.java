package com.druvu.web.core.auth;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.ee10.servlet.ServletApiResponse;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.util.Callback;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.AuthUserIdentity;
import com.druvu.web.core.internal.ContextVars;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Deniss Larka
 * on 18 March 2022
 */
@Slf4j
public final class SecurityCheck {

	private static final DefaultAuthenticatorFactory AUTH_FACTORY = new DefaultAuthenticatorFactory();

	private SecurityCheck() {
	}

	public static Optional<AuthUserIdentity> remoteUser(HttpServletRequest req, HttpServletResponse resp) {
		return userIdentity(req.getServletContext(), userPrincipal(req, resp));
	}

	public static Set<String> permissions(AuthUserIdentity authUserIdentity) {
		return authUserIdentity.getPermissions();
	}

	private static Optional<AuthUserIdentity> userIdentity(ServletContext context, Principal userPrincipal) {
		if (userPrincipal == null) {
			return Optional.empty();
		}
		final AuthConfig authConfig = ContextVars.authConfig(context);
		final Set<String> permissions = authConfig.userStore().permissions(userPrincipal.getName());
		final JettyAuthAdapter jettyAuth = ContextVars.jettyAuth(context);
		final AuthIdentityService identityService = (AuthIdentityService) jettyAuth.getIdentityService();
		return Optional.of(
				identityService.newUserIdentity(
						subject(userPrincipal),
						userPrincipal,
						permissions));
	}

	private static Subject subject(Principal userPrincipal) {
		return new Subject(true, Set.of(userPrincipal), Set.of(), Set.of());
	}

	private static Principal userPrincipal(HttpServletRequest req, HttpServletResponse resp) {
		if (!(req instanceof ServletApiRequest apiRequest)) {
			return null;
		}
		if (resp instanceof ServletApiResponse apiResponse) {
			final ServletContext context = req.getServletContext();
			try {
				AuthenticationState state = authenticator(context).validateRequest(
						apiRequest.getRequest(), apiResponse.getResponse(), Callback.NOOP);
				return state.getUserPrincipal();
			}
			catch (ServerAuthException e) {
				log.warn(e.getMessage(), e);
			}
			return null;
		}
		// WebSocket path: resp is not ServletApiResponse, authenticate from Authorization header directly
		return authenticateFromHeader(req);
	}

	private static Principal authenticateFromHeader(HttpServletRequest req) {
		String authHeader = req.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Basic ")) {
			return null;
		}
		try {
			String decoded = new String(
					Base64.getDecoder().decode(authHeader.substring(6).trim()),
					StandardCharsets.UTF_8);
			int colon = decoded.indexOf(':');
			if (colon < 0) {
				return null;
			}
			String username = decoded.substring(0, colon);
			String password = decoded.substring(colon + 1);

			JettyAuthAdapter jettyAuth = ContextVars.jettyAuth(req.getServletContext());
			var identity = jettyAuth.getLoginService().login(username, password, null, null);
			return identity != null ? identity.getUserPrincipal() : null;
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid Authorization header", e);
			return null;
		}
	}

	private static Authenticator authenticator(ServletContext context) {
		final JettyAuthAdapter jettyAuth = ContextVars.jettyAuth(context);
		final Authenticator authenticator = AUTH_FACTORY.getAuthenticator(null, null, jettyAuth);
		authenticator.setConfiguration(jettyAuth);
		return authenticator;
	}

}
