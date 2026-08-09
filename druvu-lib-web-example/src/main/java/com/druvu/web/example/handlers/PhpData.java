package com.druvu.web.example.handlers;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import java.util.List;

/**
 * Your own objects, read by the template through {@code ->}.
 *
 * <p>A record's components, then {@code getX()}, then {@code isX()} — read-only, all the way down. A template can look
 * at an object graph and can do nothing else to it: no setter, no method call, no way to reach past what it is shown.
 *
 * @author Deniss Larka
 */
public class PhpData implements HttpHandler {

    /** Public because the template reads it by reflection across a module boundary. */
    public record Order(String reference, Customer customer, List<Line> lines, Address shipTo) {

        /** Defensive copy: a record component that hands out a caller's list would leak it in both directions. */
        public Order {
            lines = List.copyOf(lines);
        }

        public double total() {
            return lines.stream().mapToDouble(Line::amount).sum();
        }
    }

    public record Customer(String name, boolean preferred) {}

    public record Line(String description, int quantity, double amount) {}

    public record Address(String city, String country) {}

    /** The other shape a host might pass: an ordinary bean, read through its getters. */
    public static final class Invoice {

        public String getNumber() {
            return "INV-2026-0042";
        }

        public boolean isPaid() {
            return false;
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        request.setAttribute("title", "Data from the application");
        request.setAttribute(
                "order",
                new Order(
                        "ORD-7781",
                        new Customer("Ada Lovelace", true),
                        List.of(
                                new Line("Fountain pen", 3, 144.00),
                                new Line("Ink bottle", 7, 87.50),
                                new Line("Notebook", 12, 28.80)),
                        new Address("Geneva", "Switzerland")));

        // No ship-to address at all, to show what ?-> does instead of failing.
        request.setAttribute(
                "backorder",
                new Order(
                        "ORD-7782",
                        new Customer("Grace Hopper", false),
                        List.of(new Line("Envelope pack", 25, 20.00)),
                        null));

        request.setAttribute("invoice", new Invoice());
    }
}
