/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static java.util.Arrays.stream;
import static java.util.function.Predicate.isEqual;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.runner.Description;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageTestDescriptor extends AbstractTestDescriptor {

	public static final String ENGINE_ID = "junit-vintage";
	public static final String SEGMENT_TYPE_RUNNER = "runner";
	public static final String SEGMENT_TYPE_TEST = "test";
	public static final String SEGMENT_TYPE_DYNAMIC = "dynamic";

	protected Description description;

	public VintageTestDescriptor(UniqueId uniqueId, Description description, @Nullable TestSource source) {
		this(uniqueId, description, generateDisplayName(description), source);
	}

	VintageTestDescriptor(UniqueId uniqueId, Description description, String displayName, @Nullable TestSource source) {
		super(uniqueId, displayName, source);
		this.description = description;
	}

	private static String generateDisplayName(Description description) {
		String methodName = DescriptionUtils.getMethodName(description);
		return isNotBlank(methodName) ? methodName : description.getDisplayName();
	}

	public Description getDescription() {
		return description;
	}

	@Override
	public String getLegacyReportingName() {
		String methodName = DescriptionUtils.getMethodName(description);
		if (methodName == null) {
			String className = description.getClassName();
			if (isNotBlank(className)) {
				return className;
			}
		}
		return super.getLegacyReportingName();
	}

	@Override
	public Type getType() {
		return description.isTest() ? Type.TEST : Type.CONTAINER;
	}

	@Override
	public Set<TestTag> getTags() {
		Set<TestTag> tags = new LinkedHashSet<>();
		addTagsFromParent(tags);
		addCategoriesAsTags(tags);
		return tags;
	}

	@Override
	public void removeFromHierarchy() {
		if (canBeRemovedFromHierarchy()) {
			super.removeFromHierarchy();
		}
	}

	protected boolean canBeRemovedFromHierarchy() {
		return tryToExcludeFromRunner(this.description);
	}

	protected boolean tryToExcludeFromRunner(Description description) {
		// @formatter:off
		return getParent().map(VintageTestDescriptor.class::cast)
				.map(parent -> parent.tryToExcludeFromRunner(description))
				.orElse(false);
		// @formatter:on
	}

	void pruneDescriptorsForObsoleteDescriptions(List<Description> newSiblingDescriptions) {
		Optional<Description> newDescription = newSiblingDescriptions.stream().filter(isEqual(description)).findAny();
		if (newDescription.isPresent()) {
			List<Description> newChildren = newDescription.get().getChildren();
			new ArrayList<>(children).stream().map(VintageTestDescriptor.class::cast).forEach(
				childDescriptor -> childDescriptor.pruneDescriptorsForObsoleteDescriptions(newChildren));
		}
		else {
			super.removeFromHierarchy();
		}
	}

	private void addTagsFromParent(Set<TestTag> tags) {
		getParent().map(TestDescriptor::getTags).ifPresent(tags::addAll);
	}

	private void addCategoriesAsTags(Set<TestTag> tags) {
		Category annotation = description.getAnnotation(Category.class);
		if (annotation != null) {
			// @formatter:off
			stream(annotation.value())
					.map(ReflectionUtils::getAllAssignmentCompatibleClasses)
					.flatMap(Collection::stream)
					.distinct()
					.map(Class::getName)
					.map(TestTag::create)
					.forEachOrdered(tags::add);
			// @formatter:on
		}
	}

}
