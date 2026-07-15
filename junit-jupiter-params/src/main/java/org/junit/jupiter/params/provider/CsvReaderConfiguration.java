/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static de.siegmar.fastcsv.reader.CommentStrategy.NONE;
import static de.siegmar.fastcsv.reader.CommentStrategy.SKIP;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CommentStrategy;

import org.junit.platform.commons.util.Preconditions;

/**
 * Wrapper for {@link CsvSource} with {@code value}, {@link CsvSource} with a
 * {@code textBlock} and {@link CsvFileSource}.
 */
abstract class CsvReaderConfiguration {

	private static final String DEFAULT_DELIMITER = ",";
	private static final char EMPTY_CHAR = '\0';

	static CsvReaderConfiguration fromValueCsvSource(CsvSource csvSource) {
		validateValueAndTextBlock(csvSource);
		Preconditions.condition(csvSource.value().length > 0, () -> "@CsvSource must be declared with `value`");
		var configuration = new ValueCsvSourceConfiguration(csvSource);
		validate(csvSource, configuration);
		return configuration;
	}

	static CsvReaderConfiguration fromTextBlockCsvSource(CsvSource csvSource) {
		validateValueAndTextBlock(csvSource);
		Preconditions.condition(!csvSource.textBlock().isEmpty(),
			() -> "@CsvSource must be declared with a `textBlock`");
		var configuration = new TextBlockCsvSourceConfiguration(csvSource);
		validate(csvSource, configuration);
		return configuration;
	}

	static CsvReaderConfiguration fromCsvFileSource(CsvFileSource csvSource) {
		var configuration = new FileCsvSourceConfiguration(csvSource);
		validate(csvSource, configuration);
		return configuration;
	}

	private static void validateValueAndTextBlock(CsvSource csvSource) {
		var values = csvSource.value();
		Preconditions.condition(values.length > 0 ^ !csvSource.textBlock().isEmpty(),
			() -> "@CsvSource must be declared with either `value` or `textBlock` but not both");
		for (int i = 0; i < values.length; i++) {
			int finalI = i;
			Preconditions.notBlank(values[i], () -> "CSV record at index %d must not be blank".formatted(finalI + 1));
		}
	}

	private static void validate(Annotation csvSource, CsvReaderConfiguration configuration) {
		validateMaxCharsPerColumn(configuration.maxCharsPerColumn());
		validateDelimiter( //
			configuration.delimiterCharacter(), //
			configuration.delimiterString(), //
			csvSource //
		);
		validateControlCharactersDiffer( //
			configuration.delimiter(), //
			configuration.quoteCharacter(), //
			configuration.commentCharacter(), //
			configuration.commentStrategy() //
		);
	}

	private static void validateMaxCharsPerColumn(int maxCharsPerColumn) {
		Preconditions.condition(maxCharsPerColumn > 0 || maxCharsPerColumn == -1,
			() -> "maxCharsPerColumn must be a positive number or -1: " + maxCharsPerColumn);
	}

	private static void validateDelimiter(char delimiter, String delimiterString, Annotation annotation) {
		Preconditions.condition(delimiter == EMPTY_CHAR || delimiterString.isEmpty(),
			() -> "The delimiter and delimiterString attributes cannot be set simultaneously in " + annotation);
	}

	private static void validateControlCharactersDiffer(String delimiter, char quoteCharacter, char commentCharacter,
			CommentStrategy commentStrategy) {

		if (commentStrategy == NONE) {
			Preconditions.condition(stringValuesUnique(delimiter, quoteCharacter),
				() -> ("delimiter or delimiterString: '%s' and quoteCharacter: '%s' " + //
						"must differ").formatted(delimiter, quoteCharacter));
		}
		else {
			Preconditions.condition(stringValuesUnique(delimiter, quoteCharacter, commentCharacter),
				() -> ("delimiter or delimiterString: '%s', quoteCharacter: '%s', and commentCharacter: '%s' " + //
						"must all differ").formatted(delimiter, quoteCharacter, commentCharacter));
		}
	}

	private static boolean stringValuesUnique(Object... values) {
		long uniqueCount = Stream.of(values).map(String::valueOf).distinct().count();
		return uniqueCount == values.length;
	}

	final CsvReaderFactory.DefaultFieldModifier fieldModifier() {
		return new CsvReaderFactory.DefaultFieldModifier(emptyValue(), nullValues(),
			ignoreLeadingAndTrailingWhitespace());
	}

	final int maxFieldSize() {
		return maxCharsPerColumn() == -1 ? Integer.MAX_VALUE : maxCharsPerColumn();
	}

	final String delimiter() {
		char delimiter = delimiterCharacter();
		String delimiterString = delimiterString();
		if (delimiter != EMPTY_CHAR) {
			return String.valueOf(delimiter);
		}
		if (!delimiterString.isEmpty()) {
			return delimiterString;
		}
		return DEFAULT_DELIMITER;
	}

	abstract char delimiterCharacter();

	abstract String delimiterString();

	abstract CommentStrategy commentStrategy();

	abstract char quoteCharacter();

	abstract char commentCharacter();

	abstract String emptyValue();

	abstract Set<String> nullValues();

	abstract boolean ignoreLeadingAndTrailingWhitespace();

	abstract int maxCharsPerColumn();

	abstract boolean namedCsvRecords();

	static final class ValueCsvSourceConfiguration extends CsvReaderConfiguration {
		private final CsvSource delegate;

		ValueCsvSourceConfiguration(CsvSource delegate) {
			this.delegate = delegate;
		}

		@Override
		CommentStrategy commentStrategy() {
			// For CsvSource.value comments are read as if they are fields.
			return NONE;
		}

		@Override
		boolean namedCsvRecords() {
			// For CsvSource.value we manually process the header.
			return false;
		}

		@Override
		char delimiterCharacter() {
			return delegate.delimiter();
		}

		@Override
		String delimiterString() {
			return delegate.delimiterString();
		}

		@Override
		char quoteCharacter() {
			return delegate.quoteCharacter();
		}

		@Override
		char commentCharacter() {
			return delegate.commentCharacter();
		}

		@Override
		String emptyValue() {
			return delegate.emptyValue();
		}

		@Override
		Set<String> nullValues() {
			return Set.of(delegate.nullValues());
		}

		@Override
		boolean ignoreLeadingAndTrailingWhitespace() {
			return delegate.ignoreLeadingAndTrailingWhitespace();
		}

		@Override
		int maxCharsPerColumn() {
			return delegate.maxCharsPerColumn();
		}

	}

	static final class TextBlockCsvSourceConfiguration extends CsvReaderConfiguration {
		private final CsvSource delegate;

		TextBlockCsvSourceConfiguration(CsvSource delegate) {
			this.delegate = delegate;
		}

		@Override
		CommentStrategy commentStrategy() {
			return SKIP;
		}

		@Override
		char delimiterCharacter() {
			return delegate.delimiter();
		}

		@Override
		String delimiterString() {
			return delegate.delimiterString();
		}

		@Override
		char quoteCharacter() {
			return delegate.quoteCharacter();
		}

		@Override
		char commentCharacter() {
			return delegate.commentCharacter();
		}

		@Override
		String emptyValue() {
			return delegate.emptyValue();
		}

		@Override
		Set<String> nullValues() {
			return Set.of(delegate.nullValues());
		}

		@Override
		boolean ignoreLeadingAndTrailingWhitespace() {
			return delegate.ignoreLeadingAndTrailingWhitespace();
		}

		@Override
		int maxCharsPerColumn() {
			return delegate.maxCharsPerColumn();
		}

		@Override
		boolean namedCsvRecords() {
			return delegate.useHeadersInDisplayName();
		}

	}

	static final class FileCsvSourceConfiguration extends CsvReaderConfiguration {
		private final CsvFileSource delegate;

		FileCsvSourceConfiguration(CsvFileSource delegate) {
			this.delegate = delegate;
		}

		@Override
		CommentStrategy commentStrategy() {
			return SKIP;
		}

		@Override
		char delimiterCharacter() {
			return delegate.delimiter();
		}

		@Override
		String delimiterString() {
			return delegate.delimiterString();
		}

		@Override
		char quoteCharacter() {
			return delegate.quoteCharacter();
		}

		@Override
		char commentCharacter() {
			return delegate.commentCharacter();
		}

		@Override
		String emptyValue() {
			return delegate.emptyValue();
		}

		@Override
		Set<String> nullValues() {
			return Set.of(delegate.nullValues());
		}

		@Override
		boolean ignoreLeadingAndTrailingWhitespace() {
			return delegate.ignoreLeadingAndTrailingWhitespace();
		}

		@Override
		int maxCharsPerColumn() {
			return delegate.maxCharsPerColumn();
		}

		@Override
		boolean namedCsvRecords() {
			return delegate.useHeadersInDisplayName();
		}

	}

}
