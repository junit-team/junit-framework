/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@code UniqueId} encapsulates the creation, parsing, and display of unique IDs
 * for {@link TestDescriptor TestDescriptors}.
 *
 * <p>Instances of this class have value semantics and are immutable.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public final class UniqueId implements Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String ENGINE_SEGMENT_TYPE = "engine";

	/**
	 * Parse a {@code UniqueId} from the supplied string representation using the
	 * default format.
	 *
	 * @param uniqueId the string representation to parse; never {@code null} or blank
	 * @return a properly constructed {@code UniqueId}
	 * @throws JUnitException if the string cannot be parsed
	 */
	public static UniqueId parse(String uniqueId) throws JUnitException {
		Preconditions.notBlank(uniqueId, "Unique ID string must not be null or blank");
		return UniqueIdFormat.getDefault().parse(uniqueId);
	}

	/**
	 * Create an engine's unique ID from its {@code engineId} using the default
	 * format.
	 *
	 * <p>The engine ID will be stored in a {@link Segment} with
	 * {@link Segment#getType type} {@code "engine"}.
	 *
	 * @param engineId the engine ID; never {@code null} or blank
	 * @see #root(String, String)
	 */
	public static UniqueId forEngine(String engineId) {
		Preconditions.notBlank(engineId, "engineId must not be null or blank");
		return root(ENGINE_SEGMENT_TYPE, engineId);
	}

	/**
	 * Create a root unique ID from the supplied {@code segmentType} and
	 * {@code value} using the default format.
	 *
	 * @param segmentType the segment type; never {@code null} or blank
	 * @param value the value; never {@code null} or blank
	 * @see #forEngine(String)
	 */
	public static UniqueId root(String segmentType, String value) {
		return new UniqueId(UniqueIdFormat.getDefault(), new Segment(segmentType, value));
	}

	private final UniqueIdFormat uniqueIdFormat;

	@SuppressWarnings({ "serial", "RedundantSuppression" }) // always used with serializable implementation (List.copyOf())
	private final List<Segment> segments;

	// lazily computed
	private transient int hashCode;

	// lazily computed
	private transient @Nullable SoftReference<String> toString;

	private UniqueId(UniqueIdFormat uniqueIdFormat, Segment segment) {
		this(uniqueIdFormat, List.of(segment));
	}

	/**
	 * Initialize a {@code UniqueId} instance.
	 *
	 * @implNote A defensive copy of the segment list is <b>not</b> created by
	 * this implementation. All callers should immediately drop the reference
	 * to the list instance that they pass into this constructor.
	 */
	UniqueId(UniqueIdFormat uniqueIdFormat, List<Segment> segments) {
		this.uniqueIdFormat = uniqueIdFormat;
		this.segments = List.copyOf(segments);
	}

	Optional<Segment> getRoot() {
		return this.segments.isEmpty() ? Optional.empty() : Optional.of(this.segments.get(0));
	}

	/**
	 * Get the engine ID stored in this {@code UniqueId}, if available.
	 *
	 * @see #forEngine(String)
	 */
	public Optional<String> getEngineId() {
		return getRoot().filter(segment -> ENGINE_SEGMENT_TYPE.equals(segment.getType())).map(Segment::getValue);
	}

	/**
	 * Get the immutable list of {@linkplain Segment segments} that make up this
	 * {@code UniqueId}.
	 */
	public List<Segment> getSegments() {
		return this.segments;
	}

	/**
	 * Construct a new {@code UniqueId} by appending a new {@link Segment}, based
	 * on the supplied {@code segmentType} and {@code value}, to the end of this
	 * {@code UniqueId}.
	 *
	 * <p>This {@code UniqueId} will not be modified.
	 *
	 * <p>Neither the {@code segmentType} nor the {@code value} may contain any
	 * of the special characters used for constructing the string representation
	 * of this {@code UniqueId}.
	 *
	 * @param segmentType the type of the segment; never {@code null} or blank
	 * @param value the value of the segment; never {@code null} or blank
	 */
	public UniqueId append(String segmentType, String value) {
		return append(new Segment(segmentType, value));
	}

	/**
	 * Construct a new {@code UniqueId} by appending a new {@link Segment} to
	 * the end of this {@code UniqueId}.
	 *
	 * <p>This {@code UniqueId} will not be modified.
	 *
	 * @param segment the segment to be appended; never {@code null}
	 *
	 * @since 1.1
	 */
	@API(status = STABLE, since = "1.1")
	public UniqueId append(Segment segment) {
		Preconditions.notNull(segment, "segment must not be null");
		List<Segment> baseSegments = new ArrayList<>(this.segments.size() + 1);
		baseSegments.addAll(this.segments);
		baseSegments.add(segment);
		return new UniqueId(this.uniqueIdFormat, baseSegments);
	}

	/**
	 * Construct a new {@code UniqueId} by appending a new {@link Segment}, based
	 * on the supplied {@code engineId}, to the end of this {@code UniqueId}.
	 *
	 * <p>This {@code UniqueId} will not be modified.
	 *
	 * <p>The engine ID will be stored in a {@link Segment} with
	 * {@link Segment#getType type} {@value #ENGINE_SEGMENT_TYPE}.
	 *
	 * @param engineId the engine ID; never {@code null} or blank
	 *
	 * @since 1.8
	 */
	@API(status = STABLE, since = "1.10")
	public UniqueId appendEngine(String engineId) {
		return append(new Segment(ENGINE_SEGMENT_TYPE, engineId));
	}

	/**
	 * Determine if the supplied {@code UniqueId} is a prefix for this
	 * {@code UniqueId}.
	 *
	 * @param potentialPrefix the {@code UniqueId} to be checked; never {@code null}
	 *
	 * @since 1.1
	 */
	@API(status = STABLE, since = "1.1")
	public boolean hasPrefix(UniqueId potentialPrefix) {
		Preconditions.notNull(potentialPrefix, "potentialPrefix must not be null");
		int size = this.segments.size();
		int prefixSize = potentialPrefix.segments.size();
		return size >= prefixSize && this.segments.subList(0, prefixSize).equals(potentialPrefix.segments);
	}

	/**
	 * Construct a new {@code UniqueId} and removing the last {@link Segment} of
	 * this {@code UniqueId}.
	 *
	 * <p>This {@code UniqueId} will not be modified.
	 *
	 * @return a new {@code UniqueId}; never {@code null}
	 * @throws org.junit.platform.commons.PreconditionViolationException
	 * if this {@code UniqueId} contains a single segment
	 * @since 1.5
	 */
	@API(status = STABLE, since = "1.5")
	public UniqueId removeLastSegment() {
		Preconditions.condition(this.segments.size() > 1, "Cannot remove last remaining segment");
		return new UniqueId(uniqueIdFormat, List.copyOf(this.segments.subList(0, this.segments.size() - 1)));
	}

	/**
	 * Get the last {@link Segment} of this {@code UniqueId}.
	 *
	 * @return the last {@code Segment}; never {@code null}
	 * @since 1.5
	 */
	@API(status = STABLE, since = "1.5")
	public Segment getLastSegment() {
		return this.segments.get(this.segments.size() - 1);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UniqueId that = (UniqueId) o;
		return this.segments.equals(that.segments);
	}

	@Override
	public int hashCode() {
		int value = this.hashCode;
		if (value == 0) {
			value = this.segments.hashCode();
			if (value == 0) {
				// handle the edge case of the computed hashCode being 0
				value = 1;
			}
			// this is a benign race like String#hash
			// we potentially read and write values from multiple threads
			// without a happens-before relationship
			// however the JMM guarantees us that we only ever see values
			// that were valid at one point, either 0 or the hash code
			// so we might end up not seeing a value that a different thread
			// has computed or multiple threads writing the same value
			this.hashCode = value;
		}
		return value;
	}

	/**
	 * Generate the unique, formatted string representation of this {@code UniqueId}
	 * using the configured {@link UniqueIdFormat}.
	 */
	@Override
	public String toString() {
		SoftReference<String> s = this.toString;
		String value = s == null ? null : s.get();
		if (value == null) {
			value = this.uniqueIdFormat.format(this);
			// this is a benign race like String#hash
			// we potentially read and write values from multiple threads
			// without a happens-before relationship
			// however the JMM guarantees us that we only ever see values
			// that were valid at one point, either null or the toString value
			// so we might end up not seeing a value that a different thread
			// has computed or multiple threads writing the same value
			this.toString = new SoftReference<>(value);
		}
		return value;
	}

	/**
	 * A segment of a {@link UniqueId} comprises a <em>type</em> and a
	 * <em>value</em>.
	 */
	@API(status = STABLE, since = "1.0")
	public static final class Segment implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private final String type;
		private final String value;

		/**
		 * Create a new {@code Segment} using the supplied {@code type} and
		 * {@code value}.
		 *
		 * @param type the type of this segment
		 * @param value the value of this segment
		 */
		Segment(String type, String value) {
			Preconditions.notBlank(type, "type must not be null or blank");
			Preconditions.notBlank(value, "value must not be null or blank");
			this.type = type;
			this.value = value;
		}

		/**
		 * Get the type of this segment.
		 */
		public String getType() {
			return this.type;
		}

		/**
		 * Get the value of this segment.
		 */
		public String getValue() {
			return this.value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.type, this.value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Segment that = (Segment) o;
			return Objects.equals(this.type, that.type) && Objects.equals(this.value, that.value);
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
					.append("type", this.type)
					.append("value", this.value)
					.toString();
			// @formatter:on
		}

	}

}
