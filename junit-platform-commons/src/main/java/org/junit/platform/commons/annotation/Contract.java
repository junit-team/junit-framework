/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.annotation;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Specifies some aspects of the annotated method's behavior to be used by tools
 * for data flow analysis.
 *
 * @since 6.0
 * @see <a href="https://github.com/JetBrains/java-annotations/blob/2a28eab73042023559d2ec4cd00d6779213b6425/src/jvmMain/java/org/jetbrains/annotations/Contract.java">org.jetbrains.annotations.Contract</a>
 * @see <a href="https://github.com/uber/NullAway/wiki/Configuration#custom-contract-annotations">NullAway custom contract annotations</a>
 */
@Documented
@Target(ElementType.METHOD)
@API(status = INTERNAL, since = "6.0")
public @interface Contract {

	/**
	 * Contains the contract clauses describing causal relations between call
	 * arguments and the returned value.
	 */
	String value();

}
