package com.druvu.web.api.utils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Deniss Larka
 * <br/>on 11 Sep 2022
 */
public class BeanNameConvention implements Function<Object, Set<String>> {

	@Override
	public Set<String> apply(Object bean) {
		Objects.requireNonNull(bean);
		Set<String> keys = new HashSet<>();
		keys.add(keyFromClass(bean.getClass()));
		final Class<?>[] interfaces = bean.getClass().getInterfaces();
		for (Class<?> anInterface : interfaces) {
			keys.add(keyFromClass(anInterface));
		}
		return keys;
	}

	public static <C> String keyFromClass(Class<C> aClass) {
		return aClass.getName();
	}

}
