package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.api.auth.BasicAuthentication;
import com.druvu.web.api.auth.BasicUser;
import java.util.Map;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.util.security.Password;

/**
 * The Jetty login service behind Basic authentication: the inline users with their passwords, permissions travelling as
 * roles, which is how they reach the identity that lives in the session.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class LoginServices {

    private LoginServices() {}

    static LoginService forBasic(BasicAuthentication basic, AuthConfig auth) {
        UserStore users = new UserStore();
        for (Map.Entry<String, BasicUser> entry : basic.users().entrySet()) {
            String name = entry.getKey();
            String[] roles = auth.permissions().permissions(name).toArray(String[]::new);
            users.addUser(name, new Password(entry.getValue().password()), roles);
        }
        HashLoginService service = new HashLoginService(basic.realm());
        service.setUserStore(users);
        return service;
    }
}
