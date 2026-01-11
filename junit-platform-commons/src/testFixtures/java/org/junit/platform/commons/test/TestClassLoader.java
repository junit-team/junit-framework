/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.test;

import java.lang.StackWalker.Option;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.commons.util.ClassLoaderUtils;

/**
 * Test {@link ClassLoader} which accepts a class name {@link Predicate} to
 * filter classes that should be loaded by this {@code ClassLoader} instead of
 * the {@linkplain ClassLoaderUtils#getDefaultClassLoader() default ClassLoader}.
 *
 * <p>This class loader is only suitable for specific testing scenarios, where
 * you need to load particular classes from a different class loader.
 *
 * @since 1.10
 */
public class TestClassLoader extends URLClassLoader {

	private static final StackWalker stackWalker = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	static {
		ClassLoader.registerAsParallelCapable();
	}

	/**
	 * Create a {@link TestClassLoader} that filters the provided classes.
	 *
	 * @see #forClasses(List)
	 * @see #forClassNamePrefix(String)
	 */
	public static TestClassLoader forClasses(Class<?>... classes) {
		Predicate<String> classNameFilter = name -> Arrays.stream(classes).map(Class::getName).anyMatch(name::equals);
		return new TestClassLoader(getCodeSourceUrl(stackWalker.getCallerClass()), classNameFilter);
	}

	/**
	 * Create a {@link TestClassLoader} that filters the provided classes.
	 *
	 * @see #forClasses(Class...)
	 * @see #forClassNamePrefix(String)
	 */
	public static TestClassLoader forClasses(List<Class<?>> classes) {
		Predicate<String> classNameFilter = name -> classes.stream().map(Class::getName).anyMatch(name::equals);
		return new TestClassLoader(getCodeSourceUrl(stackWalker.getCallerClass()), classNameFilter);
	}

	/**
	 * Create a {@link TestClassLoader} that filters classes whose fully
	 * qualified names start with the provided prefix.
	 *
	 * @see #forClasses(Class...)
	 * @see #forClasses(List)
	 */
	public static TestClassLoader forClassNamePrefix(String prefix) {
		return new TestClassLoader(getCodeSourceUrl(stackWalker.getCallerClass()), name -> name.startsWith(prefix));
	}

	private final Predicate<String> classNameFilter;

	private TestClassLoader(URL codeSourceUrl, Predicate<String> classNameFilter) {
		super(new URL[] { codeSourceUrl }, ClassLoaderUtils.getDefaultClassLoader());

		this.classNameFilter = classNameFilter;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> clazz = findLoadedClass(name);
			if (clazz != null) {
				return clazz;
			}
			return this.classNameFilter.test(name) ? findClass(name) : super.loadClass(name);
		}
	}

	/**
	 * Get the {@link CodeSource} {@link URL} of the supplied class.
	 */
	private static URL getCodeSourceUrl(Class<?> clazz) {
		return clazz.getProtectionDomain().getCodeSource().getLocation();
	}

}
