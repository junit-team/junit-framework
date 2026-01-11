/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static java.util.Collections.emptySet;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * A {@code ResourceLocksProvider} is used to programmatically add shared resources
 * to a test class or its test methods dynamically at runtime.
 *
 * <p>Each shared resource is represented by an instance of {@link Lock}.
 *
 * <p>Adding shared resources via this API has the same semantics as declaring
 * them declaratively via {@link ResourceLock @ResourceLock(value, mode)}, but for
 * some use cases the programmatic approach may be more flexible and less verbose.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @since 5.12
 * @see ResourceLock#providers()
 * @see Resources
 * @see ResourceAccessMode
 * @see Lock
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface ResourceLocksProvider {

	/**
	 * Add shared resources for a test class.
	 *
	 * <p>Invoked in case a test class or its parent class is annotated with
	 * {@code @ResourceLock(providers)}.
	 *
	 * @apiNote Adding {@linkplain Lock a shared resource} via this method has
	 * the same semantics as annotating a test class with an analogous
	 * {@code @ResourceLock(value, mode)} declaration.
	 *
	 * @param testClass a test class for which to add shared resources
	 * @return a set of {@link Lock}; may be empty
	 */
	default Set<Lock> provideForClass(Class<?> testClass) {
		return emptySet();
	}

	/**
	 * Add shared resources for a
	 * {@link org.junit.jupiter.api.Nested @Nested} test class.
	 *
	 * <p>Invoked in case:
	 * <ul>
	 *   <li>an enclosing test class of any level or its parent class is
	 *   annotated with {@code @ResourceLock(providers = ...)}.</li>
	 *   <li>a nested test class or its parent class is annotated with
	 *   {@code @ResourceLock(providers = ...)}.</li>
	 * </ul>
	 *
	 * @apiNote Adding {@linkplain Lock a shared resource} via this method has
	 * the same semantics as annotating a nested test class with an analogous
	 * {@code @ResourceLock(value, mode)} declaration.
	 *
	 * @implNote The classes supplied as {@code enclosingInstanceTypes} may
	 * differ from the classes returned from invocations of
	 * {@link Class#getEnclosingClass()} &mdash; for example, when a nested test
	 * class is inherited from a superclass.
	 *
	 * @param enclosingInstanceTypes the runtime types of the enclosing
	 * instances for the test class, ordered from outermost to innermost,
	 * excluding {@code testClass}; never {@code null}
	 * @param testClass a nested test class for which to add shared resources
	 * @return a set of {@link Lock}; may be empty
	 * @see org.junit.jupiter.api.Nested @Nested
	 */
	default Set<Lock> provideForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> testClass) {
		return emptySet();
	}

	/**
	 * Add shared resources for a test method.
	 *
	 * <p>Invoked in case:
	 * <ul>
	 *   <li>an enclosing test class of any level or its parent class is
	 *   annotated with {@code @ResourceLock(providers)}.</li>
	 *   <li>a test method is annotated with {@code @ResourceLock(providers)}.</li>
	 * </ul>
	 *
	 * @apiNote Adding {@linkplain Lock a shared resource} with this method
	 * has the same semantics as annotating a test method
	 * with analogous {@code @ResourceLock(value, mode)}.
	 *
	 * @implNote The classes supplied as {@code enclosingInstanceTypes} may
	 * differ from the classes returned from invocations of
	 * {@link Class#getEnclosingClass()} &mdash; for example, when a nested test
	 * class is inherited from a superclass. Similarly, the class instance
	 * supplied as {@code testClass} may differ from the class returned by
	 * {@code testMethod.getDeclaringClass()} &mdash; for example, when a test
	 * method is inherited from a superclass.
	 *
	 * @param enclosingInstanceTypes the runtime types of the enclosing
	 * instances for the test class, ordered from outermost to innermost,
	 * excluding {@code testClass}; never {@code null}
	 * @param testClass the test class or {@link org.junit.jupiter.api.Nested @Nested}
	 * test class that contains the {@code testMethod}
	 * @param testMethod a test method for which to add shared resources
	 * @return a set of {@link Lock}; may be empty
	 * @see org.junit.jupiter.api.Nested @Nested
	 */
	default Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
		return emptySet();
	}

	/**
	 * {@code Lock} represents a shared resource.
	 *
	 * <p>Each resource is identified by a {@linkplain #getKey() key}. In addition,
	 * the {@linkplain #getAccessMode() access mode} allows one to specify whether
	 * a test class or test method requires {@link ResourceAccessMode#READ_WRITE
	 * READ_WRITE} or {@link ResourceAccessMode#READ READ} access to the resource.
	 *
	 * @apiNote {@link #getKey()} and {@link #getAccessMode()} have the same
	 * semantics as {@link ResourceLock#value()} and {@link ResourceLock#mode()}
	 * respectively.
	 *
	 * @since 5.12
	 * @see Isolated
	 * @see Resources
	 * @see ResourceAccessMode
	 * @see ResourceLock
	 * @see ResourceLocksProvider
	 */
	final class Lock {

		private final String key;

		private final ResourceAccessMode accessMode;

		/**
		 * Create a new {@code Lock} with {@link ResourceAccessMode#READ_WRITE}.
		 *
		 * @param key the identifier of the resource; never {@code null} or blank
		 * @see ResourceLock#value()
		 */
		public Lock(String key) {
			this(key, ResourceAccessMode.READ_WRITE);
		}

		/**
		 * Create a new {@code Lock}.
		 *
		 * @param key the identifier of the resource; never {@code null} or blank
		 * @param accessMode the lock mode to use to synchronize access to the
		 * resource; never {@code null}
		 * @see ResourceLock#value()
		 * @see ResourceLock#mode()
		 */
		public Lock(String key, ResourceAccessMode accessMode) {
			this.key = Preconditions.notBlank(key, "key must not be null or blank");
			this.accessMode = Preconditions.notNull(accessMode, "accessMode must not be null");
		}

		/**
		 * Get the key for this lock.
		 *
		 * @see ResourceLock#value()
		 */
		public String getKey() {
			return this.key;
		}

		/**
		 * Get the access mode for this lock.
		 *
		 * @see ResourceLock#mode()
		 */
		public ResourceAccessMode getAccessMode() {
			return this.accessMode;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Lock that = (Lock) o;
			return this.key.equals(that.key) && this.accessMode == that.accessMode;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.key, this.accessMode);
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this) //
					.append("key", this.key) //
					.append("accessMode", this.accessMode) //
					.toString();
		}
	}

}
