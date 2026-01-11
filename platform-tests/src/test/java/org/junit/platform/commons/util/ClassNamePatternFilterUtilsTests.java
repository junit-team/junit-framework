/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.classes.AExecutionConditionClass;
import org.junit.platform.commons.util.classes.ATestExecutionListenerClass;
import org.junit.platform.commons.util.classes.AVanillaEmpty;
import org.junit.platform.commons.util.classes.BExecutionConditionClass;
import org.junit.platform.commons.util.classes.BTestExecutionListenerClass;
import org.junit.platform.commons.util.classes.BVanillaEmpty;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * Unit tests for {@link ClassNamePatternFilterUtils}.
 *
 * @since 1.7
 */
@TestInstance(Lifecycle.PER_CLASS)
class ClassNamePatternFilterUtilsTests {

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AExecutionConditionClass, BExecutionConditionClass"
	})
	//@formatter:on
	@ParameterizedTest
	void neverExcludedConditions(String pattern) {
		List<? extends ExecutionCondition> executionConditions = List.of(new AExecutionConditionClass(),
			new BExecutionConditionClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AExecutionConditionClass, *BExecutionConditionClass",
			"*ExecutionConditionClass"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysExcludedConditions(String pattern) {
		List<? extends ExecutionCondition> executionConditions = List.of(new AExecutionConditionClass(),
			new BExecutionConditionClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"ATestExecutionListenerClass, BTestExecutionListenerClass"
	})
	//@formatter:on
	@ParameterizedTest
	void neverExcludedListeners(String pattern) {
		List<? extends TestExecutionListener> executionConditions = List.of(new ATestExecutionListenerClass(),
			new BTestExecutionListenerClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*ATestExecutionListenerClass, *BTestExecutionListenerClass",
			"*TestExecutionListenerClass"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysExcludedListeners(String pattern) {
		List<? extends TestExecutionListener> executionConditions = List.of(new ATestExecutionListenerClass(),
			new BTestExecutionListenerClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AVanillaEmpty, BVanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void neverExcludedClass(String pattern) {
		var executionConditions = List.of(new AVanillaEmpty(), new BVanillaEmpty());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AVanillaEmpty, *BVanillaEmpty",
			"*VanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysExcludedClass(String pattern) {
		var executionConditions = List.of(new AVanillaEmpty(), new BVanillaEmpty());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AVanillaEmpty, BVanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void neverExcludedClassName(String pattern) {
		var executionConditions = List.of("org.junit.platform.commons.util.classes.AVanillaEmpty",
			"org.junit.platform.commons.util.classes.BVanillaEmpty");
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClassNames(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AVanillaEmpty, *BVanillaEmpty",
			"*VanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysExcludedClassName(String pattern) {
		var executionConditions = List.of("org.junit.platform.commons.util.classes.AVanillaEmpty",
			"org.junit.platform.commons.util.classes.BVanillaEmpty");
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.excludeMatchingClassNames(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AExecutionConditionClass, BExecutionConditionClass"
	})
	//@formatter:on
	@ParameterizedTest
	void neverIncludedConditions(String pattern) {
		List<? extends ExecutionCondition> executionConditions = List.of(new AExecutionConditionClass(),
			new BExecutionConditionClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AExecutionConditionClass, *BExecutionConditionClass",
			"*ExecutionConditionClass"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysIncludedConditions(String pattern) {
		List<? extends ExecutionCondition> executionConditions = List.of(new AExecutionConditionClass(),
			new BExecutionConditionClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"ATestExecutionListenerClass, BTestExecutionListenerClass"
	})
	//@formatter:on
	@ParameterizedTest
	void neverIncludedListeners(String pattern) {
		List<? extends TestExecutionListener> executionConditions = List.of(new ATestExecutionListenerClass(),
			new BTestExecutionListenerClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*ATestExecutionListenerClass, *BTestExecutionListenerClass",
			"*TestExecutionListenerClass"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysIncludedListeners(String pattern) {
		List<? extends TestExecutionListener> executionConditions = List.of(new ATestExecutionListenerClass(),
			new BTestExecutionListenerClass());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AVanillaEmpty, BVanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void neverIncludedClass(String pattern) {
		var executionConditions = List.of(new AVanillaEmpty(), new BVanillaEmpty());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AVanillaEmpty, *BVanillaEmpty",
			"*VanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysIncludedClass(String pattern) {
		var executionConditions = List.of(new AVanillaEmpty(), new BVanillaEmpty());
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClasses(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.jupiter.*",
			"org.junit.platform.*.NonExistentClass",
			"*.NonExistentClass*",
			"*NonExistentClass*",
			"AVanillaEmpty, BVanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void neverIncludedClassName(String pattern) {
		var executionConditions = List.of("org.junit.platform.commons.util.classes.AVanillaEmpty",
			"org.junit.platform.commons.util.classes.BVanillaEmpty");
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClassNames(pattern)) //
				.isEmpty();
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AVanillaEmpty, *BVanillaEmpty",
			"*VanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void alwaysIncludedClassName(String pattern) {
		var executionConditions = List.of("org.junit.platform.commons.util.classes.AVanillaEmpty",
			"org.junit.platform.commons.util.classes.BVanillaEmpty");
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClassNames(pattern)) //
				.hasSize(2);
	}

	//@formatter:off
	@ValueSource(strings = {
			"org.junit.platform.*",
			"*.platform.*",
			"*",
			"*AVanillaEmpty, *BVanillaEmpty",
			"*VanillaEmpty"
	})
	//@formatter:on
	@ParameterizedTest
	void includeAndExcludeSame(String pattern) {
		var executionConditions = List.of("org.junit.platform.commons.util.classes.AVanillaEmpty",
			"org.junit.platform.commons.util.classes.BVanillaEmpty");
		assertThat(executionConditions).filteredOn(ClassNamePatternFilterUtils.includeMatchingClassNames(pattern)) //
				.hasSize(2);
	}

}
