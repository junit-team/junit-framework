/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.util.FunctionUtils.where;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;

/**
 * Unit tests for extension composability in JUnit Jupiter.
 *
 * <p>The purpose of these tests is to ensure that a concrete extension
 * (a.k.a., the kitchen sink extension) is able to implement all extension
 * APIs supported by JUnit Jupiter without any naming conflicts or
 * ambiguities with regard to method names or method signatures.
 *
 * @since 5.0
 * @see KitchenSinkExtension
 */
class ExtensionComposabilityTests {

	@Test
	void ensureJupiterExtensionApisAreComposable() {

		// 1) Find all existing top-level Extension APIs
		List<Class<?>> extensionApis = findExtensionApis();

		// 2) Determine which methods we expect the kitchen sink to implement...

		// @formatter:off
		List<Method> expectedMethods = extensionApis.stream()
				.map(Class::getDeclaredMethods)
				.flatMap(Arrays::stream)
				.filter(not(Method::isSynthetic))
				.filter(not(where(Method::getModifiers, Modifier::isStatic)))
				.toList();

		List<String> expectedMethodSignatures = expectedMethods.stream()
				.map(this::methodSignature)
				.sorted()
				.toList();

		List<String> expectedMethodNames = expectedMethods.stream()
				.map(Method::getName)
				.distinct()
				.sorted()
				.toList();
		// @formatter:on

		// 3) Dynamically implement all Extension APIs
		Object dynamicKitchenSinkExtension = Proxy.newProxyInstance(getClass().getClassLoader(),
			extensionApis.toArray(Class[]::new), (proxy, method, args) -> null);

		// 4) Determine what ended up in the kitchen sink...

		// @formatter:off
		List<Method> actualMethods = Arrays.stream(dynamicKitchenSinkExtension.getClass().getDeclaredMethods())
				.filter(ModifierSupport::isNotStatic)
				.toList();

		List<String> actualMethodSignatures = actualMethods.stream()
				.map(this::methodSignature)
				.distinct()
				.sorted()
				.collect(toCollection(ArrayList::new));

		List<String> actualMethodNames = actualMethods.stream()
				.map(Method::getName)
				.distinct()
				.sorted()
				.collect(toCollection(ArrayList::new));
		// @formatter:on

		// 5) Remove methods from java.lang.Object
		actualMethodSignatures.remove("equals(Object)");
		actualMethodSignatures.remove("hashCode()");
		actualMethodSignatures.remove("toString()");
		actualMethodNames.remove("equals");
		actualMethodNames.remove("hashCode");
		actualMethodNames.remove("toString");

		// 6) Verify our expectations

		// @formatter:off
		assertAll(
				() -> assertThat(actualMethodSignatures).isEqualTo(expectedMethodSignatures),
				() -> assertThat(actualMethodNames).isEqualTo(expectedMethodNames)
		);
		// @formatter:on
	}

	@TestFactory
	Stream<DynamicContainer> kitchenSinkExtensionImplementsAllExtensionApis() {
		var declaredMethods = List.of(KitchenSinkExtension.class.getDeclaredMethods());
		return findExtensionApis().stream() //
				.map(c -> dynamicContainer( //
					c.getSimpleName(), //
					Stream.concat( //
						Stream.of(
							dynamicTest("implements interface", () -> c.isAssignableFrom(KitchenSinkExtension.class))), //
						Arrays.stream(c.getMethods()) //
								.filter(ModifierSupport::isNotStatic).map(m -> dynamicTest( //
									"overrides " + m.getName(), //
									() -> assertTrue( //
										declaredMethods.stream().anyMatch(it -> //
										it.getName().equals(m.getName()) //
												&& it.getReturnType().equals(m.getReturnType()) //
												&& Arrays.equals(it.getParameterTypes(), m.getParameterTypes()) //
										))) //
								) //
					) //
				));
	}

	private List<Class<?>> findExtensionApis() {
		return ReflectionSupport.findAllClassesInPackage(Extension.class.getPackage().getName(), this::isExtensionApi,
			name -> true);
	}

	private boolean isExtensionApi(Class<?> candidate) {
		return candidate.isInterface() && (candidate != Extension.class) && Extension.class.isAssignableFrom(candidate);
	}

	private String methodSignature(Method method) {
		return "%s(%s)".formatted(method.getName(),
			ClassUtils.nullSafeToString(Class::getSimpleName, method.getParameterTypes()));
	}

}
