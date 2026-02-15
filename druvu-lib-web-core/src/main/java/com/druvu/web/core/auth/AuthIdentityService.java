package com.druvu.web.core.auth;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;

/**
 * @author : Deniss Larka
 * <br/>on 05 May 2024
 **/
public class AuthIdentityService extends DefaultIdentityService {

	public AuthUserIdentityImpl newUserIdentity(Subject subject, Principal userPrincipal, Set<String> permissions) {
		return new AuthUserIdentityImpl(subject, userPrincipal, permissions);
	}

}
