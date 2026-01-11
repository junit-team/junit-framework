/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithFourConstants.BAR;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithFourConstants.BAZ;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithFourConstants.FOO;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithFourConstants.QUX;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * @since 5.0
 */
class EnumArgumentsProviderTests {

	final ParameterDeclarations parameters = mock();
	final ExtensionContext extensionContext = mock();

	@Test
	void providesAllEnumConstants() {
		var arguments = provideArguments(EnumWithFourConstants.class);

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR }, new Object[] { BAZ },
			new Object[] { QUX });
	}

	@Test
	void provideSingleEnumConstant() {
		var arguments = provideArguments(EnumWithFourConstants.class, "FOO");

		assertThat(arguments).containsExactly(new Object[] { FOO });
	}

	@Test
	void provideAllEnumConstantsWithNamingAll() {
		var arguments = provideArguments(EnumWithFourConstants.class, "FOO", "BAR");

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR });
	}

	@Test
	void duplicateConstantNameIsDetected() {
		assertPreconditionViolationFor(
			() -> provideArguments(EnumWithFourConstants.class, "FOO", "BAR", "FOO").findAny())//
					.withMessageContaining("Duplicate enum constant name(s) found");
	}

	@Test
	void invalidConstantNameIsDetected() {
		assertPreconditionViolationFor(() -> provideArguments(EnumWithFourConstants.class, "FO0", "B4R").findAny())//
				.withMessageContaining("Invalid enum constant name(s) in");
	}

	@Test
	void invalidPatternIsDetected() {
		assertPreconditionViolationFor(
			() -> provideArguments(EnumWithFourConstants.class, Mode.MATCH_ALL, "(", ")").findAny())//
					.withMessageContaining("Pattern compilation failed");
	}

	@Test
	void providesEnumConstantsBasedOnTestMethod() {
		org.junit.jupiter.params.support.ParameterDeclaration firstParameterDeclaration = mock();
		when(firstParameterDeclaration.getParameterType()).thenAnswer(__ -> EnumWithFourConstants.class);
		when(parameters.getFirst()).thenReturn(Optional.of(firstParameterDeclaration));

		var arguments = provideArguments(NullEnum.class);

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR }, new Object[] { BAZ },
			new Object[] { QUX });
	}

	@Test
	void incorrectParameterTypeIsDetected() {
		ParameterDeclaration firstParameterDeclaration = mock();
		when(firstParameterDeclaration.getParameterType()).thenAnswer(__ -> Object.class);
		when(parameters.getFirst()).thenReturn(Optional.of(firstParameterDeclaration));

		assertPreconditionViolationFor(() -> provideArguments(NullEnum.class).findAny())//
				.withMessageStartingWith("First parameter must reference an Enum type");
	}

	@Test
	void methodsWithoutParametersAreDetected() {
		when(parameters.getSourceElementDescription()).thenReturn("method");

		assertPreconditionViolationFor(() -> provideArguments(NullEnum.class).findAny())//
				.withMessageStartingWith("There must be at least one declared parameter for method");
	}

	@Test
	void providesEnumConstantsStartingFromBar() {
		var arguments = provideArguments(EnumWithFourConstants.class, "BAR", "", Mode.INCLUDE);

		assertThat(arguments).containsExactly(new Object[] { BAR }, new Object[] { BAZ }, new Object[] { QUX });
	}

	@Test
	void providesEnumConstantsEndingAtBaz() {
		var arguments = provideArguments(EnumWithFourConstants.class, "", "BAZ", Mode.INCLUDE);

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR }, new Object[] { BAZ });
	}

	@Test
	void providesEnumConstantsFromBarToBaz() {
		var arguments = provideArguments(EnumWithFourConstants.class, "BAR", "BAZ", Mode.INCLUDE);

		assertThat(arguments).containsExactly(new Object[] { BAR }, new Object[] { BAZ });
	}

	@Test
	void providesEnumConstantsFromFooToBazWhileExcludingBar() {
		var arguments = provideArguments(EnumWithFourConstants.class, "FOO", "BAZ", Mode.EXCLUDE, "BAR");

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAZ });
	}

	@Test
	void providesNoEnumConstant() {
		var arguments = provideArguments(EnumWithNoConstant.class);

		assertThat(arguments).isEmpty();
	}

	@Test
	void invalidConstantNameIsDetectedInRange() {
		assertPreconditionViolationFor(
			() -> provideArguments(EnumWithFourConstants.class, "FOO", "BAZ", Mode.EXCLUDE, "QUX").findAny())//
					.withMessageContaining("Invalid enum constant name(s) in");
	}

	@Test
	void invalidStartingRangeIsDetected() {
		assertThatIllegalArgumentException()//
				.isThrownBy(() -> provideArguments(EnumWithFourConstants.class, "B4R", "", Mode.INCLUDE).findAny())//
				.withMessageContaining("No enum constant");
	}

	@Test
	void invalidEndingRangeIsDetected() {
		assertThatIllegalArgumentException()//
				.isThrownBy(() -> provideArguments(EnumWithFourConstants.class, "", "B4R", Mode.INCLUDE).findAny())//
				.withMessageContaining("No enum constant");
	}

	@Test
	void invalidRangeOrderIsDetected() {
		assertPreconditionViolationFor(
			() -> provideArguments(EnumWithFourConstants.class, "BAR", "FOO", Mode.INCLUDE).findAny())//
					.withMessageContaining("Invalid enum range");
	}

	@Test
	void invalidRangeIsDetectedWhenEnumWithNoConstantIsProvided() {
		assertPreconditionViolationFor(
			() -> provideArguments(EnumWithNoConstant.class, "BAR", "FOO", Mode.INCLUDE).findAny())//
					.withMessageContaining("No enum constant");
	}

	static class TestCase {
		void methodWithoutParameters() {
		}
	}

	enum EnumWithFourConstants {
		FOO, BAR, BAZ, QUX
	}

	enum EnumWithNoConstant {
	}

	private <E extends Enum<E>> Stream<Object[]> provideArguments(Class<E> enumClass, String... names) {
		return provideArguments(enumClass, Mode.INCLUDE, names);
	}

	private <E extends Enum<E>> Stream<Object[]> provideArguments(Class<E> enumClass, Mode mode, String... names) {
		return provideArguments(enumClass, "", "", mode, names);
	}

	private <E extends Enum<E>> Stream<Object[]> provideArguments(Class<E> enumClass, String from, String to, Mode mode,
			String... names) {
		var annotation = mock(EnumSource.class);
		when(annotation.value()).thenAnswer(__ -> enumClass);
		when(annotation.from()).thenReturn(from);
		when(annotation.to()).thenReturn(to);
		when(annotation.mode()).thenReturn(mode);
		when(annotation.names()).thenReturn(names);
		when(annotation.toString()).thenReturn(
			"@EnumSource(value=%s.class, from=%s, to=%s, mode=%s, names=%s)".formatted(enumClass.getSimpleName(), from,
				to, mode, Arrays.toString(names)));

		var provider = new EnumArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(parameters, extensionContext).map(Arguments::get);
	}

}
