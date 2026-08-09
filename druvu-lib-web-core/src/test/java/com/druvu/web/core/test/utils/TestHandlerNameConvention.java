package com.druvu.web.core.test.utils;

import com.druvu.web.api.utils.HandlerNameConvention;
import org.testng.Assert;
import org.testng.annotations.Test;

/** @author Deniss Larka on 08 Sep 2022 */
public class TestHandlerNameConvention {

    @Test
    public void testApply() {
        HandlerNameConvention convention = new HandlerNameConvention();
        Assert.assertEquals(convention.apply(SomeClassHandler.class), "some-class");
        Assert.assertEquals(convention.apply(SomeClass.class), "some-class");
    }

    static class SomeClassHandler {}

    static class SomeClass {}
}
