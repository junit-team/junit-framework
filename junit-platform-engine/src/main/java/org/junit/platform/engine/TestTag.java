/*
 * Copyright 2015-2026 the original author or authors.
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
import java.util.Objects;
import java.util.Set;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;

/**
 * Immutable value object for a <em>tag</em> that is assigned to a test or
 * container.
 *
 * @since 1.0
 * @see #isValid(String)
 * @see #create(String)
 */
@API(status = STABLE, since = "1.0")
public final class TestTag implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String name;

	/**
	 * <em>Reserved characters</em> that are not permissible as part of a tag name.
	 *
	 * <ul>
	 * <li>{@code ,}: <em>comma</em></li>
	 * <li>{@code (}: <em>left parenthesis</em></li>
	 * <li>{@code )}: <em>right parenthesis</em></li>
	 * <li>{@code &}: <em>ampersand</em></li>
	 * <li>{@code |}: <em>vertical bar</em></li>
	 * <li>{@code !}: <em>exclamation point</em></li>
	 * </ul>
	 */
	public static final Set<String> RESERVED_CHARACTERS = Set.of(",", "(", ")", "&", "|", "!");

	/**
	 * Determine if the supplied tag name is valid with regard to the supported
	 * syntax for tags.
	 *
	 * <h4>Syntax Rules for Tags</h4>
	 * <ul>
	 * <li>A tag must not be {@code null}.</li>
	 * <li>A tag must not be blank.</li>
	 * <li>A stripped tag must not contain whitespace.</li>
	 * <li>A stripped tag must not contain ISO control characters.</li>
	 * <li>A stripped tag must not contain {@linkplain #RESERVED_CHARACTERS
	 * reserved characters}.</li>
	 * </ul>
	 *
	 * <p>If this method returns {@code true} for a given name, it is then a
	 * valid candidate for the {@link TestTag#create(String) create()} factory
	 * method.
	 *
	 * @param name the name of the tag to validate; may be {@code null} or blank
	 * @return {@code true} if the supplied tag name conforms to the supported
	 * syntax for tags
	 * @see StringUtils#isNotBlank(String)
	 * @see String#strip()
	 * @see StringUtils#doesNotContainWhitespace(String)
	 * @see StringUtils#doesNotContainIsoControlCharacter(String)
	 * @see #RESERVED_CHARACTERS
	 * @see TestTag#create(String)
	 */
	public static boolean isValid(@Nullable String name) {
		if (name == null) {
			return false;
		}
		name = name.strip();

		return !name.isEmpty() && //
				StringUtils.doesNotContainWhitespace(name) && //
				StringUtils.doesNotContainIsoControlCharacter(name) && //
				doesNotContainReservedCharacter(name);
	}

	private static boolean doesNotContainReservedCharacter(String str) {
		return RESERVED_CHARACTERS.stream().noneMatch(str::contains);
	}

	/**
	 * Create a {@code TestTag} from the supplied {@code name}.
	 *
	 * <p>Consider checking whether the syntax of the supplied {@code name}
	 * is {@linkplain #isValid(String) valid} before attempting to create a
	 * {@code TestTag} using this factory method.
	 *
	 * <p>Note: the supplied {@code name} will be {@linkplain String#strip() stripped}.
	 *
	 * @param name the name of the tag; must be syntactically <em>valid</em>
	 * @throws PreconditionViolationException if the supplied tag name is not
	 * syntactically <em>valid</em>
	 * @see TestTag#isValid(String)
	 */
	public static TestTag create(String name) throws PreconditionViolationException {
		return new TestTag(name);
	}

	private TestTag(String name) {
		Preconditions.condition(TestTag.isValid(name),
			() -> "Tag name [%s] must be syntactically valid".formatted(name));
		this.name = name.strip();
	}

	/**
	 * Get the name of this tag.
	 *
	 * @return the name of this tag; never {@code null} or blank
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof TestTag that && Objects.equals(this.name, that.name));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}

}
