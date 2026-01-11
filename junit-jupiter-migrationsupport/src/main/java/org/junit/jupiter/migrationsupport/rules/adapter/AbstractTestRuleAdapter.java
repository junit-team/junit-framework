/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules.adapter;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.support.ReflectionSupport.findMethod;
import static org.junit.platform.commons.support.ReflectionSupport.invokeMethod;

import java.lang.reflect.Method;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMember;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.rules.TestRule;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public abstract class AbstractTestRuleAdapter implements GenericBeforeAndAfterAdvice {

	private final TestRule target;

	public AbstractTestRuleAdapter(TestRuleAnnotatedMember annotatedMember, Class<? extends TestRule> adapteeClass) {
		this.target = annotatedMember.getTestRule();
		Preconditions.condition(adapteeClass.isAssignableFrom(this.target.getClass()),
			() -> adapteeClass + " is not assignable from " + this.target.getClass());
	}

	protected @Nullable Object executeMethod(String name) {
		return executeMethod(name, new Class<?>[0]);
	}

	protected @Nullable Object executeMethod(String methodName, Class<?>[] parameterTypes, Object... arguments) {
		Method method = findMethod(this.target.getClass(), methodName, parameterTypes).orElseThrow(
			() -> new JUnitException("Failed to find method %s(%s) in class %s".formatted(methodName,
				ClassUtils.nullSafeToString(parameterTypes), this.target.getClass().getName())));

		return invokeMethod(method, this.target, arguments);
	}

}
