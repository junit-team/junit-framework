/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

// tag::user_guide[]
public class ParameterResolverCustomAnnotationDemo {

	@Test
	void testInt(@FirstInteger Integer first, @SecondInteger Integer second) {
		assertEquals(1, first);
		assertEquals(2, second);
	}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(FirstInteger.Extension.class)
	public @interface FirstInteger {

		class Extension implements ParameterResolver {

			@Override
			public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return parameterContext.getParameter().getType().equals(Integer.class)
						&& !parameterContext.isAnnotated(SecondInteger.class);
			}

			@Override
			public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return 1;
			}
		}
	}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(SecondInteger.Extension.class)
	public @interface SecondInteger {

		class Extension implements ParameterResolver {

			@Override
			public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return parameterContext.isAnnotated(SecondInteger.class);
			}

			@Override
			public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
				return 2;
			}
		}
	}
}
// end::user_guide[]
