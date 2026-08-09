package com.druvu.web.php;

import com.druvu.web.php.internal.RequestModel;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The bridge between a handler and its template: what a handler puts on the request is what the template sees.
 *
 * <p>The interesting half is what does not cross. A servlet container writes its own bookkeeping onto the same request,
 * and none of it belongs in template scope.
 */
public class TestRequestModel {

    @Test
    public void testHandlerAttributesBecomeTemplateVariables() {
        Map<String, Object> model = RequestModel.from(requestWith(Map.of("title", "Home")));

        Assert.assertEquals(model.get("title"), "Home");
    }

    @Test
    public void testAnyValueCrossesUnchanged() {
        Object order = new Object();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("order", order);
        attributes.put("count", 3);
        attributes.put("lines", List.of("a", "b"));

        Map<String, Object> model = RequestModel.from(requestWith(attributes));

        Assert.assertSame(model.get("order"), order, "the object itself must arrive, not a copy");
        Assert.assertEquals(model.get("count"), 3);
        Assert.assertEquals(model.get("lines"), List.of("a", "b"));
    }

    /**
     * The reason this filter exists. A forward puts these on the request, and a template cannot name them anyway —
     * {@code $jakarta.servlet.forward.request_uri} does not parse.
     */
    @Test
    public void testContainerBookkeepingIsNotBound() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("jakarta.servlet.forward.request_uri", "/web-test/dashboard");
        attributes.put("jakarta.servlet.forward.context_path", "/web-test");
        attributes.put("org.eclipse.jetty.something", "internal");
        attributes.put("title", "Home");

        Map<String, Object> model = RequestModel.from(requestWith(attributes));

        Assert.assertEquals(model.keySet(), Set.of("title"));
    }

    @Test
    public void testNamesATemplateCouldNotWriteAreSkipped() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("2fast", "starts with a digit");
        attributes.put("my-var", "hyphen is not a name character");
        attributes.put("has space", "nor is a space");
        attributes.put("", "empty");

        Assert.assertTrue(RequestModel.from(requestWith(attributes)).isEmpty());
    }

    @Test
    public void testUnderscoreNamesAreLegal() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("_private", 1);
        attributes.put("user_name", "ada");
        attributes.put("page2", "second");

        Assert.assertEquals(
                RequestModel.from(requestWith(attributes)).keySet(), Set.of("_private", "user_name", "page2"));
    }

    @Test
    public void testNoRequestIsNotAFailure() {
        Assert.assertTrue(RequestModel.from(null).isEmpty());
    }

    @Test
    public void testNoAttributesGivesNoVariables() {
        Assert.assertTrue(RequestModel.from(requestWith(Map.of())).isEmpty());
    }

    /**
     * A request that answers only the two questions this class asks. {@link HttpServletRequest} has some sixty methods
     * and a hand-written stub of it would be sixty lines of noise; anything else being called here is a mistake worth
     * failing on.
     */
    private static HttpServletRequest requestWith(Map<String, Object> attributes) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                TestRequestModel.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttributeNames" -> Collections.enumeration(attributes.keySet());
                    case "getAttribute" -> attributes.get((String) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
