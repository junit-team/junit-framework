/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ClassUtils;

/**
 * @since 1.1
 */
class ClassSupportTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void nullSafeToStringPreconditions() {
		Function<? super Class<?>, ? extends String> mapper = null;
		assertPreconditionViolationNotNullFor("Mapping function",
			() -> ClassSupport.nullSafeToString(mapper, String.class, List.class));
	}

	@Test
	void nullSafeToStringDelegates() {
		assertEquals(ClassUtils.nullSafeToString(String.class, List.class),
			ClassSupport.nullSafeToString(String.class, List.class));

		Function<Class<?>, String> classToStringMapper = aClass -> aClass.getSimpleName() + "-Test";
		assertEquals(ClassUtils.nullSafeToString(classToStringMapper, String.class, List.class),
			ClassSupport.nullSafeToString(classToStringMapper, String.class, List.class));
	}

}
