package com.druvu.web.core.internal;

import jakarta.servlet.MultipartConfigElement;
import java.util.function.Consumer;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

/**
 * Registers the dispatcher on every path, with the multipart limits uploads are read under: the application's upload
 * limit for a whole body and for any one file in it.
 *
 * @author Deniss Larka on 11 February 2022
 */
public class DispatcherServletSetup implements Consumer<ServletContextHandler> {

    /** Parts up to this size stay in memory; a larger one waits in a temporary file until the request ends. */
    private static final int IN_MEMORY_PART_SIZE = 1024 * 1024;

    private final long uploadLimit;

    public DispatcherServletSetup(long uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    @Override
    public void accept(ServletContextHandler servletContext) {
        ServletHolder holder = new ServletHolder("dispatcher", DispatcherServlet.class);
        holder.setInitOrder(0);
        holder.setAsyncSupported(false);
        holder.getRegistration()
                .setMultipartConfig(new MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"), uploadLimit, uploadLimit, IN_MEMORY_PART_SIZE));
        servletContext.addServlet(holder, "/*");
    }
}
