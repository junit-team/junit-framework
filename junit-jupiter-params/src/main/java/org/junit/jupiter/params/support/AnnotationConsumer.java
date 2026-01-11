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

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.apiguardian.api.API;

/**
 * {@code AnnotationConsumer} is a {@linkplain FunctionalInterface functional
 * interface} for consuming annotations.
 *
 * <p>It is typically implemented by implementations of
 * {@link org.junit.jupiter.params.provider.ArgumentsProvider ArgumentsProvider}
 * and {@link org.junit.jupiter.params.converter.ArgumentConverter ArgumentConverter}
 * in order to signal that they can {@link #accept accept} a certain annotation.
 *
 * @since 5.0
 */
@FunctionalInterface
@API(status = STABLE, since = "5.7")
public interface AnnotationConsumer<A extends Annotation> extends Consumer<A> {
}
