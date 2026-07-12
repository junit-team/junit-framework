/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.junit.platform.testkit.engine.EventConditions.container;

import org.assertj.core.api.Condition;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.EventConditions;

class SuiteEventConditions {

	private SuiteEventConditions() {
		/* no-op */
	}

	static Condition<Event> suite(Class<?> clazz) {
		return container(EventConditions.uniqueId(suiteId(clazz)));
	}

	private static Condition<UniqueId> suiteId(Class<?> clazz) {
		return new Condition<>(uniqueId -> {
			var last = uniqueId.getSegments().getLast();
			return last.getType().equals(SuiteTestDescriptor.SEGMENT_TYPE) && last.getValue().equals(clazz.getName());
		}, "last segment equals '%s:%s'", SuiteTestDescriptor.SEGMENT_TYPE, clazz.getName());
	}
}
