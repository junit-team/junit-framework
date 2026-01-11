/*
 * Copyright 2015-2026 the original author or authors.
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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvCallbackHandler;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

import org.junit.platform.commons.util.Preconditions;

/**
 * @since 6.0
 */
class CsvReaderFactory {

	private static final String DEFAULT_DELIMITER = ",";
	private static final char EMPTY_CHAR = '\0';
	private static final boolean SKIP_EMPTY_LINES = true;
	private static final boolean TRIM_WHITESPACES_AROUND_QUOTES = true;
	private static final boolean ALLOW_EXTRA_FIELDS = true;
	private static final boolean ALLOW_MISSING_FIELDS = true;
	private static final boolean ALLOW_DUPLICATE_HEADER_FIELDS = true;
	private static final int MAX_FIELDS = 512;
	private static final int MAX_RECORD_SIZE = Integer.MAX_VALUE;

	static void validate(CsvSource csvSource) {
		validateMaxCharsPerColumn(csvSource.maxCharsPerColumn());
		validateDelimiter(csvSource.delimiter(), csvSource.delimiterString(), csvSource);
	}

	static void validate(CsvFileSource csvFileSource) {
		validateMaxCharsPerColumn(csvFileSource.maxCharsPerColumn());
		validateDelimiter(csvFileSource.delimiter(), csvFileSource.delimiterString(), csvFileSource);
	}

	private static void validateMaxCharsPerColumn(int maxCharsPerColumn) {
		Preconditions.condition(maxCharsPerColumn > 0 || maxCharsPerColumn == -1,
			() -> "maxCharsPerColumn must be a positive number or -1: " + maxCharsPerColumn);
	}

	private static void validateDelimiter(char delimiter, String delimiterString, Annotation annotation) {
		Preconditions.condition(delimiter == EMPTY_CHAR || delimiterString.isEmpty(),
			() -> "The delimiter and delimiterString attributes cannot be set simultaneously in " + annotation);
	}

	static CsvReader<? extends CsvRecord> createReaderFor(CsvSource csvSource, String data) {
		String delimiter = selectDelimiter(csvSource.delimiter(), csvSource.delimiterString());
		var commentStrategy = csvSource.textBlock().isEmpty() ? NONE : SKIP;
		// @formatter:off
		validateControlCharactersDiffer(
				delimiter, csvSource.quoteCharacter(), csvSource.commentCharacter(), commentStrategy);

		var builder = CsvReader.builder()
				.skipEmptyLines(SKIP_EMPTY_LINES)
				.trimWhitespacesAroundQuotes(TRIM_WHITESPACES_AROUND_QUOTES)
				.allowExtraFields(ALLOW_EXTRA_FIELDS)
				.allowMissingFields(ALLOW_MISSING_FIELDS)
				.fieldSeparator(delimiter)
				.quoteCharacter(csvSource.quoteCharacter())
				.commentStrategy(commentStrategy)
				.commentCharacter(csvSource.commentCharacter());

		var callbackHandler = createCallbackHandler(
				csvSource.emptyValue(),
				Set.of(csvSource.nullValues()),
				csvSource.ignoreLeadingAndTrailingWhitespace(),
				csvSource.maxCharsPerColumn(),
				csvSource.useHeadersInDisplayName()
		);
		// @formatter:on
		return builder.build(callbackHandler, data);
	}

	static CsvReader<? extends CsvRecord> createReaderFor(CsvFileSource csvFileSource, InputStream inputStream,
			Charset charset) {

		String delimiter = selectDelimiter(csvFileSource.delimiter(), csvFileSource.delimiterString());
		var commentStrategy = SKIP;
		// @formatter:off
		validateControlCharactersDiffer(
				delimiter, csvFileSource.quoteCharacter(), csvFileSource.commentCharacter(), commentStrategy);

		var builder = CsvReader.builder()
				.skipEmptyLines(SKIP_EMPTY_LINES)
				.trimWhitespacesAroundQuotes(TRIM_WHITESPACES_AROUND_QUOTES)
				.allowExtraFields(ALLOW_EXTRA_FIELDS)
				.allowMissingFields(ALLOW_MISSING_FIELDS)
				.fieldSeparator(delimiter)
				.quoteCharacter(csvFileSource.quoteCharacter())
				.commentStrategy(commentStrategy)
				.commentCharacter(csvFileSource.commentCharacter());

		var callbackHandler = createCallbackHandler(
				csvFileSource.emptyValue(),
				Set.of(csvFileSource.nullValues()),
				csvFileSource.ignoreLeadingAndTrailingWhitespace(),
				csvFileSource.maxCharsPerColumn(),
				csvFileSource.useHeadersInDisplayName()
		);
		// @formatter:on
		return builder.build(callbackHandler, inputStream, charset);
	}

	private static String selectDelimiter(char delimiter, String delimiterString) {
		if (delimiter != EMPTY_CHAR) {
			return String.valueOf(delimiter);
		}
		if (!delimiterString.isEmpty()) {
			return delimiterString;
		}
		return DEFAULT_DELIMITER;
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

	private static CsvCallbackHandler<? extends CsvRecord> createCallbackHandler(String emptyValue,
			Set<String> nullValues, boolean ignoreLeadingAndTrailingWhitespaces, int maxCharsPerColumn,
			boolean useHeadersInDisplayName) {

		int maxFieldSize = maxCharsPerColumn == -1 ? Integer.MAX_VALUE : maxCharsPerColumn;
		FieldModifier modifier = new DefaultFieldModifier(emptyValue, nullValues, ignoreLeadingAndTrailingWhitespaces);

		// @formatter:off
		if (useHeadersInDisplayName) {
			return NamedCsvRecordHandler.builder()
					.allowDuplicateHeaderFields(ALLOW_DUPLICATE_HEADER_FIELDS)
					.maxFields(MAX_FIELDS)
					.maxRecordSize(MAX_RECORD_SIZE)
					.maxFieldSize(maxFieldSize)
					.fieldModifier(modifier)
					.build();
		}
		return CsvRecordHandler.builder()
				.maxFields(MAX_FIELDS)
				.maxRecordSize(MAX_RECORD_SIZE)
				.maxFieldSize(maxFieldSize)
				.fieldModifier(modifier)
				.build();
		// @formatter:on
	}

	record DefaultFieldModifier(String emptyValue, Set<String> nullValues, boolean ignoreLeadingAndTrailingWhitespaces)
			implements FieldModifier {

		/**
		 * Represents a {@code null} value and serves as a workaround since FastCSV
		 * does not allow the modified field value to be {@code null}.
		 *
		 * <p>The marker is generated with a unique ID to ensure it cannot conflict
		 * with actual CSV content.
		 */
		static final String NULL_MARKER = "<null marker: %s>".formatted(UUID.randomUUID());

		@Override
		public String modify(long unusedStartingLineNumber, int unusedFieldIdx, boolean quoted, String field) {
			if (quoted && field.isEmpty() && !emptyValue.isEmpty()) {
				return emptyValue;
			}
			if (!quoted && field.isBlank()) {
				return NULL_MARKER;
			}
			String modifiedField = (!quoted && ignoreLeadingAndTrailingWhitespaces) ? field.trim() : field;
			if (nullValues.contains(modifiedField)) {
				return NULL_MARKER;
			}
			return modifiedField;
		}

	}

	private CsvReaderFactory() {
	}

}
