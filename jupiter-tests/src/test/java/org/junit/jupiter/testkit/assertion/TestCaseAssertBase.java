/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.testkit.assertion.single.TestCaseAbortedAssert;
import org.junit.jupiter.testkit.assertion.single.TestCaseFailureAssert;
import org.junit.jupiter.testkit.assertion.single.TestCaseStartedAssert;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.Events;

class TestCaseAssertBase extends AbstractJUnitJupiterAssert<TestCaseAssertBase, Events>
		implements TestCaseStartedAssert, TestCaseFailureAssert, TestCaseAbortedAssert {

	TestCaseAssertBase(Events events) {
		super(events, TestCaseAssertBase.class, 1);
	}

	@Override
	public <T extends Throwable> AbstractThrowableAssert<?, T> withExceptionInstanceOf(Class<T> exceptionType) {
		Throwable thrown = getRequiredThrowable();
		return assertThat(thrown).asInstanceOf(InstanceOfAssertFactories.throwable(exceptionType));
	}

	@Override
	public AbstractThrowableAssert<?, ? extends Throwable> withException() {
		Throwable thrown = getRequiredThrowable();
		return assertThat(thrown);
	}

	@Override
	public TestCaseFailureAssert whichFailed() {
		assertThat(actual.failed().count()).isEqualTo(1);
		return new TestCaseAssertBase(actual.failed());
	}

	@Override
	public void whichSucceeded() {
		assertThat(actual.succeeded().count()).isEqualTo(1);
	}

	@Override
	public TestCaseAbortedAssert whichAborted() {
		assertThat(actual.aborted().count()).isEqualTo(1);
		return new TestCaseAssertBase(actual.aborted());
	}

	@Override
	public void withExceptionFulfilling(Predicate<Throwable> predicate) {
		Throwable thrown = getRequiredThrowable();
		assertThat(predicate).accepts(thrown);
	}

	@Override
	public void andThenCheckException(Consumer<Throwable> testFunction) {
		Throwable thrown = getRequiredThrowable();
		testFunction.accept(thrown);
	}

	private Throwable getRequiredThrowable() {
		Optional<? extends Throwable> thrown = throwable();
		assertThat(thrown).isPresent();
		return thrown.get();
	}

	private Optional<? extends Throwable> throwable() {
		return actual.stream().findFirst().flatMap(fail -> fail.getPayload(TestExecutionResult.class)).flatMap(
			TestExecutionResult::getThrowable);
	}

}
