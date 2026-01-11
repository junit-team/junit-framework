/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;

/**
 * An exclusive resource identified by a key with a lock mode that is used to
 * synchronize access to shared resources when executing nodes in parallel.
 *
 * @since 1.3
 * @see Node#getExecutionMode()
 */
@API(status = STABLE, since = "1.10")
public class ExclusiveResource {

	/**
	 * Key of the global resource lock that all direct children of the engine
	 * descriptor acquire in {@linkplain LockMode#READ read mode} by default:
	 * {@value}
	 *
	 * <p>If any node {@linkplain Node#getExclusiveResources() requires} an
	 * exclusive resource with the same key in
	 * {@linkplain LockMode#READ_WRITE read-write mode}, the lock will be
	 * coarsened to be acquired by the node's ancestor that is a direct child of
	 * the engine descriptor and all of the ancestor's descendants will be
	 * forced to run in the {@linkplain ExecutionMode#SAME_THREAD same thread}.
	 *
	 * @since 1.7
	 */
	@API(status = STABLE, since = "1.10")
	public static final String GLOBAL_KEY = "org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_KEY";

	static final ExclusiveResource GLOBAL_READ = new ExclusiveResource(GLOBAL_KEY, LockMode.READ);
	static final ExclusiveResource GLOBAL_READ_WRITE = new ExclusiveResource(GLOBAL_KEY, LockMode.READ_WRITE);

	static final Comparator<ExclusiveResource> COMPARATOR //
		= comparing(ExclusiveResource::getKey, globalKeyFirst().thenComparing(naturalOrder())) //
				.thenComparing(ExclusiveResource::getLockMode);

	private static Comparator<String> globalKeyFirst() {
		return comparing(key -> !GLOBAL_KEY.equals(key));
	}

	private final String key;
	private final LockMode lockMode;
	private int hash;

	/**
	 * Create a new {@code ExclusiveResource}.
	 *
	 * @param key the identifier of the resource; never {@code null} or blank
	 * @param lockMode the lock mode to use to synchronize access to the
	 * resource; never {@code null}
	 */
	public ExclusiveResource(String key, LockMode lockMode) {
		this.key = Preconditions.notBlank(key, "key must not be blank");
		this.lockMode = Preconditions.notNull(lockMode, "lockMode must not be null");
	}

	/**
	 * Get the key of this resource.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the lock mode of this resource.
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	@SuppressWarnings("EqualsGetClass")
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExclusiveResource that = (ExclusiveResource) o;
		return Objects.equals(key, that.key) && lockMode == that.lockMode;
	}

	@Override
	public int hashCode() {
		int h = hash;
		if (h == 0) {
			h = hash = Objects.hash(key, lockMode);
		}
		return h;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("key", key).append("lockMode", lockMode).toString();
	}

	/**
	 * {@code LockMode} translates to the respective {@link ReadWriteLock}
	 * locks.
	 *
	 * @implNote Enum order is important, since it can be used to sort locks, so
	 * the stronger mode has to be first.
	 */
	public enum LockMode {

		/**
		 * Require read and write access to the resource.
		 *
		 * @see ReadWriteLock#writeLock()
		 */
		READ_WRITE,

		/**
		 * Require only read access to the resource.
		 *
		 * @see ReadWriteLock#readLock()
		 */
		READ

	}

}
