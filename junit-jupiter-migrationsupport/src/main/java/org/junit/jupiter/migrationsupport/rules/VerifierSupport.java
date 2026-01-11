/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.apiguardian.api.API.Status.DEPRECATED;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.migrationsupport.rules.adapter.VerifierAdapter;
import org.junit.rules.Verifier;

/**
 * This {@code Extension} provides native support for subclasses of
 * the {@link Verifier} rule from JUnit 4.
 *
 * <p>{@code @Rule}-annotated fields as well as methods are supported.
 *
 * <p>By using this class-level extension on a test class such
 * {@code Verifier} implementations in legacy code bases
 * can be left unchanged including the JUnit 4 rule import statements.
 *
 * <p>However, if you intend to develop a <em>new</em> extension for
 * JUnit please use the new extension model of JUnit Jupiter instead
 * of the rule-based model of JUnit 4.
 *
 * @since 5.0
 * @see org.junit.rules.Verifier
 * @see org.junit.rules.TestRule
 * @see org.junit.Rule
 * @deprecated Please implement {@link org.junit.jupiter.api.extension.AfterTestExecutionCallback} instead.
 */
@SuppressWarnings("removal")
@API(status = DEPRECATED, since = "6.0")
@Deprecated(since = "6.0", forRemoval = true)
public class VerifierSupport implements AfterEachCallback {

	private final TestRuleSupport support = new TestRuleSupport(VerifierAdapter::new, Verifier.class);

	public VerifierSupport() {
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		this.support.afterEach(context);
	}

}
