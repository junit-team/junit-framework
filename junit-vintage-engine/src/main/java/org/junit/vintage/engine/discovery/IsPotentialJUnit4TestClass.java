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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.support.ModifierSupport.isAbstract;
import static org.junit.platform.commons.support.ModifierSupport.isPublic;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;

import java.util.function.Predicate;

import org.apiguardian.api.API;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "5.8", consumers = "org.junit.vintage.**")
public class IsPotentialJUnit4TestClass implements Predicate<Class<?>> {

	@Override
	public boolean test(Class<?> candidate) {
		// Do not collapse into a single return statement.
		if (!isPublic(candidate)) {
			return false;
		}
		if (isAbstract(candidate)) {
			return false;
		}
		if (isInnerClass(candidate)) {
			return false;
		}

		return true;
	}

}
