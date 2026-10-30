/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.store;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.util.function.Function;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * {@code Store} provides methods to save and retrieve data.
 *
 * <p>Unless documented otherwise by a subinterface, stored values that
 * implement {@link AutoCloseable} are closed when the store is closed.
 * Values that are manually {@linkplain #remove(Object) removed} are not
 * closed.
 *
 * @since 6.2
 * @see org.junit.jupiter.api.extension.ExtensionContext.Store
 */
@API(status = MAINTAINED, since = "6.2")
public interface Store {

	/**
	 * Get the value that is stored under the supplied {@code key}.
	 *
	 * <p>If no value is stored in this store for the supplied
	 * {@code key}, parent stores (if any) will be queried for a value with
	 * the same {@code key} in the {@code Namespace} used to create this store.
	 *
	 * <p>For greater type safety, consider using {@link #get(Object, Class)}
	 * instead.
	 *
	 * @param key the key; never {@code null}
	 * @return the value; potentially {@code null}
	 * @see #get(Object, Class)
	 * @see #getOrDefault(Object, Class, Object)
	 */
	@Nullable
	Object get(Object key);

	/**
	 * Get the value of the specified required type that is stored under
	 * the supplied {@code key}.
	 *
	 * <p>If no value is stored in this store for the supplied
	 * {@code key}, parent stores (if any) will be queried for a value with
	 * the same {@code key} in the {@code Namespace} used to create this store.
	 *
	 * @param key the key; never {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @param <V> the value type
	 * @return the value; potentially {@code null}
	 * @see #get(Object)
	 * @see #getOrDefault(Object, Class, Object)
	 */
	<V> @Nullable V get(Object key, Class<V> requiredType);

	/**
	 * Get the value of the specified required type that is stored under
	 * the supplied {@code key}, or the supplied {@code defaultValue} if no
	 * value is found for the supplied {@code key} in this store or in a
	 * parent store.
	 *
	 * <p>If no value is stored in this store for the supplied
	 * {@code key}, parent stores (if any) will be queried for a value with
	 * the same {@code key} in the {@code Namespace} used to create this store.
	 *
	 * @param key the key; never {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @param defaultValue the default value; never {@code null}
	 * @param <V> the value type
	 * @return the value; never {@code null}
	 * @see #get(Object, Class)
	 */
	default <V> V getOrDefault(Object key, Class<V> requiredType, V defaultValue) {
		V value = get(key, requiredType);
		return (value != null ? value : defaultValue);
	}

	/**
	 * Return the object of type {@code type} if it is present and not
	 * {@code null} in this {@code Store} (<em>keyed</em> by {@code type});
	 * otherwise, invoke the default constructor for {@code type} to
	 * generate the object, store it, and return it.
	 *
	 * <p>This method is a shortcut for the following, where {@code X} is
	 * the type of object we wish to retrieve from the store.
	 *
	 * <pre style="code">
	 * X x = store.computeIfAbsent(X.class, key -&gt; new X(), X.class);
	 * // Equivalent to:
	 * // X x = store.computeIfAbsent(X.class);
	 * </pre>
	 *
	 * <p>See {@link #computeIfAbsent(Object, Function, Class)} for further
	 * details.
	 *
	 * <p>If {@code type} implements {@link AutoCloseable}, then the
	 * {@code close()} method will be invoked on the stored object when the
	 * store is closed, unless documented otherwise by a subinterface.
	 *
	 * @param type the type of object to retrieve; never {@code null}
	 * @param <V> the key and value type
	 * @return the object; never {@code null}
	 * @see #computeIfAbsent(Object, Function)
	 * @see #computeIfAbsent(Object, Function, Class)
	 * @see AutoCloseable
	 */
	default <V> V computeIfAbsent(Class<V> type) {
		return computeIfAbsent(type, ReflectionSupport::newInstance, type);
	}

	/**
	 * Return the value of the specified required type that is stored under
	 * the supplied {@code key}.
	 *
	 * <p>If no value is stored in this store for the supplied
	 * {@code key}, parent stores (if any) will be queried for a value with
	 * the same {@code key} in the {@code Namespace} used to create this store.
	 * If no value is found for the supplied {@code key}
	 * or the value is {@code null}, a new value will be computed by the
	 * {@code defaultCreator} (given the {@code key} as input), stored, and
	 * returned.
	 *
	 * <p>For greater type safety, consider using
	 * {@link #computeIfAbsent(Object, Function, Class)} instead.
	 *
	 * <p>If the created value is an instance of {@link AutoCloseable}, then the
	 * {@code close()} method will be invoked on the stored object when the
	 * store is closed, unless documented otherwise by a subinterface.
	 *
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} and must not return
	 * {@code null}
	 * @param <K> the key type
	 * @param <V> the value type
	 * @return the value; never {@code null}
	 * @see #computeIfAbsent(Class)
	 * @see #computeIfAbsent(Object, Function, Class)
	 * @see AutoCloseable
	 */
	<K, V> Object computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator);

	/**
	 * Get the value of the specified required type that is stored under the
	 * supplied {@code key}.
	 *
	 * <p>If no value is stored in this store for the supplied
	 * {@code key}, parent stores (if any) will be queried for a value with
	 * the same {@code key} in the {@code Namespace} used to create this store.
	 * If no value is found for the supplied {@code key}
	 * or the value is {@code null}, a new value will be computed by the
	 * {@code defaultCreator} (given the {@code key} as input), stored, and
	 * returned.
	 *
	 * <p>If the created value is an instance of {@link AutoCloseable}, then the
	 * {@code close()} method will be invoked on the stored object when the
	 * store is closed, unless documented otherwise by a subinterface.
	 *
	 * @param key the key; never {@code null}
	 * @param defaultCreator the function called with the supplied {@code key}
	 * to create a new value; never {@code null} and must not return
	 * {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @param <K> the key type
	 * @param <V> the value type
	 * @return the value; never {@code null}
	 * @see #computeIfAbsent(Class)
	 * @see #computeIfAbsent(Object, Function)
	 * @see AutoCloseable
	 */
	<K, V> V computeIfAbsent(K key, Function<? super K, ? extends V> defaultCreator, Class<V> requiredType);

	/**
	 * Store a {@code value} for later retrieval under the supplied {@code key}.
	 *
	 * <p>A stored {@code value} is visible in child stores for the store's
	 * {@code Namespace} unless they
	 * overwrite it.
	 *
	 * <p>If the {@code value} is an instance of {@link AutoCloseable}, then the
	 * {@code close()} method will be invoked on the stored object when the
	 * store is closed, unless documented otherwise by a subinterface.
	 *
	 * @param key the key under which the value should be stored; never
	 * {@code null}
	 * @param value the value to store; may be {@code null}
	 * @see AutoCloseable
	 */
	void put(Object key, @Nullable Object value);

	/**
	 * Remove the value that was previously stored under the supplied {@code key}.
	 *
	 * <p>The value will only be removed in this store, not in parent
	 * stores. In addition, the {@link AutoCloseable}
	 * API will not be honored for values that are manually removed via this method.
	 *
	 * <p>For greater type safety, consider using {@link #remove(Object, Class)}
	 * instead.
	 *
	 * @param key the key; never {@code null}
	 * @return the previous value or {@code null} if no value was present
	 * for the specified key
	 * @see #remove(Object, Class)
	 */
	@Nullable
	Object remove(Object key);

	/**
	 * Remove the value of the specified required type that was previously stored
	 * under the supplied {@code key}.
	 *
	 * <p>The value will only be removed in this store, not in parent
	 * stores. In addition, the {@link AutoCloseable}
	 * API will not be honored for values that are manually removed via this method.
	 *
	 * @param key the key; never {@code null}
	 * @param requiredType the required type of the value; never {@code null}
	 * @param <V> the value type
	 * @return the previous value or {@code null} if no value was present
	 * for the specified key
	 * @see #remove(Object)
	 */
	<V> @Nullable V remove(Object key, Class<V> requiredType);

}
