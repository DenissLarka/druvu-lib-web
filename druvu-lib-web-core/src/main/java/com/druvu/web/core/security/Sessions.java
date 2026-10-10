package com.druvu.web.core.security;

import com.druvu.web.api.auth.AuthConfig;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpCookie;

/**
 * The session handler and its cookie: HttpOnly, Secure whenever the request was, SameSite Lax, the application's name
 * and idle timeout.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
final class Sessions {

    private Sessions() {}

    static SessionHandler handler(AuthConfig auth) {
        SessionHandler sessions = new SessionHandler();
        sessions.setSessionCookie(auth.cookieName());
        sessions.setHttpOnly(true);
        sessions.setSecureRequestOnly(true);
        sessions.setSameSite(HttpCookie.SameSite.LAX);
        sessions.setMaxInactiveInterval((int) auth.sessionTimeout().toSeconds());
        return sessions;
    }
}
