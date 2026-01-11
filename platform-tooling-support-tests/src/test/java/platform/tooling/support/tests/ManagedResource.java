/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.platform.commons.support.ReflectionSupport.streamFields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.Preconditions;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RUNTIME)
@ExtendWith(ManagedResource.Extension.class)
public @interface ManagedResource {

	@Target(ElementType.TYPE)
	@Retention(RUNTIME)
	@interface Scoped {

		Class<? extends Provider> value();

		interface Provider {
			Scope determineScope(ExtensionContext extensionContext);
		}
	}

	enum Scope {
		GLOBAL, PER_CONTEXT
	}

	class Extension implements ParameterResolver, TestInstancePostProcessor {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			return parameterContext.isAnnotated(ManagedResource.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			Class<?> type = parameterContext.getParameter().getType();
			return getOrCreateResource(extensionContext, type).get();
		}

		@Override
		public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
			return ExtensionContextScope.TEST_METHOD;
		}

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) {
			streamFields(testInstance.getClass(), field -> AnnotationSupport.isAnnotated(field, ManagedResource.class),
				HierarchyTraversalMode.BOTTOM_UP) //
						.forEach(field -> {
							try {
								field.set(testInstance, getOrCreateResource(extensionContext, field.getType()).get());
							}
							catch (IllegalAccessException e) {
								throw new RuntimeException("Failed to inject resource into field: " + field, e);
							}
						});
		}

		@SuppressWarnings("unchecked")
		private <T> Resource<T> getOrCreateResource(ExtensionContext extensionContext, Class<T> type) {
			var scope = AnnotationSupport.findAnnotation(type, Scoped.class) //
					.map(Scoped::value) //
					.map(ReflectionSupport::newInstance) //
					.map(provider -> provider.determineScope(extensionContext)) //
					.orElse(Scope.GLOBAL);
			var storingContext = switch (scope) {
				case GLOBAL -> extensionContext.getRoot();
				case PER_CONTEXT -> extensionContext;
			};
			return storingContext.getStore(Namespace.GLOBAL) //
					.computeIfAbsent(type, Resource::new, Resource.class);
		}
	}

	@SuppressWarnings("try")
	class Resource<T> implements AutoCloseable {

		private final T value;

		private Resource(Class<T> type) {
			Preconditions.condition(AutoCloseable.class.isAssignableFrom(type),
				() -> "Resource type must implement AutoCloseable: " + type.getName());
			this.value = ReflectionSupport.newInstance(type);
		}

		private T get() {
			return value;
		}

		@Override
		public void close() throws Exception {
			((AutoCloseable) value).close();
		}
	}
}
