/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.api.util.testannotations;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.util.AbstractSystemPropertiesExtension;
import org.junit.jupiter.api.util.RestoreSystemProperties;

public final class CustomSystemPropertiesExtension
		extends AbstractSystemPropertiesExtension<SetProp, ClearProp, RestoreSystemProperties> {

	public @NonNull Class<SetProp> setPropertyAnnotation() {
		return SetProp.class;
	}

	public @NonNull Class<RestoreSystemProperties> restoreAnnotation() {
		return RestoreSystemProperties.class;
	}

	public @NonNull Class<ClearProp> clearAnnotation() {
		return ClearProp.class;
	}

	@Override
	public @NonNull String setAnnotationKey(SetProp annotation) {
		return annotation.key().name();
	}

	@Override
	public @NonNull String setAnnotationValue(SetProp annotation) {
		return annotation.value();
	}

	@Override
	public @NonNull String clearAnnotationKey(ClearProp annotation) {
		return annotation.key().name();
	}

}
