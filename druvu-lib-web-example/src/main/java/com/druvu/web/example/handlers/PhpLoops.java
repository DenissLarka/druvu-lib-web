package com.druvu.web.example.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Arrays and every loop form, including the colon syntax a layout is actually written in.
 *
 * <p>A Java {@code List} arrives as a PHP list and a {@code Map} as a keyed array, so a template iterates them with
 * plain {@code foreach} and never learns where they came from.
 *
 * @author Deniss Larka
 */
public class PhpLoops implements HttpHandler {

    /** Public because the template reads it by reflection across a module boundary. */
    public record Product(String name, String category, int quantity, double unitPrice) {

        public double total() {
            return quantity * unitPrice;
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("title", "Loops and arrays");
        request.setAttribute(
                "products",
                List.of(
                        new Product("Notebook", "paper", 12, 2.40),
                        new Product("Fountain pen", "writing", 3, 48.00),
                        new Product("Ink bottle", "writing", 7, 12.50),
                        new Product("Envelope pack", "paper", 25, 0.80)));
        request.setAttribute("stockByWarehouse", Map.of("Geneva", 128, "Zurich", 74, "Basel", 39));
    }
}
