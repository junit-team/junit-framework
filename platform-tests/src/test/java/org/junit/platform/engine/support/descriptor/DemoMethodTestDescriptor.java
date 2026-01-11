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

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

/**
 * @since 1.0
 */
public class DemoMethodTestDescriptor extends AbstractTestDescriptor {

	private final Method testMethod;

	public DemoMethodTestDescriptor(UniqueId uniqueId, Method testMethod) {
		super(uniqueId,
			"%s(%s)".formatted(Preconditions.notNull(testMethod, "Method must not be null").getName(),
				ClassUtils.nullSafeToString(Class::getSimpleName, testMethod.getParameterTypes())),
			MethodSource.from(testMethod));

		this.testMethod = testMethod;
	}

	@Override
	public Set<TestTag> getTags() {
		Set<TestTag> methodTags = findRepeatableAnnotations(this.testMethod, Tag.class).stream() //
				.map(Tag::value) //
				.filter(TestTag::isValid) //
				.map(TestTag::create) //
				.collect(toCollection(LinkedHashSet::new));

		getParent().ifPresent(parentDescriptor -> methodTags.addAll(parentDescriptor.getTags()));
		return methodTags;
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}

}
