/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.EqualsAndHashCodeAssertions.assertEqualsAndHashCode;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link FileSource} and {@link DirectorySource}.
 *
 * @since 1.0
 */
class FileSystemSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<FileSource> createSerializableInstances() {
		return Stream.of( //
			FileSource.from(new File("file.source")), //
			FileSource.from(new File("file.and.position"), FilePosition.from(42, 23)));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void nullSourceFileOrDirectoryYieldsException() {
		assertThrows(PreconditionViolationException.class, () -> FileSource.from(null));
	}

	@Test
	void directory() throws Exception {
		var canonicalDir = new File(".").getCanonicalFile();
		var relativeDir = new File("..", canonicalDir.getName());

		var source = DirectorySource.from(relativeDir);

		assertThat(source.getUri()).isEqualTo(canonicalDir.toURI());
		assertThat(source.getFile()).isEqualTo(canonicalDir);
	}

	@Test
	void fileWithoutPosition() throws Exception {
		var canonicalDir = new File(".").getCanonicalFile();
		var relativeDir = new File("..", canonicalDir.getName());
		var relativeFile = new File(relativeDir, "test.txt");
		var canonicalFile = relativeFile.getCanonicalFile();

		var source = FileSource.from(relativeFile);

		assertThat(source.getUri()).isEqualTo(canonicalFile.toURI());
		assertThat(source.getFile()).isEqualTo(canonicalFile);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void fileWithPosition() {
		var file = new File("test.txt");
		var position = FilePosition.from(42, 23);
		var source = FileSource.from(file, position);

		assertThat(source.getUri()).isEqualTo(file.getAbsoluteFile().toURI());
		assertThat(source.getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void fileReuseWithPosition() {
		var file = new File("test.txt");
		var position = FilePosition.from(42, 23);
		var source = FileSource.from(file);
		var sourceWithPosition = source.withPosition(position);

		assertThat(source.getUri()).isEqualTo(file.getAbsoluteFile().toURI());
		assertThat(source.getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(source.getPosition()).isEmpty();

		assertThat(source).isNotSameAs(sourceWithPosition);
		assertThat(source.getFile()).isSameAs(sourceWithPosition.getFile());
		assertThat(sourceWithPosition.getPosition()).hasValue(position);
	}

	@Test
	void equalsAndHashCodeForFileSource() {
		var file1 = new File("foo.txt");
		var file2 = new File("bar.txt");
		assertEqualsAndHashCode(FileSource.from(file1), FileSource.from(file1), FileSource.from(file2));

		var position = FilePosition.from(42, 23);
		assertEqualsAndHashCode(FileSource.from(file1, position), FileSource.from(file1, position),
			FileSource.from(file2, position));
	}

	@Test
	void equalsAndHashCodeForDirectorySource() {
		var dir1 = new File(".");
		var dir2 = new File("..");
		assertEqualsAndHashCode(DirectorySource.from(dir1), DirectorySource.from(dir1), DirectorySource.from(dir2));
	}

}
