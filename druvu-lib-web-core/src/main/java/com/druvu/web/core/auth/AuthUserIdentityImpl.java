package com.druvu.web.core.auth;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.UserIdentity;

import com.druvu.web.api.auth.AuthUserIdentity;

import lombok.NonNull;

/**
 * Implementation of {@link AuthUserIdentity} with Jetty's {@link UserIdentity}.
 *
 * @author : Deniss Larka
 * <br/>on 05 May 2024
 **/
public class AuthUserIdentityImpl implements AuthUserIdentity, UserIdentity {

	private final Subject subject;
	private final Principal userPrincipal;
	private final Set<String> permissions;

	public AuthUserIdentityImpl(@NonNull Subject subject, @NonNull Principal userPrincipal, @NonNull Set<String> permissions) {
		this.subject = subject;
		this.userPrincipal = userPrincipal;
		this.permissions = Set.copyOf(permissions);
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override
	public Set<String> getPermissions() {
		return permissions;
	}


	@Override
	public boolean isUserInRole(String role) {
		throw new IllegalStateException("Role based access is not implemented in AuthUserIdentityImpl");
	}
}
