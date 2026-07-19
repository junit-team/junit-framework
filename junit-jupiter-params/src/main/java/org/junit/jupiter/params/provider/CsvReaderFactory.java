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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;
import de.siegmar.fastcsv.reader.FieldModifier;
import de.siegmar.fastcsv.reader.NamedCsvRecordHandler;

/**
 * @since 6.0
 */
class CsvReaderFactory {

	private static final boolean SKIP_EMPTY_LINES = true;
	private static final boolean TRIM_WHITESPACES_AROUND_QUOTES = true;
	private static final FieldMismatchStrategy ALLOW_EXTRA_FIELDS = FieldMismatchStrategy.IGNORE;
	private static final FieldMismatchStrategy ALLOW_MISSING_FIELDS = FieldMismatchStrategy.IGNORE;
	private static final boolean ALLOW_DUPLICATE_HEADER_FIELDS = true;
	private static final int MAX_FIELDS = 512;
	private static final int MAX_RECORD_SIZE = Integer.MAX_VALUE;

	static CsvReader<? extends CsvRecord> createReaderFor(CsvReaderConfiguration configuration, String data) {
		return createReaderFor(configuration, new StringReader(data));
	}

	static CsvReader<? extends CsvRecord> createReaderFor(CsvReaderConfiguration configuration, InputStream inputStream,
			Charset charset) {
		return createReaderFor(configuration, new InputStreamReader(inputStream, charset));
	}

	private static CsvReader<? extends CsvRecord> createReaderFor(CsvReaderConfiguration configuration, Reader reader) {
		var builder = CsvReader.builder() //
				.skipEmptyLines(SKIP_EMPTY_LINES) //
				.trimWhitespacesAroundQuotes(TRIM_WHITESPACES_AROUND_QUOTES) //
				.extraFieldStrategy(ALLOW_EXTRA_FIELDS) //
				.missingFieldStrategy(ALLOW_MISSING_FIELDS) //
				.fieldSeparator(configuration.fieldSeparator()) //
				.quoteCharacter(configuration.quoteCharacter()) //
				.commentStrategy(configuration.commentStrategy()) //
				.commentCharacter(configuration.commentCharacter()); //

		var fieldModifier = (FieldModifier) new DefaultFieldModifier(//
			configuration.emptyValue(), //
			configuration.nullValues(), //
			configuration.ignoreLeadingAndTrailingWhitespace() //
		);

		var callbackHandler = configuration.namedCsvRecords() //
				? NamedCsvRecordHandler.builder() //
						.allowDuplicateHeaderFields(ALLOW_DUPLICATE_HEADER_FIELDS) //
						.maxFields(MAX_FIELDS) //
						.maxRecordSize(MAX_RECORD_SIZE) //
						.maxFieldSize(configuration.maxFieldSize()) //
						.fieldModifier(fieldModifier) //
						.build() //
				: CsvRecordHandler.builder() //
						.maxFields(MAX_FIELDS) //
						.maxRecordSize(MAX_RECORD_SIZE) //
						.maxFieldSize(configuration.maxFieldSize()) //
						.fieldModifier(fieldModifier) //
						.build();

		return builder.build(callbackHandler, reader);
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
