/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.support;

import static java.util.Collections.synchronizedMap;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.LruCache;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code AsyncReturnTypeSupport} provides support for test methods that return
 * an asynchronous completion signal.
 *
 * <p>Returning a {@link CompletionStage}, {@link CompletableFuture}, or
 * {@link Future} from a {@code @Test} method is interpreted as a promise that
 * the test's asynchronous work has terminated; its payload (if any) is ignored.
 * Only the termination itself is awaited, so the return type is treated as an
 * opaque signal.
 *
 * @since 6.2
 */
@API(status = INTERNAL, since = "6.2", consumers = "org.junit.jupiter.engine")
public final class AsyncReturnTypeSupport {

	private AsyncReturnTypeSupport() {
		/* no-op */
	}

	/**
	 * Caches, per test {@link Method}, whether it has an asynchronous return
	 * type. This avoids invoking user-provided
	 * {@link AsyncReturnValueHandler#supports(Type, AnnotatedElement)
	 * handlers} and re-walking {@link ExtendWith @ExtendWith} annotations over
	 * and over again for the same method during discovery and execution.
	 */
	private static final Map<Method, Boolean> isAsynchronousReturnTypeCache = synchronizedMap(new LruCache<>(512));

	/**
	 * Determine whether the supplied return type is a fully supported
	 * asynchronous completion signal.
	 *
	 * @param type the (raw) return type; never {@code null}
	 * @return {@code true} if the type is a supported asynchronous signal
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code type} is {@code null}
	 */
	public static boolean isFullySupported(Class<?> type) {
		Preconditions.notNull(type, "type must not be null");
		return CompletionStage.class.isAssignableFrom(type) || Future.class.isAssignableFrom(type);
	}

	/**
	 * Determine whether the return type of the supplied method is asynchronous,
	 * either because it is a built-in JRE type or because one of the supplied
	 * handlers {@link AsyncReturnValueHandler#supports(Type, AnnotatedElement)
	 * supports} it.
	 *
	 * @param method the method whose return type to inspect; never {@code null}
	 * @param asyncReturnValueHandlers the handlers to consult; never {@code null}
	 * @return {@code true} if the method is asynchronous
	 */
	public static boolean isSupported(Method method, List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		return isAsynchronousReturnTypeCache.computeIfAbsent(method,
			__ -> isSupportedUncached(method, asyncReturnValueHandlers));
	}

	private static boolean isSupportedUncached(Method method, List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		if (isFullySupported(method.getReturnType())) {
			return true;
		}
		Type genericReturnType = method.getGenericReturnType();
		for (AsyncReturnValueHandler handler : asyncReturnValueHandlers) {
			if (handler.supports(genericReturnType, method)) {
				return true;
			}
		}
		for (AsyncReturnValueHandler handler : getExtendWithHandlers(method)) {
			if (handler.supports(genericReturnType, method)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Instantiate the {@link AsyncReturnValueHandler AsyncReturnValueHandlers}
	 * declared directly or transitively via {@link ExtendWith @ExtendWith} for
	 * the supplied test method and its declaring class hierarchy.
	 *
	 * <p>The method's own {@code @ExtendWith} annotations (including composed
	 * indirection annotations), the annotations of its declaring class and of
	 * each enclosing class (for {@code @Nested} tests), are consulted. For each
	 * level, {@link #collectFromExtendWith(AnnotatedElement, Set, List)} delegates
	 * to {@link AnnotationUtils#findRepeatableAnnotations(AnnotatedElement,
	 * Class)} so that {@code @Inherited} superclass and interface declarations,
	 * composed annotations, and {@code @Repeatable} containers are resolved
	 * exactly as they are for the runtime {@link ExtensionRegistry}.
	 *
	 * <p>This is needed during <em>discovery</em> to recognize a custom return
	 * type declared through a class- or method-level {@code @ExtendWith} before
	 * the regular {@link ExtensionRegistry} exists. The actual handler instances
	 * that convert a returned value into a signal to await are resolved from
	 * that runtime {@link ExtensionRegistry}.
	 *
	 * @param method the test method; never {@code null}
	 * @return the discovered handlers; never {@code null}
	 */
	public static List<AsyncReturnValueHandler> getExtendWithHandlers(Method method) {
		Set<Class<?>> visitedExtensionClasses = new HashSet<>();
		Set<Class<?>> visitedClasses = new HashSet<>();
		List<AsyncReturnValueHandler> handlers = new ArrayList<>();

		collectFromExtendWith(method, visitedExtensionClasses, handlers);

		Class<?> testClass = method.getDeclaringClass();
		for (Class<?> candidateClass = testClass; candidateClass != null
				&& visitedClasses.add(candidateClass); candidateClass = candidateClass.getEnclosingClass()) {
			collectFromExtendWith(candidateClass, visitedExtensionClasses, handlers);
		}
		return handlers;
	}

	private static void collectFromExtendWith(AnnotatedElement element, Set<Class<?>> visitedExtensionClasses,
			List<AsyncReturnValueHandler> handlers) {
		if (element == null) {
			return;
		}
		for (ExtendWith extendWith : AnnotationUtils.findRepeatableAnnotations(element, ExtendWith.class)) {
			for (Class<? extends Extension> extensionClass : extendWith.value()) {
				if (visitedExtensionClasses.add(extensionClass)
						&& AsyncReturnValueHandler.class.isAssignableFrom(extensionClass)) {
					handlers.add(instantiate(extensionClass));
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private static AsyncReturnValueHandler instantiate(Class<? extends Extension> extensionClass) {
		try {
			Constructor<? extends Extension> constructor = extensionClass.getDeclaredConstructor();
			if (!constructor.canAccess(null)) {
				constructor.setAccessible(true);
			}
			return (AsyncReturnValueHandler) constructor.newInstance();
		}
		catch (ReflectiveOperationException ex) {
			throw ExceptionUtils.throwAsUncheckedException(ex);
		}
	}

	/**
	 * Find the first {@link AsyncReturnValueHandler} that {@link
	 * AsyncReturnValueHandler#supports(Type, AnnotatedElement) supports} the
	 * supplied value, or {@code null} if none does.
	 *
	 * <p>The value's raw class is used as the return type to be matched.
	 *
	 * @param value the value to match; never {@code null}
	 * @param asyncReturnValueHandlers the handlers to consult; never {@code null}
	 * @return the matching handler, or {@code null}
	 */
	@Nullable
	public static AsyncReturnValueHandler findHandler(Object value, @Nullable AnnotatedElement annotatedElement,
			List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		Class<?> valueType = value.getClass();
		for (AsyncReturnValueHandler handler : asyncReturnValueHandlers) {
			if (handler.supports(valueType, annotatedElement)) {
				return handler;
			}
		}
		return null;
	}

	/**
	 * Convert the supplied asynchronously returned value into a signal to await.
	 *
	 * @param value the value returned by a test method; never {@code null}
	 * @return a {@link CompletableFuture} representing the value's termination;
	 * never {@code null}
	 * @throws org.junit.platform.commons.PreconditionViolationException if
	 * {@code value} is {@code null}
	 */
	public static CompletableFuture<Void> toCompletableFuture(Object value) {
		return toCompletableFuture(value, null, List.of());
	}

	/**
	 * Convert the supplied asynchronously returned value into a signal to await,
	 * consulting the supplied {@link AsyncReturnValueHandler handlers} before
	 * falling back to the built-in JRE types.
	 *
	 * @param value the value returned by a test method; never {@code null}
	 * @param asyncReturnValueHandlers the handlers to consult; never {@code null}
	 * @return a {@link CompletableFuture} representing the value's termination;
	 * never {@code null}
	 */
	public static CompletableFuture<Void> toCompletableFuture(Object value, @Nullable AnnotatedElement annotatedElement,
			List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		Preconditions.notNull(value, "value must not be null");
		AsyncReturnValueHandler handler = findHandler(value, annotatedElement, asyncReturnValueHandlers);
		if (handler != null) {
			CompletionStage<?> stage = handler.toCompletionStage(value);
			Preconditions.notNull(stage, "toCompletionStage() must not return null");
			return stage.toCompletableFuture().thenApply(__ -> null);
		}
		if (value instanceof CompletionStage<?> stage) {
			return stage.toCompletableFuture().thenApply(__ -> null);
		}
		if (value instanceof Future<?> future) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					future.get();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new CompletionException(e);
				}
				catch (java.util.concurrent.ExecutionException e) {
					throw new CompletionException(e.getCause());
				}
				return null;
			});
		}
		return CompletableFuture.completedFuture(null);
	}
}
