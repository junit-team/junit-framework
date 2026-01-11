/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * Unit tests for {@link ParameterResolutionUtils}.
 *
 * @since 5.9
 */
class ParameterResolutionUtilsTests {

	private static final String ENIGMA = "enigma";

	private final MethodSource instance = mock();

	private @Nullable Method method;

	private final ExtensionContext extensionContext = mock();

	private final JupiterConfiguration configuration = mock();

	private final MutableExtensionRegistry extensionRegistry = MutableExtensionRegistry.createRegistryWithDefaultExtensions(
		configuration);

	@Test
	void resolveConstructorArguments() {
		register(new StringParameterResolver());

		Class<ConstructorInjectionTestCase> topLevelClass = ConstructorInjectionTestCase.class;
		@Nullable
		Object[] arguments = resolveConstructorParameters(topLevelClass, null);

		assertThat(arguments).containsExactly(ENIGMA);
	}

	@Test
	void resolveNestedConstructorArguments() {
		register(new NumberParameterResolver());

		Class<ConstructorInjectionTestCase> outerClass = ConstructorInjectionTestCase.class;
		ConstructorInjectionTestCase outer = ReflectionSupport.newInstance(outerClass, "str");

		Class<ConstructorInjectionTestCase.NestedTestCase> innerClass = ConstructorInjectionTestCase.NestedTestCase.class;
		@Nullable
		Object[] arguments = resolveConstructorParameters(innerClass, outer);

		assertThat(arguments).containsExactly(outer, 42);
	}

	@Test
	void resolveConstructorArgumentsWithMissingResolver() {
		Constructor<ConstructorInjectionTestCase> constructor = ReflectionUtils.getDeclaredConstructor(
			ConstructorInjectionTestCase.class);

		Exception exception = assertThrows(ParameterResolutionException.class,
			() -> ParameterResolutionUtils.resolveParameters(constructor, Optional.empty(), Optional.empty(),
				extensionContext, extensionRegistry));

		assertThat(exception.getMessage())//
				.contains("No ParameterResolver registered for parameter [java.lang.String")//
				.contains("in constructor")//
				.contains(ConstructorInjectionTestCase.class.getName());
	}

	@Test
	void resolvingArgumentsForMethodsWithoutParameterDoesNotDependOnParameterResolvers() {
		testMethodWithNoParameters();
		throwDuringParameterResolution(new RuntimeException("boom!"));

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).isEmpty();
	}

	@Test
	void resolveArgumentsViaParameterResolver() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("argument");

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).containsExactly("argument");
	}

	@Test
	void resolveMultipleArguments() {
		testMethodWith("multipleParameters", String.class, Integer.class, Double.class);
		register(ConfigurableParameterResolver.supportsAndResolvesTo(parameterContext -> {
			return switch (parameterContext.getIndex()) {
				case 0 -> "0";
				case 1 -> 1;
				default -> 2.0;
			};
		}));

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).containsExactly("0", 1, 2.0);
	}

	@Test
	void onlyConsiderParameterResolversThatSupportAParticularParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatDoesNotSupportThisParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("something");

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).containsExactly("something");
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void passContextInformationToParameterResolverMethods() {
		anyTestMethodWithAtLeastOneParameter();
		ArgumentRecordingParameterResolver extension = new ArgumentRecordingParameterResolver();
		register(extension);

		resolveMethodParameters();

		assertSame(extensionContext, extension.supportsArguments.extensionContext);
		assertEquals(0, extension.supportsArguments.parameterContext.getIndex());
		assertSame(instance, extension.supportsArguments.parameterContext.getTarget().orElseThrow());
		assertSame(extensionContext, extension.resolveArguments.extensionContext);
		assertEquals(0, extension.resolveArguments.parameterContext.getIndex());
		assertSame(instance, extension.resolveArguments.parameterContext.getTarget().orElseThrow());
		assertThat(extension.resolveArguments.parameterContext.toString())//
				.contains("parameter", String.class.getTypeName(), "index", "0", "target", "Mock");
	}

	@Test
	void resolvingArgumentsForMethodsWithPrimitiveTypesIsSupported() {
		testMethodWithASinglePrimitiveIntParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(42);

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).containsExactly(42);
	}

	@Test
	void nullIsAViableArgumentIfAReferenceTypeParameterIsExpected() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(null);

		@Nullable
		Object[] arguments = resolveMethodParameters();

		assertThat(arguments).hasSize(1);
		assertNull(arguments[0]);
	}

	@Test
	void reportThatNullIsNotAViableArgumentIfAPrimitiveTypeIsExpected() {
		testMethodWithASinglePrimitiveIntParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(null);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		// @formatter:off
		assertThat(caught.getMessage())
				.contains("in method")
				.contains("resolved a null value for parameter [int")
				.contains("but a primitive of type [int] is required.");
		// @formatter:on
	}

	@Test
	void reportIfThereIsNoParameterResolverThatSupportsTheParameter() {
		testMethodWithASingleStringParameter();

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		assertThat(caught.getMessage()).contains("parameter [java.lang.String").contains("in method");
	}

	@Test
	void reportIfThereAreMultipleParameterResolversThatSupportTheParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("one");
		thereIsAParameterResolverThatResolvesTheParameterTo("two");

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		String className = Pattern.quote(ConfigurableParameterResolver.class.getName());

		// @formatter:off
		assertThat(caught.getMessage())
				.matches("Discovered multiple competing ParameterResolvers for parameter \\[java.lang.String .+?\\] " +
						"in method .+: " + className + "@.+, " + className + "@.+");
		// @formatter:on
	}

	@Test
	void reportTypeMismatchBetweenParameterAndResolvedParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(BigDecimal.ONE);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		// @formatter:off
		assertThat(caught.getMessage())
				.contains("resolved a value of type [java.math.BigDecimal] for parameter [java.lang.String")
				.contains("in method")
				.contains("but a value assignment compatible with [java.lang.String] is required.");
		// @formatter:on
	}

	@Test
	void reportTypeMismatchBetweenParameterAndResolvedParameterWithArrayTypes() {
		testMethodWithASingleStringArrayParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(new int[][] {});

		assertThatExceptionOfType(ParameterResolutionException.class)//
				.isThrownBy(this::resolveMethodParameters)//
				.withMessageContaining(//
					"resolved a value of type [int[][]] for parameter [java.lang.String[]", //
					"in method", //
					"but a value assignment compatible with [java.lang.String[]] is required." //
				);
	}

	@Test
	void wrapAllExceptionsThrownDuringParameterResolutionIntoAParameterResolutionException() {
		anyTestMethodWithAtLeastOneParameter();
		IllegalArgumentException cause = anyExceptionButParameterResolutionException();
		throwDuringParameterResolution(cause);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		assertSame(cause, caught.getCause(), () -> "cause should be present");
		assertThat(caught.getMessage())//
				.matches("^Failed to resolve parameter \\[java.lang.String .+?\\] in method \\[.+?\\]$");
	}

	@Test
	void exceptionMessageContainsMessageFromExceptionThrownDuringParameterResolution() {
		anyTestMethodWithAtLeastOneParameter();
		RuntimeException cause = new RuntimeException("boom!");
		throwDuringParameterResolution(cause);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		assertSame(cause, caught.getCause(), () -> "cause should be present");
		assertThat(caught.getMessage())//
				.matches("^Failed to resolve parameter \\[java.lang.String .+?\\] in method \\[.+?\\]: boom!$");
	}

	@Test
	void doNotWrapThrownExceptionIfItIsAlreadyAParameterResolutionException() {
		anyTestMethodWithAtLeastOneParameter();
		ParameterResolutionException cause = new ParameterResolutionException("custom message");
		throwDuringParameterResolution(cause);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class,
			this::resolveMethodParameters);

		assertSame(cause, caught);
	}

	private IllegalArgumentException anyExceptionButParameterResolutionException() {
		return new IllegalArgumentException();
	}

	private void throwDuringParameterResolution(RuntimeException parameterResolutionException) {
		register(ConfigurableParameterResolver.onAnyCallThrow(parameterResolutionException));
	}

	private void thereIsAParameterResolverThatResolvesTheParameterTo(@Nullable Object argument) {
		register(ConfigurableParameterResolver.supportsAndResolvesTo(parameterContext -> argument));
	}

	private void thereIsAParameterResolverThatDoesNotSupportThisParameter() {
		register(ConfigurableParameterResolver.withoutSupport());
	}

	private void anyTestMethodWithAtLeastOneParameter() {
		testMethodWithASingleStringParameter();
	}

	private void testMethodWithNoParameters() {
		testMethodWith("noParameter");
	}

	private void testMethodWithASingleStringParameter() {
		testMethodWith("singleStringParameter", String.class);
	}

	private void testMethodWithASingleStringArrayParameter() {
		testMethodWith("singleStringArrayParameter", String[].class);
	}

	private void testMethodWithASinglePrimitiveIntParameter() {
		testMethodWith("primitiveParameterInt", int.class);
	}

	private void testMethodWith(String methodName, Class<?>... parameterTypes) {
		this.method = ReflectionSupport.findMethod(this.instance.getClass(), methodName, parameterTypes).get();
	}

	private void register(ParameterResolver... resolvers) {
		for (ParameterResolver resolver : resolvers) {
			extensionRegistry.registerExtension(resolver, this);
		}
	}

	private <T> @Nullable Object[] resolveConstructorParameters(Class<T> clazz, @Nullable Object outerInstance) {
		Constructor<T> constructor = ReflectionUtils.getDeclaredConstructor(clazz);
		return ParameterResolutionUtils.resolveParameters(constructor, Optional.empty(),
			Optional.ofNullable(outerInstance), extensionContext, extensionRegistry);
	}

	private @Nullable Object[] resolveMethodParameters() {
		return ParameterResolutionUtils.resolveParameters(requireNonNull(this.method), Optional.of(this.instance),
			this.extensionContext, this.extensionRegistry);
	}

	// -------------------------------------------------------------------------

	static class ArgumentRecordingParameterResolver implements ParameterResolver {

		ArgumentRecordingParameterResolver.@Nullable Arguments supportsArguments;
		ArgumentRecordingParameterResolver.@Nullable Arguments resolveArguments;

		record Arguments(ParameterContext parameterContext, ExtensionContext extensionContext) {
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			supportsArguments = new ArgumentRecordingParameterResolver.Arguments(parameterContext, extensionContext);
			return true;
		}

		@Override
		public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			resolveArguments = new ArgumentRecordingParameterResolver.Arguments(parameterContext, extensionContext);
			return null;
		}
	}

	static class ConfigurableParameterResolver implements ParameterResolver {

		static ParameterResolver onAnyCallThrow(RuntimeException runtimeException) {
			return new ConfigurableParameterResolver(parameterContext -> {
				throw runtimeException;
			}, parameterContext -> {
				throw runtimeException;
			});
		}

		static ParameterResolver supportsAndResolvesTo(Function<ParameterContext, @Nullable Object> resolve) {
			return new ConfigurableParameterResolver(parameterContext -> true, resolve);
		}

		static ParameterResolver withoutSupport() {
			return new ConfigurableParameterResolver(parameterContext -> false, parameter -> {
				throw new UnsupportedOperationException();
			});
		}

		private final Predicate<ParameterContext> supports;
		private final Function<ParameterContext, @Nullable Object> resolve;

		private ConfigurableParameterResolver(Predicate<ParameterContext> supports,
				Function<ParameterContext, @Nullable Object> resolve) {
			this.supports = supports;
			this.resolve = resolve;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return supports.test(parameterContext);
		}

		@Override
		public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return resolve.apply(parameterContext);
		}
	}

	@SuppressWarnings("unused")
	interface MethodSource {

		void noParameter();

		void singleStringParameter(String parameter);

		void singleStringArrayParameter(String[] parameter);

		void primitiveParameterInt(int parameter);

		void multipleParameters(String first, Integer second, Double third);
	}

	static class StringParameterResolver implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType() == String.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return ENIGMA;
		}
	}

	static class NumberParameterResolver implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType() == Number.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return 42;
		}
	}

	static class ConstructorInjectionTestCase {

		final String str;

		@SuppressWarnings("unused")
		ConstructorInjectionTestCase(String str) {
			this.str = str;
		}

		class NestedTestCase {

			final Number num;

			@SuppressWarnings("unused")
			NestedTestCase(Number num) {
				this.num = num;
			}
		}
	}

}
