package com.druvu.web.php.internal;

import com.druvu.php.PhpEngine;
import com.druvu.php.PhpEngineConfig;
import com.druvu.php.PhpProcessingException;
import com.druvu.php.TemplateLoader;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    private transient PhpEngine engine;

    public PhpServlet() {
        this(PhpEngineConfig.DEFAULTS);
    }

    public PhpServlet(PhpEngineConfig config) {
        this.config = config;
    }

    @Override
    public void init() {
        engine = new PhpEngine(TemplateLoader.classpath("webapp"), config);
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
        Optional<String> page;
        try {
            page = engine.render(path, RequestModel.from(request), new ServletHostRequest(request));
        } catch (PhpProcessingException failed) {
            // The detail is for whoever maintains the template, not for whoever requested the page.
            LOG.error("Rendering {} failed", path, failed);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The page could not be rendered");
            return;
        }
        if (page.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("text/html");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(page.get());
        }
    }
}
