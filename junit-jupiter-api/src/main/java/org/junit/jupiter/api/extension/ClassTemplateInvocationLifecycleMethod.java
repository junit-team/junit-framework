/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Internal marker annotation for lifecycle methods specific to implementations
 * of {@link ClassTemplateInvocationContextProvider}.
 *
 * @since 5.13
 */
@API(status = INTERNAL, since = "5.13")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ClassTemplateInvocationLifecycleMethod {

	/**
	 * The corresponding {@link org.junit.jupiter.api.ClassTemplate}-derived
	 * annotation class.
	 */
	Class<? extends Annotation> classTemplateAnnotation();

	/**
	 * The actual lifecycle method annotation class.
	 */
	Class<? extends Annotation> lifecycleMethodAnnotation();

}
