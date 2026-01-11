/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.Ignore;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.internal.builders.IgnoredBuilder;
import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filterable;
import org.junit.runners.model.RunnerBuilder;

/**
 * Customization of {@link AllDefaultPossibilitiesBuilder} from JUnit 4 to
 * ignore certain classes that would otherwise be reported as errors or cause
 * infinite recursion.
 *
 * @since 4.12
 * @see DefensiveAnnotatedBuilder
 * @see DefensiveJUnit4Builder
 * @see IgnoredClassRunner
 */
class DefensiveAllDefaultPossibilitiesBuilder extends AllDefaultPossibilitiesBuilder {

	private static final Logger logger = LoggerFactory.getLogger(DefensiveAllDefaultPossibilitiesBuilder.class);

	private final AnnotatedBuilder annotatedBuilder;
	private final JUnit4Builder junit4Builder;
	private final IgnoredBuilder ignoredBuilder;

	@SuppressWarnings("deprecation")
	DefensiveAllDefaultPossibilitiesBuilder() {
		super(true);
		annotatedBuilder = new DefensiveAnnotatedBuilder(this);
		junit4Builder = new DefensiveJUnit4Builder();
		ignoredBuilder = new NullIgnoredBuilder();
	}

	@Override
	public Runner runnerForClass(Class<?> testClass) throws Throwable {
		Runner runner = super.runnerForClass(testClass);
		if (testClass.getAnnotation(Ignore.class) != null) {
			if (runner == null) {
				return new IgnoredClassRunner(testClass);
			}
			return decorateIgnoredTestClass(runner);
		}
		return runner;
	}

	boolean isIgnored(Runner runner) {
		return runner instanceof IgnoredClassRunner || runner instanceof IgnoringRunnerDecorator;
	}

	/**
	 * Instead of checking for the {@link Ignore} annotation and returning an
	 * {@link IgnoredClassRunner} from {@link IgnoredBuilder}, we've let the
	 * super class determine the regular runner that would have been used if
	 * {@link Ignore} hadn't been present. Now, we decorate the runner to
	 * override its runtime behavior (i.e. skip execution) but return its
	 * regular {@link org.junit.runner.Description}.
	 */
	private IgnoringRunnerDecorator decorateIgnoredTestClass(Runner runner) {
		if (runner instanceof Filterable) {
			return new FilterableIgnoringRunnerDecorator(runner);
		}
		return new IgnoringRunnerDecorator(runner);
	}

	@Override
	protected AnnotatedBuilder annotatedBuilder() {
		return annotatedBuilder;
	}

	@Override
	protected JUnit4Builder junit4Builder() {
		return junit4Builder;
	}

	@Override
	protected IgnoredBuilder ignoredBuilder() {
		return ignoredBuilder;
	}

	/**
	 * Customization of {@link AnnotatedBuilder} that ignores classes annotated
	 * with {@code @RunWith(JUnitPlatform.class)} to avoid infinite recursion.
	 */
	private static class DefensiveAnnotatedBuilder extends AnnotatedBuilder {

		DefensiveAnnotatedBuilder(RunnerBuilder suiteBuilder) {
			super(suiteBuilder);
		}

		@Override
		public @Nullable Runner buildRunner(Class<? extends Runner> runnerClass, Class<?> testClass) throws Exception {
			// Referenced by name because it might not be available at runtime.
			if ("org.junit.platform.runner.JUnitPlatform".equals(runnerClass.getName())) {
				logger.warn(() -> "Ignoring test class using JUnitPlatform runner: " + testClass.getName());
				return null;
			}
			return super.buildRunner(runnerClass, testClass);
		}
	}

	/**
	 * Customization of {@link JUnit4Builder} that ignores classes that do not
	 * contain any test methods in order not to report errors for them.
	 */
	private static class DefensiveJUnit4Builder extends JUnit4Builder {

		private static final Predicate<Method> isPotentialJUnit4TestMethod = new IsPotentialJUnit4TestMethod();

		@Override
		public @Nullable Runner runnerForClass(Class<?> testClass) throws Throwable {
			if (containsTestMethods(testClass)) {
				return super.runnerForClass(testClass);
			}
			return null;
		}

		private boolean containsTestMethods(Class<?> testClass) {
			return ReflectionUtils.isMethodPresent(testClass, isPotentialJUnit4TestMethod);
		}
	}

	/**
	 * Customization of {@link IgnoredBuilder} that always returns {@code null}.
	 *
	 * @since 5.1
	 */
	private static class NullIgnoredBuilder extends IgnoredBuilder {
		@Override
		public @Nullable Runner runnerForClass(Class<?> testClass) {
			// don't ignore entire test classes just yet
			return null;
		}
	}
}
