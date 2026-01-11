/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.support;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.Field;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;

/**
 * {@code FieldContext} encapsulates the <em>context</em> in which an
 * {@link Parameter @Parameter}-annotated {@link Field} is declared in a
 * {@link ParameterizedClass @ParameterizedClass}.
 *
 * @since 5.13
 * @see ParameterizedClass
 * @see Parameter
 */
@API(status = EXPERIMENTAL, since = "6.0")
public interface FieldContext extends AnnotatedElementContext {

	/**
	 * {@return the field for this context; never {@code null}}
	 */
	Field getField();

	/**
	 * {@return the index of the parameter}
	 *
	 * <p>This method returns {@value Parameter#UNSET_INDEX} for aggregator
	 * fields and a value greater than or equal to zero for <em>indexed</em>
	 * parameters.
	 *
	 * @see Parameter#value()
	 */
	int getParameterIndex();

}
