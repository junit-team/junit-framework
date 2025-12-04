/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apiguardian.api.API.Status.DEPRECATED;

import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;

/**
 * Represents a media type as defined by
 * <a href="https://tools.ietf.org/html/rfc2045">RFC 2045</a>.
 *
 * @since 5.12
 * @see org.junit.jupiter.api.TestReporter#publishFile(Path, MediaType)
 * @see org.junit.jupiter.api.TestReporter#publishFile(String, MediaType, org.junit.jupiter.api.function.ThrowingConsumer)
 * @see ExtensionContext#publishFile(String, MediaType, org.junit.jupiter.api.function.ThrowingConsumer)
 * @deprecated Use {@link org.junit.jupiter.api.MediaType} instead.
 */
@Deprecated(since = "5.14", forRemoval = true)
@API(status = Status.DEPRECATED, since = "5.14")
public final class MediaType extends org.junit.jupiter.api.MediaType {

	/**
	 * The {@code text/plain} media type.
	 */
	public static final MediaType TEXT_PLAIN = create("text", "plain");

	/**
	 * The {@code text/plain; charset=UTF-8} media type.
	 */
	public static final MediaType TEXT_PLAIN_UTF_8 = create("text", "plain", UTF_8);

	/**
	 * The {@code application/json} media type.
	 */
	public static final MediaType APPLICATION_JSON = create("application", "json");

	/**
	 * The {@code application/json; charset=UTF-8} media type.
	 * @deprecated Use {@link #APPLICATION_JSON} instead.
	 */
	@Deprecated(since = "5.14")
	@API(status = DEPRECATED, since = "5.14")
	public static final MediaType APPLICATION_JSON_UTF_8 = create("application", "json", UTF_8);

	/**
	 * The {@code application/octet-stream} media type.
	 */
	public static final MediaType APPLICATION_OCTET_STREAM = create("application", "octet-stream");

	/**
	 * The {@code image/jpeg} media type.
	 */
	public static final MediaType IMAGE_JPEG = create("image", "jpeg");

	/**
	 * The {@code image/png} media type.
	 */
	public static final MediaType IMAGE_PNG = create("image", "png");

	/**
	 * Parse the given media type value.
	 *
	 * <p>Must be valid according to
	 * <a href="https://tools.ietf.org/html/rfc2045">RFC 2045</a>.
	 *
	 * @param value the media type value to parse; never {@code null} or blank
	 * @return the parsed media type
	 * @throws PreconditionViolationException if the value is not a valid media type
	 */
	public static MediaType parse(String value) {
		return new MediaType(value);
	}

	/**
	 * Create a media type with the given type and subtype.
	 *
	 * @param type the type; never {@code null} or blank
	 * @param subtype the subtype; never {@code null} or blank
	 * @return the media type
	 */
	public static MediaType create(String type, String subtype) {
		return new MediaType(type, subtype, null);
	}

	/**
	 * Create a media type with the given type, subtype, and charset.
	 *
	 * @param type the type; never {@code null} or blank
	 * @param subtype the subtype; never {@code null} or blank
	 * @param charset the charset; never {@code null}
	 * @return the media type
	 */
	public static MediaType create(String type, String subtype, Charset charset) {
		Preconditions.notNull(charset, "charset must not be null");
		return new MediaType(type, subtype, charset);
	}

	private MediaType(String type, String subtype, @Nullable Charset charset) {
		super(type, subtype, charset);
	}

	private MediaType(String value) {
		super(value);
	}

}
