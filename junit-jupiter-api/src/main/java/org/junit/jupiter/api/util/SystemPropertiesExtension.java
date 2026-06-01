/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

/**
 * {@code Extension} which provides support for the following annotations.
 *
 * <ul>
 * <li>{@link SetSystemProperty @SetSystemProperty}</li>
 * <li>{@link ClearSystemProperty @ClearSystemProperty}</li>
 * <li>{@link RestoreSystemProperties @RestoreSystemProperties}</li>
 * </ul>
 *
 * @since 6.1
 */
final class SystemPropertiesExtension
		extends AbstractSystemPropertiesExtension<SetSystemProperty, ClearSystemProperty, RestoreSystemProperties> {

	@Override
	protected Class<SetSystemProperty> setPropertyAnnotation() {
		return SetSystemProperty.class;
	}

	@Override
	protected Class<RestoreSystemProperties> restoreAnnotation() {
		return RestoreSystemProperties.class;
	}

	@Override
	protected Class<ClearSystemProperty> clearAnnotation() {
		return ClearSystemProperty.class;
	}

	@Override
	String setAnnotationKey(SetSystemProperty annotation) {
		return annotation.key();
	}

	@Override
	String setAnnotationValue(SetSystemProperty annotation) {
		return annotation.value();
	}

	@Override
	String clearAnnotationKey(ClearSystemProperty annotation) {
		return annotation.key();
	}

}
