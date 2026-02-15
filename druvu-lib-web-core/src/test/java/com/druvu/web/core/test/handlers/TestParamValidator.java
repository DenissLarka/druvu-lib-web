package com.druvu.web.core.test.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.druvu.web.api.handlers.ParamInfo;
import com.druvu.web.core.utils.ParamValidator;

/**
 * @author Deniss Larka
 * on 09 Sep 2022
 */
public class TestParamValidator {

	@Test
	public void testValidateExist1() {
		Map<String, String[]> parameterMap = Map.of("param1", new String[] {"value1"});
		ParamInfo paramInfo = new ParamInfo(parameterMap);
		List<String> messages = new ArrayList<>();
		final ParamValidator validator = ParamValidator.from(paramInfo, messages::add);
		validator.validateExist("param1");
		validator.validateExist("param2");
		Assert.assertEquals(messages.size(), 1);
		Assert.assertEquals(messages.get(0), "Parameter param2 must be provided");
	}

	@Test
	public void testValidateCustom() {
	}

	@Test
	public void testValidateDate() {
		Map<String, String[]> parameterMap = Map.of("param1", new String[] {"2022-08-08"});
		ParamInfo paramInfo = new ParamInfo(parameterMap);
		List<String> messages = new ArrayList<>();
		final ParamValidator validator = ParamValidator.from(paramInfo, messages::add);
		validator.validateDate("param1", "YYYY-MM-DD");
		Assert.assertEquals(messages.size(), 0);
	}

	@Test
	public void testValidateInt() {
	}
}
