package com.druvu.web.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Ends the session and sends the browser on: {@code request.logout()} is the servlet API's own word for it, and the
 * destination is context-relative.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final String afterLogout;

    public LogoutServlet(String afterLogout) {
        this.afterLogout = afterLogout;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.logout();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + afterLogout);
    }
}
