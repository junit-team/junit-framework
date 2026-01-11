/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static java.util.stream.Collectors.toCollection;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

/**
 * @since 1.0
 */
public class DemoClassTestDescriptor extends AbstractTestDescriptor {

	private final Class<?> testClass;

	public DemoClassTestDescriptor(UniqueId uniqueId, Class<?> testClass) {
		super(uniqueId, Preconditions.notNull(testClass, "Class must not be null").getSimpleName(),
			ClassSource.from(testClass));
		this.testClass = testClass;
	}

	@Override
	public Set<TestTag> getTags() {
		return findRepeatableAnnotations(this.testClass, Tag.class).stream() //
				.map(Tag::value) //
				.filter(TestTag::isValid) //
				.map(TestTag::create) //
				.collect(toCollection(LinkedHashSet::new));
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

}
