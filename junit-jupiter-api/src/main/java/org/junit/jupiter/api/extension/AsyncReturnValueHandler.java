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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code AsyncReturnValueHandler} enables support for custom asynchronous
 * return types in test methods.
 *
 * <p>By default the engine only treats the JRE types {@link CompletionStage}
 * and {@link java.util.concurrent.Future} as asynchronous return types. An
 * {@code AsyncReturnValueHandler} allows a user-defined promise-like type
 * (for example {@code MyPromise<T>}) to be awaited by mapping the value
 * returned from a test method to a {@link CompletionStage}.
 *
 * <p>Implementations can be registered <em>automatically</em> via the
 * {@link java.util.ServiceLoader} mechanism (in which case they apply
 * <em>globally</em> to all tests) or <em>declaratively</em> on a test method
 * or class via {@link ExtendWith @ExtendWith}. When registered through
 * {@code @ExtendWith}, an indirection annotation is also possible, for example:
 *
 * {@snippet :
 * @Retention(RUNTIME)
 * @Target({ METHOD, ANNOTATION_TYPE })
 * @ExtendWith(MyAsyncReturnValueHandler.class)
 * public @interface MyPromise {
 * }
 *
 * @Test
 * @MyPromise
 * MyPromise<String> myTest() { ... }
 * }
 *
 * <p>Because this interface extends {@link EarlyExtension}, implementations are
 * loaded <em>during discovery</em> so that the engine can recognize the custom
 * return type while deciding whether a method is a test.
 *
 * <h2>Requirements</h2>
 *
 * <ul>
 * <li>{@link #supports(Type, AnnotatedElement)} must be a <em>pure</em> query
 * method with no side effects; it is called during discovery as well as at
 * runtime.</li>
 * <li>{@link #toCompletionStage(Object)} must not return {@code null}.</li>
 * <li>Implementations must have a {@code public} default constructor when
 * loaded via the {@code ServiceLoader}.</li>
 * </ul>
 *
 * @since 6.2
 */
@API(status = EXPERIMENTAL, since = "6.2")
public interface AsyncReturnValueHandler extends EarlyExtension {

	/**
	 * Determine whether this handler supports the supplied generic return type
	 * of a test method.
	 *
	 * <p>This method is a <em>pure</em> query and must not have side effects.
	 * It may be called during discovery, before any test instance or
	 * {@link ExtensionContext} exists.
	 *
	 * <p>When the engine converts an already-returned value (rather than
	 * inspecting a method declaration) the {@code annotatedElement} may be
	 * {@code null}; implementations must not dereference it in that case.
	 *
	 * @param genericReturnType the generic return type of the test method, or
	 * the raw class of a returned value; never {@code null}
	 * @param annotatedElement the test method itself, or {@code null} when not
	 * available
	 * @return {@code true} if this handler can map a value of the supplied type
	 * to a {@link CompletionStage}
	 */
	boolean supports(Type genericReturnType, @Nullable AnnotatedElement annotatedElement);

	/**
	 * Map the value actually returned from a test method to a
	 * {@link CompletionStage} to be awaited.
	 *
	 * <p>This method is only invoked for values whose type was reported as
	 * supported by {@link #supports(Type, AnnotatedElement)}.
	 *
	 * @param returnedValue the value returned from the test method; never
	 * {@code null}
	 * @return the {@link CompletionStage} to await; never {@code null}
	 */
	CompletionStage<?> toCompletionStage(Object returnedValue);

}
