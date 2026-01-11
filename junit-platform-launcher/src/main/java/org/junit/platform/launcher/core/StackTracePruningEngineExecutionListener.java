/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * Prunes the stack trace in case of a failed event.
 *
 * @since 1.10
 * @see org.junit.platform.commons.util.ExceptionUtils#pruneStackTrace(Throwable, List)
 */
class StackTracePruningEngineExecutionListener extends DelegatingEngineExecutionListener {

	StackTracePruningEngineExecutionListener(EngineExecutionListener delegate) {
		super(delegate);
	}

	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		List<String> testClassNames = getTestClassNames(testDescriptor);
		if (testExecutionResult.getThrowable().isPresent()) {
			Throwable throwable = testExecutionResult.getThrowable().get();

			ExceptionUtils.findNestedThrowables(throwable).forEach(
				t -> ExceptionUtils.pruneStackTrace(t, testClassNames));
		}
		super.executionFinished(testDescriptor, testExecutionResult);
	}

	private static List<String> getTestClassNames(TestDescriptor testDescriptor) {
		Stream<? extends TestDescriptor> self = Stream.of(testDescriptor);
		Stream<? extends TestDescriptor> ancestors = testDescriptor.getAncestors().stream();
		return Stream.concat(self, ancestors) //
				.map(TestDescriptor::getSource) //
				.flatMap(Optional::stream) //
				.map(source -> {
					if (source instanceof ClassSource classSource) {
						return classSource.getClassName();
					}
					else if (source instanceof MethodSource methodSource) {
						return methodSource.getClassName();
					}
					else {
						return null;
					}
				}) //
				.filter(Objects::nonNull) //
				.toList();
	}

}
