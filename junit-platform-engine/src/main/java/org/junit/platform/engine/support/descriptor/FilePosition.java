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
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * Position inside a file represented by {@linkplain #getLine line} and
 * {@linkplain #getColumn column} numbers.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public final class FilePosition implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(FilePosition.class);

	/**
	 * Create a new {@code FilePosition} using the supplied {@code line} number
	 * and an undefined column number.
	 *
	 * @param line the line number; must be greater than zero
	 * @return a {@link FilePosition} with the given line number
	 */
	public static FilePosition from(int line) {
		return new FilePosition(line);
	}

	/**
	 * Create a new {@code FilePosition} using the supplied {@code line} and
	 * {@code column} numbers.
	 *
	 * @param line the line number; must be greater than zero
	 * @param column the column number; must be greater than zero
	 * @return a {@link FilePosition} with the given line and column numbers
	 */
	public static FilePosition from(int line, int column) {
		return new FilePosition(line, column);
	}

	/**
	 * Create an optional {@code FilePosition} by parsing the supplied
	 * {@code query} string.
	 *
	 * <p>Examples of valid {@code query} strings:
	 * <ul>
	 *     <li>{@code "line=23"}</li>
	 *     <li>{@code "line=23&column=42"}</li>
	 * </ul>
	 *
	 * @param query the query string; may be {@code null}
	 * @return an {@link Optional} containing a {@link FilePosition} with
	 * the parsed line and column numbers; never {@code null} but potentially
	 * empty
	 * @since 1.3
	 * @see #from(int)
	 * @see #from(int, int)
	 */
	public static Optional<FilePosition> fromQuery(String query) {
		FilePosition result = null;
		Integer line = null;
		Integer column = null;
		if (StringUtils.isNotBlank(query)) {
			try {
				for (String pair : query.split("&")) {
					String[] data = pair.split("=");
					if (data.length == 2) {
						String key = data[0];
						if (line == null && "line".equals(key)) {
							line = Integer.valueOf(data[1]);
						}
						else if (column == null && "column".equals(key)) {
							column = Integer.valueOf(data[1]);
						}
					}

					// Already found what we're looking for?
					if (line != null && column != null) {
						break;
					}
				}
			}
			catch (IllegalArgumentException ex) {
				logger.debug(ex, () -> "Failed to parse 'line' and/or 'column' from query string: " + query);
				// fall-through and continue
			}

			if (line != null) {
				result = column == null ? new FilePosition(line) : new FilePosition(line, column);
			}
		}
		return Optional.ofNullable(result);
	}

	private final int line;

	private final @Nullable Integer column;

	private FilePosition(int line) {
		Preconditions.condition(line > 0, "line number must be greater than zero");
		this.line = line;
		this.column = null;
	}

	private FilePosition(int line, int column) {
		Preconditions.condition(line > 0, "line number must be greater than zero");
		Preconditions.condition(column > 0, "column number must be greater than zero");
		this.line = line;
		this.column = column;
	}

	/**
	 * Get the line number of this {@code FilePosition}.
	 *
	 * @return the line number
	 */
	public int getLine() {
		return this.line;
	}

	/**
	 * Get the column number of this {@code FilePosition}, if available.
	 *
	 * @return an {@code Optional} containing the column number; never
	 * {@code null} but potentially empty
	 */
	public Optional<Integer> getColumn() {
		return Optional.ofNullable(this.column);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FilePosition that = (FilePosition) o;
		return (this.line == that.line) && Objects.equals(this.column, that.column);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.line, this.column);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("line", this.line)
				.append("column", getColumn().orElse(-1))
				.toString();
		// @formatter:on
	}

}
