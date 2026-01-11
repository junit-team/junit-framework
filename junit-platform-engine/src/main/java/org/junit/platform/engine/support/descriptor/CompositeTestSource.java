/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestSource;

/**
 * A {@code CompositeTestSource} contains one or more {@link TestSource TestSources}.
 *
 * <p>{@code CompositeTestSource} and its {@link #getSources sources} are immutable.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public final class CompositeTestSource implements TestSource {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code CompositeTestSource} based on the supplied
	 * collection of {@link TestSource sources}.
	 *
	 * <p>This constructor makes a defensive copy of the supplied collection
	 * and stores the sources as a list in the order in which they are
	 * returned by the collection's iterator.
	 *
	 * @param sources the collection of sources to store in this
	 * {@code CompositeTestSource}; never {@code null} or empty
	 */
	public static CompositeTestSource from(Collection<? extends TestSource> sources) {
		return new CompositeTestSource(sources);
	}

	@SuppressWarnings({ "serial", "RedundantSuppression" }) // always used with serializable implementation (unmodifiableList())
	private final List<TestSource> sources;

	private CompositeTestSource(Collection<? extends TestSource> sources) {
		Preconditions.notEmpty(sources, "TestSource collection must not be null or empty");
		Preconditions.containsNoNullElements(sources, "individual TestSources must not be null");
		this.sources = List.copyOf(sources);
	}

	/**
	 * Get an immutable list of the {@linkplain TestSource sources} stored in this
	 * {@code CompositeTestSource}.
	 *
	 * @return the sources stored in this {@code CompositeTestSource}; never {@code null}
	 */
	public List<TestSource> getSources() {
		return this.sources;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		CompositeTestSource that = (CompositeTestSource) obj;
		return this.sources.equals(that.sources);
	}

	@Override
	public int hashCode() {
		return this.sources.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("sources", this.sources).toString();
	}

}
