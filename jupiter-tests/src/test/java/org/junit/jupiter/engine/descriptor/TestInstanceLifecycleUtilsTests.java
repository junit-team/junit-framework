/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;
import static org.junit.jupiter.engine.descriptor.TestInstanceLifecycleUtils.getTestInstanceLifecycle;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * Unit tests for {@link TestInstanceLifecycleUtils}.
 *
 * <p>NOTE: it doesn't make sense to unit test the JVM system property fallback
 * support in this test class since that feature is a concrete implementation
 * detail of {@code LauncherConfigurationParameters} which necessitates an
 * integration test via the {@code Launcher} API.
 *
 * @since 5.0
 */
class TestInstanceLifecycleUtilsTests {

	private static final String KEY = DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;

	@SuppressWarnings("DataFlowIssue")
	@Test
	void getTestInstanceLifecyclePreconditions() {
		assertPreconditionViolationNotNullFor("testClass", () -> getTestInstanceLifecycle(null,
			new DefaultJupiterConfiguration(mock(), dummyOutputDirectoryCreator(), mock())));

		assertPreconditionViolationNotNullFor("configuration", () -> getTestInstanceLifecycle(getClass(), null));
	}

	@Test
	void getTestInstanceLifecycleWithNoConfigParamSet() {
		Lifecycle lifecycle = getTestInstanceLifecycle(getClass(),
			new DefaultJupiterConfiguration(mock(), dummyOutputDirectoryCreator(), mock()));
		assertThat(lifecycle).isEqualTo(PER_METHOD);
	}

	@Test
	void getTestInstanceLifecycleWithConfigParamSet() {
		ConfigurationParameters configParams = mock();
		when(configParams.get(KEY)).thenReturn(Optional.of(PER_CLASS.name().toLowerCase()));
		Lifecycle lifecycle = getTestInstanceLifecycle(getClass(),
			new DefaultJupiterConfiguration(configParams, dummyOutputDirectoryCreator(), mock()));
		assertThat(lifecycle).isEqualTo(PER_CLASS);
	}

	@Test
	void getTestInstanceLifecycleWithLocalConfigThatOverridesCustomDefaultSetViaConfigParam() {
		ConfigurationParameters configParams = mock();
		when(configParams.get(KEY)).thenReturn(Optional.of(PER_CLASS.name().toLowerCase()));
		Lifecycle lifecycle = getTestInstanceLifecycle(TestCase.class,
			new DefaultJupiterConfiguration(configParams, dummyOutputDirectoryCreator(), mock()));
		assertThat(lifecycle).isEqualTo(PER_METHOD);
	}

	@Test
	void getTestInstanceLifecycleFromMetaAnnotationWithNoConfigParamSet() {
		Class<?> testClass = BaseMetaAnnotatedTestCase.class;
		Lifecycle lifecycle = getTestInstanceLifecycle(testClass,
			new DefaultJupiterConfiguration(mock(), dummyOutputDirectoryCreator(), mock()));
		assertThat(lifecycle).isEqualTo(PER_CLASS);
	}

	@Test
	void getTestInstanceLifecycleFromSpecializedClassWithNoConfigParamSet() {
		Class<?> testClass = SpecializedTestCase.class;
		Lifecycle lifecycle = getTestInstanceLifecycle(testClass,
			new DefaultJupiterConfiguration(mock(), dummyOutputDirectoryCreator(), mock()));
		assertThat(lifecycle).isEqualTo(PER_CLASS);
	}

	@TestInstance(Lifecycle.PER_METHOD)
	private static class TestCase {
	}

	@Inherited
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@TestInstance(Lifecycle.PER_CLASS)
	private @interface PerClassLifeCycle {
	}

	@PerClassLifeCycle
	private static class BaseMetaAnnotatedTestCase {
	}

	private static class SpecializedTestCase extends BaseMetaAnnotatedTestCase {
	}

}
