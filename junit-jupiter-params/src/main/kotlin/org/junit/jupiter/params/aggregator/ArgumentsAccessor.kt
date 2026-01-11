/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

@file:API(status = API.Status.STABLE, since = "5.7")

package org.junit.jupiter.params.aggregator

import org.apiguardian.api.API

/**
 * Get the value of the argument at the given index as an instance of the
 * reified type.
 *
 * @param index the index of the argument to get; must be greater than or
 * equal to zero and less than {@link #size}
 * @return the value at the given index, potentially {@code null}
 * @since 5.3
 * @receiver[ArgumentsAccessor]
 * @see ArgumentsAccessor.get(Int, Class<T!>!)
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // method is in fact not shadowed due to reified type
inline fun <reified T : Any> ArgumentsAccessor.get(index: Int): T =
    this.get(index, T::class.java)!!
