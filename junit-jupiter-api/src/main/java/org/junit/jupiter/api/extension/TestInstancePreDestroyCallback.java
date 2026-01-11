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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * {@code TestInstancePreDestroyCallback} defines the API for {@link Extension
 * Extensions} that wish to process test instances <em>after</em> they have been
 * used in tests but <em>before</em> they are destroyed.
 *
 * <p>Common use cases include releasing resources that have been created for
 * the test instance, invoking custom clean-up methods on the test instance, etc.
 *
 * <p>Extensions that implement {@code TestInstancePreDestroyCallback} must be
 * registered at the class level if the test class is configured with
 * {@link Lifecycle @TestInstance(Lifecycle.PER_CLASS)}
 * semantics. If the test class is configured with
 * {@link Lifecycle @TestInstance(Lifecycle.PER_METHOD)}
 * semantics, {@code TestInstancePreDestroyCallback} extensions may be registered
 * at the class level or at the method level. In the latter case, the
 * {@code TestInstancePreDestroyCallback} extension will only be applied to the
 * test method for which it is registered.
 *
 * <p>A symmetric {@link TestInstancePreConstructCallback} extension defines a callback
 * hook that is invoked prior to any test class instances being constructed.
 *
 * <h2>Constructor Requirements</h2>
 *
 * <p>Consult the documentation in {@link Extension} for details on constructor
 * requirements.
 *
 * @since 5.6
 * @see #preDestroyTestInstance(ExtensionContext)
 * @see TestInstancePostProcessor
 * @see TestInstancePreConstructCallback
 * @see TestInstanceFactory
 * @see ParameterResolver
 */
@FunctionalInterface
@API(status = STABLE, since = "5.7")
public interface TestInstancePreDestroyCallback extends Extension {

	/**
	 * Callback for processing test instances before they are destroyed.
	 *
	 * <p>Contrary to {@link TestInstancePostProcessor#postProcessTestInstance}
	 * this method is only called once for each {@link ExtensionContext} even if
	 * there are multiple test instances about to be destroyed in case of
	 * {@link Nested @Nested} tests. Please use the provided
	 * {@link #preDestroyTestInstances(ExtensionContext, Consumer)} utility
	 * method to ensure that all test instances are handled.
	 *
	 * @param context the current extension context; never {@code null}
	 * @see ExtensionContext#getTestInstance()
	 * @see ExtensionContext#getRequiredTestInstance()
	 * @see ExtensionContext#getTestInstances()
	 * @see ExtensionContext#getRequiredTestInstances()
	 * @see #preDestroyTestInstances(ExtensionContext, Consumer)
	 */
	void preDestroyTestInstance(ExtensionContext context) throws Exception;

	/**
	 * Utility method for processing <em>all</em> test instances of an
	 * {@link ExtensionContext} that are not present in any of its parent
	 * contexts.
	 *
	 * <p>This method should be called in order to implement this interface
	 * correctly since it ensures that the right test instances are processed
	 * regardless of the used {@linkplain Lifecycle lifecycle}. The supplied
	 * callback is called once per test instance that is about to be destroyed
	 * starting with the innermost one.
	 *
	 * <p>This method is intended to be called from an implementation of
	 * {@link #preDestroyTestInstance(ExtensionContext)} like this:
	 *
	 * <pre>{@code class MyExtension implements TestInstancePreDestroyCallback {
	 *    @Override
	 *    public void preDestroyTestInstance(ExtensionContext context) {
	 *        TestInstancePreDestroyCallback.preDestroyTestInstances(context, testInstance -> {
	 *            // custom logic that processes testInstance
	 *        });
	 *    }
	 *}}</pre>
	 *
	 * @param context the current extension context; never {@code null}
	 * @param callback the callback to be invoked for every test instance of the
	 * current extension context that is about to be destroyed; never
	 * {@code null}
	 * @since 5.7.1
	 */
	@API(status = STABLE, since = "5.10")
	static void preDestroyTestInstances(ExtensionContext context, Consumer<Object> callback) {
		List<Object> destroyedInstances = new ArrayList<>(context.getRequiredTestInstances().getAllInstances());
		for (Optional<ExtensionContext> current = context.getParent(); current.isPresent(); current = current.get().getParent()) {
			current.get().getTestInstances().map(TestInstances::getAllInstances).ifPresent(
				destroyedInstances::removeAll);
		}
		Collections.reverse(destroyedInstances);
		destroyedInstances.forEach(callback);
	}

}
