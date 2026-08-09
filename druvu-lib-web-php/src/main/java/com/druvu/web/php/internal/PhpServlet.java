package com.druvu.web.php.internal;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves a template over HTTP.
 *
 * <p>Two things it takes care to do. The content type, including its charset, is set before anything is written, since
 * a container that has already begun a response will not accept it afterwards. And when a template fails, the browser
 * is told only that something went wrong: the message, which names files and line numbers and sometimes the shape of
 * the data, goes to the log. Handing a stack trace to whoever asked for the page is how a template engine becomes a
 * reconnaissance tool.
 *
 * @author Deniss Larka
 */
public class PhpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PhpServlet.class);

    private final PhpEngineConfig config;
    private final boolean cacheTemplates;

    /** Built in {@link #init()}, which is what a container calls however the servlet instance came about. */
    private transient PhpEngine engine;

    /** The default policy: output escaped, templates parsed once. */
    public PhpServlet() {
        this(PhpEngineConfig.DEFAULTS, true);
    }

    public PhpServlet(PhpEngineConfig config, boolean cacheTemplates) {
        this.config = config;
        this.cacheTemplates = cacheTemplates;
    }

    @Override
    public void init() {
        engine = new PhpEngine(PhpServlet::loadTemplate, config, cacheTemplates);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        serve(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        serve(request, response);
    }

    private void serve(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String page;
        try {
            page = engine.render(path, request, Map.of());
        } catch (PhpProcessingException failed) {
            // The detail is for whoever maintains the template, not for whoever requested the page.
            LOG.error("Rendering {} failed", path, failed);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The page could not be rendered");
            return;
        }

        if (page == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("text/html");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(page);
        }
    }

    /**
     * A template's text, from the webapp resources.
     *
     * <p>The classpath fallback is for the executable-jar case: when this library runs inside one, webapp templates
     * live under {@code webapp/} on the classpath, where the container's own resource lookup does not resolve the
     * nested entry.
     */
    private static String loadTemplate(String path) throws IOException {
        String resource = "webapp" + (path.startsWith("/") ? path : "/" + path);
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            return null;
        }
        try (InputStream open = stream) {
            return new String(open.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
