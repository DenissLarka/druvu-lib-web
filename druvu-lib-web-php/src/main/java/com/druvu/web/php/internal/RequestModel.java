package com.druvu.web.php.internal;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a handler put on the request, in the form a template can see.
 *
 * <p>An {@code HttpHandler} runs first and the framework then forwards to the template, so the request is the one thing
 * both halves hold. Every attribute a handler set becomes an ordinary variable: {@code setAttribute("order", o)} makes
 * {@code $order}. There is no model object to reach through, which is the whole point — a layout stays readable without
 * knowing how it is driven.
 *
 * <p>Only attributes whose name is a legal PHP variable name are taken. The servlet container puts its own bookkeeping
 * on the same request under dotted, reserved names ({@code jakarta.servlet.forward.request_uri} and its neighbours),
 * and a template could not name those anyway — {@code $jakarta.servlet.forward.request_uri} does not parse. Skipping
 * them keeps container internals out of template scope rather than binding variables nobody can reference.
 *
 * @author Deniss Larka
 */
public final class RequestModel {

    private RequestModel() {}

    /** @return every handler-set attribute a template can name, or an empty map when there is no request */
    public static Map<String, Object> from(HttpServletRequest request) {
        if (request == null) {
            return Map.of();
        }
        Enumeration<String> names = request.getAttributeNames();
        if (names == null) {
            return Map.of();
        }
        Map<String, Object> model = new LinkedHashMap<>();
        for (String name : Collections.list(names)) {
            if (isTemplateVariableName(name)) {
                model.put(name, request.getAttribute(name));
            }
        }
        return model;
    }

    /**
     * PHP's own rule for a variable name: a letter or underscore, then letters, digits or underscores. Anything else —
     * a dotted container attribute, a name starting with a digit — is not something a template could write after a
     * {@code $}.
     */
    private static boolean isTemplateVariableName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char each = name.charAt(i);
            if (!Character.isLetterOrDigit(each) && each != '_') {
                return false;
            }
        }
        return true;
    }
}
