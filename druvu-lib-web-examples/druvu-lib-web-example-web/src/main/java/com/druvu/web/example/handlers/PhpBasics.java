package com.druvu.web.example.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import java.util.List;

/**
 * Variables, expressions, conditions and escaping — the smallest useful page.
 *
 * <p>Everything this handler puts on the request arrives in {@code php-basics.php} as an ordinary variable of the same
 * name: {@code setAttribute("visitor", …)} makes {@code $visitor}.
 *
 * @author Deniss Larka
 */
public class PhpBasics implements HttpHandler {

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("title", "PHP basics");
        request.setAttribute("visitor", "ada lovelace");
        request.setAttribute("visits", 7);
        request.setAttribute("price", 1234.5);
        request.setAttribute("tags", List.of("jetty", "php", "java"));

        // Deliberately hostile, to show what escape-by-default does with it.
        request.setAttribute("untrusted", "<script>alert('xss')</script>");
    }
}
