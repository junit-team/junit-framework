/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import static org.junit.runner.Description.createTestDescription;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * @since 4.12
 */
public class RunnerWithCustomUniqueIdsAndDisplayNames extends BlockJUnit4ClassRunner {

	public RunnerWithCustomUniqueIdsAndDisplayNames(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected String getName() {
		return getLabel(getTestClass(), super::getName);
	}

	@Override
	protected Description describeChild(FrameworkMethod method) {
		var testName = testName(method);
		return createTestDescription(getTestClass().getJavaClass().getName(), testName, new CustomUniqueId(testName));
	}

	@Override
	protected String testName(FrameworkMethod method) {
		return getLabel(method, () -> super.testName(method));
	}

	private String getLabel(Annotatable element, Supplier<String> fallback) {
		var label = element.getAnnotation(Label.class);
		return label == null ? fallback.get() : label.value();
	}

	private record CustomUniqueId(String testName) implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CustomUniqueId(String name)) {
				return Objects.equals(this.testName, name);
			}
			return false;
		}

	}
}
