/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.DEFAULT_DISPLAY_NAME;
import static org.junit.jupiter.params.ParameterizedInvocationConstants.DISPLAY_NAME_PLACEHOLDER;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.params.provider.Arguments;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 3, time = 2)
public class ParameterizedInvocationNameFormatterBenchmarks {

	@Param({ "1", "2", "4", "10", "100", "1000" })
	private int numberOfParameters;

	List<? extends Arguments> argumentsList;

	@Setup
	public void setUp() {
		argumentsList = IntStream.range(0, numberOfParameters) //
				.mapToObj(i -> Arguments.argumentSet(String.valueOf(i), i)) //
				.toList();
	}

	@Benchmark
	public void formatTestNames(Blackhole blackhole) throws Exception {
		var method = TestCase.class.getDeclaredMethod("parameterizedTest", int.class);
		var formatter = new ParameterizedInvocationNameFormatter(
			DISPLAY_NAME_PLACEHOLDER + " " + DEFAULT_DISPLAY_NAME + " ({0})", "displayName",
			new ParameterizedTestContext(TestCase.class, method,
				requireNonNull(method.getAnnotation(ParameterizedTest.class))),
			512);
		for (int i = 0; i < argumentsList.size(); i++) {
			Arguments arguments = argumentsList.get(i);
			blackhole.consume(formatter.format(i, EvaluatedArgumentSet.allOf(arguments), false));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase {
		@SuppressWarnings("unused")
		@ParameterizedTest
		void parameterizedTest(int param) {
		}
	}
}
