package com.druvu.web.api.utils;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates class name to agreed path name
 *
 * @author Deniss Larka
 * <br/>on 08 Sep 2022
 */
public class HandlerNameConvention implements Function<Class<?>, String> {

	private final static Pattern RX_DASHER = Pattern.compile("(?!^)(?=[A-Z][a-z])");
	private final static Pattern RX_TAIL_CUTTER = Pattern.compile("Handler");
	public static final String DASH = "-";
	public static final HandlerNameConvention INSTANCE = new HandlerNameConvention();

	@Override
	public String apply(Class<?> aClass) {
		final Matcher matcher = RX_DASHER.matcher(preFilter(aClass.getSimpleName()));
		return matcher.replaceAll(DASH).toLowerCase(Locale.ENGLISH);
	}

	private String preFilter(String className) {
		return RX_TAIL_CUTTER.matcher(className).replaceAll("");
	}

	public static String translate(Class<?> aClass) {
		return INSTANCE.apply(aClass);
	}
}
