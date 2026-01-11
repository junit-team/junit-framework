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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.0
 */
class CsvFileArgumentsProvider extends AnnotationBasedArgumentsProvider<CsvFileSource> {

	private final InputStreamProvider inputStreamProvider;

	CsvFileArgumentsProvider() {
		this(DefaultInputStreamProvider.INSTANCE);
	}

	CsvFileArgumentsProvider(InputStreamProvider inputStreamProvider) {
		this.inputStreamProvider = inputStreamProvider;
	}

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			CsvFileSource csvFileSource) {

		Charset charset = getCharsetFrom(csvFileSource);

		CsvReaderFactory.validate(csvFileSource);

		Stream<Source> resources = Arrays.stream(csvFileSource.resources()).map(inputStreamProvider::classpathResource);
		Stream<Source> files = Arrays.stream(csvFileSource.files()).map(inputStreamProvider::file);
		List<Source> sources = Stream.concat(resources, files).toList();

		// @formatter:off
		return Preconditions.notEmpty(sources, "Resources or files must not be empty")
				.stream()
				.map(source -> source.open(context))
				.map(inputStream -> CsvReaderFactory.createReaderFor(csvFileSource, inputStream, charset))
				.flatMap(reader -> toStream(reader, csvFileSource));
		// @formatter:on
	}

	private static Charset getCharsetFrom(CsvFileSource csvFileSource) {
		try {
			return Charset.forName(csvFileSource.encoding());
		}
		catch (Exception ex) {
			throw new PreconditionViolationException("The charset supplied in " + csvFileSource + " is invalid", ex);
		}
	}

	private static Stream<Arguments> toStream(CsvReader<? extends CsvRecord> reader, CsvFileSource csvFileSource) {
		var spliterator = CsvExceptionHandlingSpliterator.delegatingTo(reader.spliterator(), csvFileSource);
		boolean useHeadersInDisplayName = csvFileSource.useHeadersInDisplayName();
		// @formatter:off
		return StreamSupport.stream(spliterator, false)
				.skip(csvFileSource.numLinesToSkip())
				.map(record -> CsvArgumentsProvider.processCsvRecord(
						record, useHeadersInDisplayName)
				)
				.onClose(() -> {
					try {
						reader.close();
					}
					catch (Throwable throwable) {
						throw CsvArgumentsProvider.handleCsvException(throwable, csvFileSource);
					}
				});
		// @formatter:on
	}

	private record CsvExceptionHandlingSpliterator<T>(Spliterator<T> delegate, CsvFileSource csvFileSource)
			implements Spliterator<T> {

		static <T> CsvExceptionHandlingSpliterator<T> delegatingTo(Spliterator<T> delegate,
				CsvFileSource csvFileSource) {
			return new CsvExceptionHandlingSpliterator<>(delegate, csvFileSource);
		}

		@Override
		public boolean tryAdvance(final Consumer<? super T> action) {
			try {
				return delegate.tryAdvance(action);
			}
			catch (Throwable throwable) {
				throw CsvArgumentsProvider.handleCsvException(throwable, csvFileSource);
			}
		}

		@Override
		public Spliterator<T> trySplit() {
			return delegate.trySplit();
		}

		@Override
		public long estimateSize() {
			return delegate.estimateSize();
		}

		@Override
		public int characteristics() {
			return delegate.characteristics();
		}

	}

	@FunctionalInterface
	interface Source {

		InputStream open(ExtensionContext context);

	}

	interface InputStreamProvider {

		InputStream openClasspathResource(Class<?> baseClass, String path);

		InputStream openFile(String path);

		default Source classpathResource(String path) {
			return context -> openClasspathResource(context.getRequiredTestClass(), path);
		}

		default Source file(String path) {
			return __ -> openFile(path);
		}

	}

	private static class DefaultInputStreamProvider implements InputStreamProvider {

		private static final DefaultInputStreamProvider INSTANCE = new DefaultInputStreamProvider();

		@Override
		public InputStream openClasspathResource(Class<?> baseClass, String path) {
			Preconditions.notBlank(path, () -> "Classpath resource [" + path + "] must not be null or blank");
			//noinspection resource (closed elsewhere)
			InputStream inputStream = baseClass.getResourceAsStream(path);
			return Preconditions.notNull(inputStream, () -> "Classpath resource [" + path + "] does not exist");
		}

		@Override
		public InputStream openFile(String path) {
			Preconditions.notBlank(path, () -> "File [" + path + "] must not be null or blank");
			try {
				return Files.newInputStream(Path.of(path));
			}
			catch (IOException e) {
				throw new JUnitException("File [" + path + "] could not be read", e);
			}
		}

	}

}
