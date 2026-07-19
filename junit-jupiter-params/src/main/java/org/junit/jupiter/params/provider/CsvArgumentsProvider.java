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

import static org.junit.jupiter.params.provider.CsvReaderConfiguration.fromTextBlockCsvSource;
import static org.junit.jupiter.params.provider.CsvReaderConfiguration.fromValueCsvSource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.jupiter.params.support.ParameterNameAndArgument;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.UnrecoverableExceptions;

/**
 * @since 5.0
 */
class CsvArgumentsProvider extends AnnotationBasedArgumentsProvider<CsvSource> {

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			CsvSource csvSource) {

		return csvSource.textBlock().isEmpty() //
				? provideArgumentsFromValueArray(csvSource, csvSource.value()) //
				: provideArgumentsFromTextBlock(csvSource, csvSource.textBlock());
	}

	private static Stream<Arguments> provideArgumentsFromValueArray(CsvSource csvSource, String[] values) {
		var configuration = fromValueCsvSource(csvSource);
		var useHeadersInDisplayName = csvSource.useHeadersInDisplayName();
		var isFirstRecord = true;
		List<String> headers = List.of();
		List<Arguments> arguments = new ArrayList<>();
		for (String data : values) {
			try (var reader = CsvReaderFactory.createReaderFor(configuration, data)) {
				for (CsvRecord record : reader) {
					requireNoCsvComments(record, configuration);
					if (isFirstRecord && useHeadersInDisplayName) {
						headers = record.getFields();
					}
					else {
						arguments.add(processCsvRecord(record, useHeadersInDisplayName, headers));
					}
					isFirstRecord = false;
				}
			}
			catch (Throwable throwable) {
				throw handleCsvException(throwable, csvSource);
			}
		}
		return arguments.stream();
	}

	private static void requireNoCsvComments(CsvRecord record, CsvReaderConfiguration configuration) {
		Preconditions.condition(!record.isComment(),
			() -> "Comments may not be used when using @CsvSourve.value. Either change the comment character to something other than [%s] or enclose the field in [%s]".formatted(
				configuration.commentCharacter(), configuration.quoteCharacter()));
	}

	private static Stream<Arguments> provideArgumentsFromTextBlock(CsvSource csvSource, String textBlock) {
		var configuration = fromTextBlockCsvSource(csvSource);
		var arguments = new ArrayList<Arguments>();
		try (var reader = CsvReaderFactory.createReaderFor(configuration, textBlock)) {
			boolean useHeadersInDisplayName = csvSource.useHeadersInDisplayName();
			for (CsvRecord record : reader) {
				arguments.add(processCsvRecord(record, useHeadersInDisplayName));
			}
		}
		catch (Throwable throwable) {
			throw handleCsvException(throwable, csvSource);
		}
		return arguments.stream();
	}

	/**
	 * Processes custom null values, supports wrapping of column values in
	 * {@link Named} if necessary (for CSV header support), and returns the
	 * CSV record wrapped in an {@link Arguments} instance.
	 */
	static Arguments processCsvRecord(CsvRecord record, boolean useHeadersInDisplayName) {
		List<String> headers = useHeadersInDisplayName ? getHeaders(record) : List.of();
		return processCsvRecord(record, useHeadersInDisplayName, headers);
	}

	private static Arguments processCsvRecord(CsvRecord record, boolean useHeadersInDisplayName, List<String> headers) {
		List<String> fields = record.getFields();
		Preconditions.condition(!useHeadersInDisplayName || fields.size() <= headers.size(), //
			() -> "The number of columns (%d) exceeds the number of supplied headers (%d) in CSV record: %s".formatted( //
				fields.size(), headers.size(), fields)); //

		@Nullable
		Object[] arguments = new Object[fields.size()];

		for (int i = 0; i < fields.size(); i++) {
			Object argument = resolveNullMarker(fields.get(i));
			if (useHeadersInDisplayName) {
				String header = resolveNullMarker(headers.get(i));
				argument = new ParameterNameAndArgument(String.valueOf(header), argument);
			}
			arguments[i] = argument;
		}

		return Arguments.of(arguments);
	}

	private static List<String> getHeaders(CsvRecord record) {
		return ((NamedCsvRecord) record).getHeader();
	}

	@SuppressWarnings({ "ReferenceEquality", "StringEquality" })
	private static @Nullable String resolveNullMarker(String record) {
		return record == CsvReaderFactory.DefaultFieldModifier.NULL_MARKER ? null : record;
	}

	/**
	 * @return this method always throws an exception and therefore never
	 * returns anything; the return type is merely present to allow this
	 * method to be supplied as the operand in a {@code throw} statement
	 */
	static RuntimeException handleCsvException(Throwable throwable, Annotation annotation) {
		UnrecoverableExceptions.rethrowIfUnrecoverable(throwable);
		if (throwable instanceof PreconditionViolationException exception) {
			throw exception;
		}
		throw new CsvParsingException("Failed to parse CSV input configured via " + annotation, throwable);
	}

}
